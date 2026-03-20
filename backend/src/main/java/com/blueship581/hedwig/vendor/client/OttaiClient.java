package com.blueship581.hedwig.vendor.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.blueship581.hedwig.domain.enums.TrendDirection;
import com.blueship581.hedwig.exception.VendorException;
import com.blueship581.hedwig.vendor.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * Client for the Ottai CGM vendor API.
 *
 * Base URL: https://api.ottai.com
 * Auth: Bearer token (JWT HS512, valid 180 days)
 * Token validation: parse JWT payload Base64, extract "exp" field
 */
@Slf4j
@Component
public class OttaiClient implements VendorClient {

    private static final String BASE_URL = "https://api.ottai.com";

    // arrowType integer -> TrendDirection mapping per API spec
    private static final Map<Integer, TrendDirection> ARROW_MAP = Map.of(
            1, TrendDirection.DOUBLE_UP,
            2, TrendDirection.SINGLE_UP,
            3, TrendDirection.FLAT,
            4, TrendDirection.SINGLE_DOWN,
            5, TrendDirection.DOUBLE_DOWN,
            6, TrendDirection.FORTY_FIVE_UP,
            7, TrendDirection.FORTY_FIVE_DOWN
    );

    private final WebClient webClient;

    public OttaiClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(BASE_URL).build();
    }

    @Override
    public VendorTokenInfo validateToken(String accessToken) {
        String normalizedToken = VendorTokenNormalizer.normalize(accessToken);
        log.info("[Ottai] validateToken called, token length={}", normalizedToken != null ? normalizedToken.length() : 0);
        try {
            // Decode JWT payload from Base64 without verifying signature
            String[] parts = normalizedToken.split("\\.");
            if (parts.length < 2) {
                log.warn("[Ottai] Invalid JWT format: expected 3 parts, got {}", parts.length);
                return VendorTokenInfo.builder().valid(false).build();
            }
            String payloadJson = new String(
                    Base64.getUrlDecoder().decode(padBase64(parts[1])),
                    StandardCharsets.UTF_8
            );
            log.info("[Ottai] JWT payload: {}", payloadJson);
            JSONObject payload = JSON.parseObject(payloadJson);
            long expEpochSec = payload.getLongValue("exp");
            String userId = payload.containsKey("userId") ? payload.getString("userId") : null;
            Instant expiresAt = Instant.ofEpochSecond(expEpochSec);
            boolean valid = Instant.now().isBefore(expiresAt);

            log.info("[Ottai] Token validation: userId={}, expiresAt={}, valid={}", userId, expiresAt, valid);

            return VendorTokenInfo.builder()
                    .token(normalizedToken)
                    .userId(userId)
                    .expiresAt(expiresAt)
                    .valid(valid)
                    .build();
        } catch (Exception e) {
            log.error("[Ottai] Failed to parse JWT token", e);
            return VendorTokenInfo.builder().valid(false).build();
        }
    }

    /**
     * Ottai login requires WeChat mini-program codes and cannot be automated server-side.
     * This method is provided for interface compliance only.
     */
    @Override
    public VendorTokenInfo login(VendorLoginRequest loginRequest) {
        throw new VendorException(
                "欧泰暂不支持通过账号密码直接登录，请在微信小程序中抓包获取访问令牌后再连接。"
        );
    }

    @Override
    public List<VendorSubject> getMonitoredSubjects(String accessToken, String vendorUserId) {
        log.info("[Ottai] getMonitoredSubjects: vendorUserId={}", vendorUserId);
        try {
            log.info("[Ottai] POST {}/link/application/server/invite/link/relatives", BASE_URL);
            String body = webClient.post()
                    .uri("/link/application/server/invite/link/relatives")
                    .headers(h -> buildHeaders(h, accessToken, vendorUserId))
                    .bodyValue(Map.of("unit", "mmol_L"))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JSONObject response = JSON.parseObject(body);
            log.info("[Ottai] getMonitoredSubjects response: {}", response);
            validateResponse(response, "getMonitoredSubjects");

            List<VendorSubject> subjects = new ArrayList<>();
            JSONArray data = response.getJSONArray("data");
            for (int i = 0; i < data.size(); i++) {
                JSONObject item = data.getJSONObject(i);
                String subjectUserId = item.containsKey("fromUserId")
                        ? String.valueOf(item.getLongValue("fromUserId")) : null;
                String deviceId = item.containsKey("fromUserDeviceId")
                        ? String.valueOf(item.getLongValue("fromUserDeviceId")) : null;
                String displayName = item.containsKey("fromUserRemark")
                        ? item.getString("fromUserRemark") : null;

                Double latestGlucose = null;
                if (item.containsKey("glucose") && item.get("glucose") != null) {
                    try {
                        latestGlucose = Double.parseDouble(item.getString("glucose"));
                    } catch (NumberFormatException ignored) {
                    }
                }

                Long sensorRestSeconds = null;
                if (item.containsKey("restDeviceTime") && item.get("restDeviceTime") != null) {
                    sensorRestSeconds = item.getLongValue("restDeviceTime");
                }

                subjects.add(VendorSubject.builder()
                        .subjectId(subjectUserId)
                        .deviceId(deviceId)
                        .displayName(displayName)
                        .latestGlucoseMmol(latestGlucose)
                        .sensorRestSeconds(sensorRestSeconds)
                        .build());
            }
            return subjects;

        } catch (VendorException e) {
            throw e;
        } catch (WebClientResponseException e) {
            throw apiError("获取监测对象列表", e);
        } catch (Exception e) {
            throw new VendorException("获取欧泰监测对象列表失败", e);
        }
    }

    /**
     * Lightweight realtime fetch: one POST to /relatives returns latest glucose,
     * receiveTime, and arrowType for ALL monitored subjects.
     *
     * @return map of vendorSubjectId (fromUserId) -> VendorGlucoseData
     */
    public Map<String, VendorGlucoseData> getRealtimeFromRelatives(
            String accessToken, String vendorUserId) {
        log.debug("[Ottai] getRealtimeFromRelatives: vendorUserId={}", vendorUserId);
        try {
            String body = webClient.post()
                    .uri("/link/application/server/invite/link/relatives")
                    .headers(h -> buildHeaders(h, accessToken, vendorUserId))
                    .bodyValue(Map.of("unit", "mmol_L"))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JSONObject response = JSON.parseObject(body);
            validateResponse(response, "getRealtimeFromRelatives");

            Map<String, VendorGlucoseData> result = new HashMap<>();
            JSONArray data = response.getJSONArray("data");
            for (int i = 0; i < data.size(); i++) {
                JSONObject item = data.getJSONObject(i);
                String subjectId = String.valueOf(item.getLongValue("fromUserId"));

                if (!item.containsKey("glucose") || item.get("glucose") == null
                        || !item.containsKey("receiveTime") || item.get("receiveTime") == null) {
                    continue;
                }

                double glucose;
                try {
                    glucose = Double.parseDouble(item.getString("glucose"));
                } catch (NumberFormatException e) {
                    log.warn("[Ottai] Skipping subject {} with unparseable glucose: {}",
                            subjectId, item.getString("glucose"));
                    continue;
                }

                long receiveTimeMs = item.getLongValue("receiveTime");
                int arrowType = item.containsKey("arrowType") ? item.getIntValue("arrowType") : 0;

                result.put(subjectId, VendorGlucoseData.builder()
                        .glucoseMmol(glucose)
                        .readingTime(Instant.ofEpochMilli(receiveTimeMs))
                        .trendDirection(mapArrowType(arrowType))
                        .build());
            }
            log.debug("[Ottai] getRealtimeFromRelatives returned {} subjects", result.size());
            return result;

        } catch (VendorException e) {
            throw e;
        } catch (WebClientResponseException e) {
            throw apiError("获取实时血糖数据", e);
        } catch (Exception e) {
            throw new VendorException("获取欧泰实时血糖数据失败", e);
        }
    }

    @Override
    public VendorGlucoseData getLatestGlucose(String accessToken, String vendorUserId, VendorSubject subject) {
        // The relatives list already contains the latest glucose; use historical endpoint for accuracy
        List<VendorGlucoseData> history = getHistoricalGlucose(accessToken, vendorUserId, subject);
        if (history.isEmpty()) {
            throw new VendorException("监测对象暂无血糖数据：" + subject.getSubjectId());
        }
        return history.get(history.size() - 1);
    }

    @Override
    public List<VendorGlucoseData> getHistoricalGlucose(String accessToken, String vendorUserId, VendorSubject subject) {
        log.info("[Ottai] getHistoricalGlucose: vendorUserId={}, subjectId={}, deviceId={}",
                vendorUserId, subject.getSubjectId(), subject.getDeviceId());
        try {
            log.info("[Ottai] GET {}/link/application/server/tag/search/queryMonitorBase?deviceId={}&userId={}&isOpen=1&timeType=2&unit=mmol_L",
                    BASE_URL, subject.getDeviceId(), subject.getSubjectId());
            String body = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/link/application/server/tag/search/queryMonitorBase")
                            .queryParam("deviceId", subject.getDeviceId())
                            .queryParam("userId", subject.getSubjectId())
                            .queryParam("isOpen", "1")
                            .queryParam("timeType", "2")   // last 24 hours
                            .queryParam("unit", "mmol_L")
                            .build())
                    .headers(h -> buildHeaders(h, accessToken, vendorUserId))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JSONObject response = JSON.parseObject(body);
            log.info("[Ottai] getHistoricalGlucose response code: {}",
                    response != null ? response.getString("code") : "null");
            validateResponse(response, "getHistoricalGlucose");

            JSONObject data = response.getJSONObject("data");
            JSONArray curveList = data.getJSONArray("curveList");
            if (curveList == null || curveList.isEmpty()) {
                return Collections.emptyList();
            }

            List<VendorGlucoseData> readings = new ArrayList<>();
            for (int i = 0; i < curveList.size(); i++) {
                JSONObject point = curveList.getJSONObject(i);
                double glucoseMmol = point.getDoubleValue("glucose");
                long monitorTimeMs = point.getLongValue("monitorTime");
                readings.add(VendorGlucoseData.builder()
                        .glucoseMmol(glucoseMmol)
                        .readingTime(Instant.ofEpochMilli(monitorTimeMs))
                        .trendDirection(TrendDirection.NONE)  // historical data has no trend
                        .build());
            }
            return readings;

        } catch (VendorException e) {
            throw e;
        } catch (WebClientResponseException e) {
            throw apiError("获取历史血糖数据", e);
        } catch (Exception e) {
            throw new VendorException("获取欧泰历史血糖数据失败", e);
        }
    }

    private void buildHeaders(org.springframework.http.HttpHeaders headers,
                              String accessToken, String vendorUserId) {
        String normalizedToken = VendorTokenNormalizer.normalize(accessToken);
        headers.set("Host", "api.ottai.com");
        headers.set("Content-Type", "application/json");
        headers.setBearerAuth(normalizedToken);
        headers.set("userId", vendorUserId);
        headers.set("unit", "mmol_L");
        headers.set("timeStamp", String.valueOf(System.currentTimeMillis()));
        headers.set("timezone", "28800");
        headers.set("appName", "miniprogram");
    }

    private TrendDirection mapArrowType(int arrowType) {
        return ARROW_MAP.getOrDefault(arrowType, TrendDirection.NONE);
    }

    private void validateResponse(JSONObject response, String operation) {
        if (response == null) {
            throw new VendorException("欧泰接口返回为空：" + localizeOperation(operation));
        }
        String code = response.getString("code");
        if (code == null || !"OK".equals(code)) {
            String msg = response.containsKey("msg") ? response.getString("msg") : "未知错误";
            throw new VendorException("欧泰接口返回异常（" + localizeOperation(operation) + "）：" + msg);
        }
    }

    private VendorException apiError(String operation, WebClientResponseException e) {
        String detail = extractErrorDetail(e.getResponseBodyAsString());
        String message = String.format(
                "欧泰接口请求失败（%s，HTTP %s）",
                operation,
                e.getStatusCode().value()
        );
        if (detail == null || detail.isBlank()) {
            return new VendorException(message, e);
        }
        return new VendorException(message + "：" + detail, e);
    }

    private String extractErrorDetail(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JSONObject root = JSON.parseObject(body);
            if (root.containsKey("msg") && root.getString("msg") != null) {
                return root.getString("msg");
            }
            if (root.containsKey("message") && root.getString("message") != null) {
                return root.getString("message");
            }
            if (root.containsKey("error") && root.getString("error") != null) {
                return root.getString("error");
            }
        } catch (Exception ignored) {
        }
        return body.length() > 160 ? body.substring(0, 160) + "..." : body;
    }

    private String localizeOperation(String operation) {
        return switch (operation) {
            case "getMonitoredSubjects" -> "获取监测对象列表";
            case "getRealtimeFromRelatives" -> "获取实时血糖数据";
            case "getHistoricalGlucose" -> "获取历史血糖数据";
            default -> operation;
        };
    }

    private static String padBase64(String base64) {
        int padding = (4 - base64.length() % 4) % 4;
        return base64 + "=".repeat(padding);
    }
}

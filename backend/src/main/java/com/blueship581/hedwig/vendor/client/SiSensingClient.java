package com.blueship581.hedwig.vendor.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.blueship581.hedwig.domain.enums.TrendDirection;
import com.blueship581.hedwig.exception.VendorException;
import com.blueship581.hedwig.vendor.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
public class SiSensingClient implements VendorClient {

  private static final String BASE_URL = "https://api.sisensing.com";

  private final WebClient webClient;

  public SiSensingClient(WebClient.Builder webClientBuilder) {
    this.webClient = webClientBuilder.baseUrl(BASE_URL).build();
  }

  @Override
  public VendorTokenInfo validateToken(String accessToken) {
    String normalizedToken = VendorTokenNormalizer.normalize(accessToken);
    if (normalizedToken == null || normalizedToken.isBlank()) {
      return VendorTokenInfo.builder().valid(false).build();
    }
    try {
      String body =
          webClient
              .get()
              .uri("/auth/token/info")
              .headers(h -> buildHeaders(h, normalizedToken))
              .retrieve()
              .bodyToMono(String.class)
              .block();

      JSONObject response = JSON.parseObject(body);
      if (response == null || isFailureResponse(response)) {
        return VendorTokenInfo.builder().valid(false).build();
      }

      JSONObject tokenInfo = extractResponseData(response);
      if (tokenInfo == null) {
        return VendorTokenInfo.builder().valid(false).build();
      }

      String returnedToken = firstText(tokenInfo, "token", "accessToken");
      if (returnedToken == null || returnedToken.isBlank()) {
        return VendorTokenInfo.builder().valid(false).build();
      }

      String userId = firstText(tokenInfo, "userId", "id");
      Instant expiresAt = null;
      String expireTimeStr = tokenInfo.getString("expireTime");
      if (expireTimeStr != null && !expireTimeStr.isBlank()) {
        expiresAt = Instant.ofEpochMilli(Long.parseLong(expireTimeStr));
      }

      boolean valid = expiresAt == null || Instant.now().isBefore(expiresAt);
      return VendorTokenInfo.builder()
          .token(returnedToken)
          .userId(userId)
          .expiresAt(expiresAt)
          .valid(valid)
          .build();

    } catch (WebClientResponseException e) {
      int status = e.getStatusCode().value();
      if (status == 400 || status == 401 || status == 403) {
        log.debug("SiSensing token validation failed with status {}", e.getStatusCode());
        return VendorTokenInfo.builder().valid(false).build();
      }
      throw apiError("校验访问令牌", e);
    } catch (Exception e) {
      throw new VendorException("硅基轻享访问令牌校验失败，请稍后重试", e);
    }
  }

  @Override
  public VendorTokenInfo login(VendorLoginRequest loginRequest) {
    String phone = normalizeCredential(loginRequest.getUsername());
    String password = normalizeCredential(loginRequest.getPassword());

    if (phone == null || password == null) {
      throw new VendorException("请输入硅基账号（手机号）和密码");
    }

    try {
      String payload = JSON.toJSONString(Map.of("phone", phone, "password", password));
      String encryptedPayload;
      try {
        encryptedPayload = SiSensingCrypto.encrypt(payload);
      } catch (Exception e) {
        throw new VendorException("硅基轻享登录请求加密失败", e);
      }

      String body =
          webClient
              .post()
              .uri("/lite-sense-app/user/password/login")
              .headers(h -> {
                buildCommonHeaders(h);
                h.set("en-request", "1");
                h.set("de-response", "1");
              })
              .body(Mono.just(encryptedPayload.getBytes(StandardCharsets.UTF_8)), byte[].class)
              .retrieve()
              .bodyToMono(String.class)
              .block();

      JSONObject response = JSON.parseObject(body);
      if (response == null) {
        throw new VendorException("硅基轻享登录接口返回为空");
      }
      if (isFailureResponse(response)) {
        String detail = extractErrorDetail(response.toJSONString());
        throw new VendorException("硅基轻享账号密码登录失败" + (detail != null && !detail.isBlank() ? "：" + detail : ""));
      }

      // data 字段可能是加密字符串，尝试解密
      Object dataRaw = response.get("data");
      if (dataRaw instanceof String dataStr && !dataStr.isBlank()) {
        try {
          String decrypted = SiSensingCrypto.decrypt(dataStr);
          log.debug("SiSensing login response decrypted: {}", decrypted);
          JSONObject decryptedData = JSON.parseObject(decrypted);
          if (decryptedData != null) {
            response.put("data", decryptedData);
          }
        } catch (Exception e) {
          log.debug("SiSensing login data field is not encrypted, using as-is");
        }
      }

      JSONObject data = extractResponseData(response);
      String token = data != null
          ? firstText(data, "token", "accessToken", "access_token", "authorization", "bearerToken")
          : null;
      if (token == null || token.isBlank()) {
        throw new VendorException("硅基轻享登录成功，但未返回访问令牌");
      }

      VendorTokenInfo tokenInfo = validateToken(token);
      if (!tokenInfo.isValid()) {
        throw new VendorException("硅基轻享登录成功，但返回的访问令牌校验失败");
      }
      return tokenInfo;

    } catch (VendorException e) {
      throw e;
    } catch (WebClientResponseException e) {
      throw apiError("账号密码登录", e);
    } catch (Exception e) {
      throw new VendorException("硅基轻享账号密码登录失败", e);
    }
  }

  @Override
  public List<VendorSubject> getMonitoredSubjects(String accessToken, String vendorUserId) {
    try {
      String body =
          webClient
              .get()
              .uri(uriBuilder -> uriBuilder
                  .path("/lite-sense-app/follow/list")
                  .queryParam("pageNum", "1")
                  .queryParam("pageSize", "9999")
                  .queryParam("status", "3")
                  .build())
              .headers(h -> buildHeaders(h, accessToken))
              .retrieve()
              .bodyToMono(String.class)
              .block();

      JSONObject response = JSON.parseObject(body);
      if (response == null) {
        throw new VendorException("硅基轻享接口返回为空：获取监测对象列表");
      }
      if (isFailureResponse(response)) {
        String detail = extractErrorDetail(response.toJSONString());
        throw new VendorException("硅基轻享接口返回异常：获取监测对象列表" + (detail != null && !detail.isBlank() ? "：" + detail : ""));
      }

      JSONObject payload = extractResponseData(response);
      JSONArray records = payload == null ? null : payload.getJSONArray("records");
      if (records == null || records.isEmpty()) {
        return Collections.emptyList();
      }

      List<VendorSubject> subjects = new ArrayList<>();
      for (int i = 0; i < records.size(); i++) {
        JSONObject record = records.getJSONObject(i);
        String followId = record.getString("id");

        String displayName = null;
        JSONObject userInfo = record.getJSONObject("followedUserInfo");
        if (userInfo != null) {
          displayName = firstText(userInfo, "nickName", "userName");
        }

        Double latestGlucose = null;
        JSONObject glucoseData = record.getJSONObject("followedDeviceGlucoseDataPO");
        if (glucoseData != null && glucoseData.get("latestGlucoseValue") != null) {
          latestGlucose = glucoseData.getDoubleValue("latestGlucoseValue");
        }

        subjects.add(VendorSubject.builder()
            .subjectId(followId)
            .deviceId(null)
            .displayName(displayName)
            .latestGlucoseMmol(latestGlucose)
            .build());
      }
      return subjects;

    } catch (VendorException e) {
      throw e;
    } catch (WebClientResponseException e) {
      throw apiError("获取监测对象列表", e);
    } catch (Exception e) {
      throw new VendorException("获取硅基轻享监测对象列表失败", e);
    }
  }

  public Map<String, VendorGlucoseData> getRealtimeFromFollowList(String accessToken) {
    log.debug("[SiSensing] getRealtimeFromFollowList");
    try {
      String body =
          webClient
              .get()
              .uri(uriBuilder -> uriBuilder
                  .path("/lite-sense-app/follow/list")
                  .queryParam("pageNum", "1")
                  .queryParam("pageSize", "9999")
                  .queryParam("status", "3")
                  .build())
              .headers(h -> buildHeaders(h, accessToken))
              .retrieve()
              .bodyToMono(String.class)
              .block();

      JSONObject response = JSON.parseObject(body);
      if (response == null || isFailureResponse(response)) {
        throw new VendorException("硅基轻享接口返回异常：获取实时血糖数据");
      }

      JSONObject payload = extractResponseData(response);
      JSONArray records = payload == null ? null : payload.getJSONArray("records");
      if (records == null || records.isEmpty()) {
        return Collections.emptyMap();
      }

      Map<String, VendorGlucoseData> result = new HashMap<>();
      for (int i = 0; i < records.size(); i++) {
        JSONObject record = records.getJSONObject(i);
        String followId = record.getString("id");
        if (followId == null) continue;

        JSONObject glucoseData = record.getJSONObject("followedDeviceGlucoseDataPO");
        if (glucoseData == null || glucoseData.get("latestGlucoseValue") == null) continue;

        log.debug("[SiSensing] followId={}, glucoseDataPO={}", followId, glucoseData);

        double glucose = glucoseData.getDoubleValue("latestGlucoseValue");

        Instant readingTime = null;
        for (String field : List.of("latestGlucoseTime", "monitorTime", "updateTime", "createTime")) {
          if (glucoseData.get(field) != null) {
            long ts = glucoseData.getLongValue(field);
            if (ts > 0) {
              readingTime = Instant.ofEpochMilli(ts);
              break;
            }
          }
        }
        if (readingTime == null) continue;

        result.put(followId, VendorGlucoseData.builder()
            .glucoseMmol(glucose)
            .readingTime(readingTime)
            .trendDirection(mapSiSensingTrend(extractTrendRaw(glucoseData)))
            .build());
      }
      log.debug("[SiSensing] getRealtimeFromFollowList returned {} subjects", result.size());
      return result;

    } catch (VendorException e) {
      throw e;
    } catch (WebClientResponseException e) {
      throw apiError("获取实时血糖数据", e);
    } catch (Exception e) {
      throw new VendorException("获取硅基轻享实时血糖数据失败", e);
    }
  }

  @Override
  public VendorGlucoseData getLatestGlucose(String accessToken, String vendorUserId, VendorSubject subject) {
    List<VendorGlucoseData> history = getHistoricalGlucose(accessToken, vendorUserId, subject);
    if (history.isEmpty()) {
      throw new VendorException("监测对象暂无血糖数据：" + subject.getSubjectId());
    }
    return history.get(history.size() - 1);
  }

  @Override
  public List<VendorGlucoseData> getHistoricalGlucose(String accessToken, String vendorUserId, VendorSubject subject) {
    try {
      String body =
          webClient
              .get()
              .uri(uriBuilder -> uriBuilder
                  .path("/lite-sense-app/follow/glucose")
                  .queryParam("followId", subject.getSubjectId())
                  .build())
              .headers(h -> buildHeaders(h, accessToken))
              .retrieve()
              .bodyToMono(String.class)
              .block();

      JSONObject response = JSON.parseObject(body);
      if (response == null) {
        throw new VendorException("硅基轻享接口返回为空：获取历史血糖数据");
      }
      if (isFailureResponse(response)) {
        String detail = extractErrorDetail(response.toJSONString());
        throw new VendorException("硅基轻享接口返回异常：获取历史血糖数据" + (detail != null && !detail.isBlank() ? "：" + detail : ""));
      }

      JSONObject payload = extractResponseData(response);
      JSONArray glucoseDataList = payload == null ? null : payload.getJSONArray("glucoseDataList");
      if (glucoseDataList == null || glucoseDataList.isEmpty()) {
        return Collections.emptyList();
      }

      JSONObject firstDevice = glucoseDataList.getJSONObject(0);
      JSONArray glucoseInfos = firstDevice.getJSONArray("glucoseInfos");
      if (glucoseInfos == null || glucoseInfos.isEmpty()) {
        return Collections.emptyList();
      }

      List<VendorGlucoseData> readings = new ArrayList<>();
      for (int i = 0; i < glucoseInfos.size(); i++) {
        JSONObject point = glucoseInfos.getJSONObject(i);
        if (point.containsKey("effective") && !point.getBooleanValue("effective", true)) continue;

        readings.add(VendorGlucoseData.builder()
            .glucoseMmol(point.getDoubleValue("v"))
            .readingTime(Instant.ofEpochMilli(point.getLongValue("t")))
            .trendDirection(mapSiSensingTrend(extractTrendRaw(point)))
            .build());
      }
      return readings;

    } catch (VendorException e) {
      throw e;
    } catch (WebClientResponseException e) {
      throw apiError("获取历史血糖数据", e);
    } catch (Exception e) {
      throw new VendorException("获取硅基轻享历史血糖数据失败", e);
    }
  }

  // --- 私有辅助方法 ---

  private void buildHeaders(HttpHeaders headers, String accessToken) {
    buildCommonHeaders(headers);
    headers.setBearerAuth(VendorTokenNormalizer.normalize(accessToken));
  }

  private void buildCommonHeaders(HttpHeaders headers) {
    headers.set("version", "2.0");
    headers.set("Sib-Agent", "GJQX&02.24.00.00&iphone&26.2&iPad8,6");
    headers.set("User-Agent", "ECO/3.8 (iPad; iOS 26.2; Scale/2.00)");
    headers.set("Accept", "*/*");
    headers.set("Content-Type", "application/json;charset=UTF-8");
    headers.set("lang", "zh_CN");
    headers.set("TimeZone", "Asia/Shanghai");
  }

  private boolean isFailureResponse(JSONObject response) {
    Boolean success = response.getBoolean("success");
    return success != null && !success;
  }

  private JSONObject extractResponseData(JSONObject response) {
    Object data = response.get("data");
    return data instanceof JSONObject ? (JSONObject) data : null;
  }

  private String firstText(JSONObject node, String... fieldNames) {
    for (String field : fieldNames) {
      String value = node.getString(field);
      if (value != null && !value.isBlank()) return value;
    }
    return null;
  }

  private int extractTrendRaw(JSONObject node) {
    for (String field : List.of("bloodGlucoseTrend", "trend", "s", "arrowType")) {
      if (node.get(field) != null) return node.getIntValue(field, 0);
    }
    return 0;
  }

  private String normalizeCredential(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private VendorException apiError(String operation, WebClientResponseException e) {
    String detail = extractErrorDetail(e.getResponseBodyAsString());
    String message = String.format("硅基轻享接口请求失败（%s，HTTP %s）", operation, e.getStatusCode().value());
    return new VendorException(detail != null && !detail.isBlank() ? message + "：" + detail : message, e);
  }

  private String extractErrorDetail(String body) {
    if (body == null || body.isBlank()) return null;
    try {
      JSONObject root = JSON.parseObject(body);
      String msg = firstText(root, "msg", "message", "error");
      if (msg != null) return msg;
    } catch (Exception ignored) {
    }
    return body.length() > 160 ? body.substring(0, 160) + "..." : body;
  }

  private TrendDirection mapSiSensingTrend(int s) {
    return switch (s) {
      case 3 -> TrendDirection.DOUBLE_UP;
      case 2 -> TrendDirection.SINGLE_UP;
      case 1 -> TrendDirection.FORTY_FIVE_UP;
      case 0 -> TrendDirection.FLAT;
      case -1 -> TrendDirection.FORTY_FIVE_DOWN;
      case -2 -> TrendDirection.SINGLE_DOWN;
      case -3 -> TrendDirection.DOUBLE_DOWN;
      default -> TrendDirection.NONE;
    };
  }
}

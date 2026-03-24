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

import java.time.Instant;
import java.util.*;

/**
 * Client for the SiSensing (硅基仿生) CGM vendor API.
 *
 * <p>Base URL: https://api.sisensing.com Auth: Bearer token (UUID format, ~20 min expiry per docs,
 * but validated via /auth/token/info) Login: phone/password via /lite-sense-app/user/password/login
 */
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
        long expireMs = Long.parseLong(expireTimeStr);
        expiresAt = Instant.ofEpochMilli(expireMs);
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
      String body =
          webClient
              .post()
              .uri("/lite-sense-app/user/password/login")
              .headers(this::buildCommonHeaders)
              .bodyValue(
                  Map.of(
                      "phone", phone,
                      "password", password))
              .retrieve()
              .bodyToMono(String.class)
              .block();

      JSONObject response = JSON.parseObject(body);
      if (response == null) {
        throw new VendorException("硅基轻享登录接口返回为空");
      }

      if (isFailureResponse(response)) {
        String detail = extractErrorDetail(response.toJSONString());
        if (detail == null || detail.isBlank()) {
          throw new VendorException("硅基轻享账号密码登录失败");
        }
        throw new VendorException("硅基轻享账号密码登录失败：" + detail);
      }

      String token = extractLoginToken(response);
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
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/lite-sense-app/follow/list")
                          .queryParam("pageNum", "1")
                          .queryParam("pageSize", "9999")
                          .queryParam("status", "3") // confirmed follow relationships
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
        if (detail == null || detail.isBlank()) {
          throw new VendorException("硅基轻享接口返回异常：获取监测对象列表");
        }
        throw new VendorException("硅基轻享接口返回异常：获取监测对象列表：" + detail);
      }

      JSONObject payload = extractResponseData(response);
      JSONArray records = payload == null ? null : payload.getJSONArray("records");
      if (records == null || records.isEmpty()) {
        return Collections.emptyList();
      }

      List<VendorSubject> subjects = new ArrayList<>();
      for (int i = 0; i < records.size(); i++) {
        JSONObject record = records.getJSONObject(i);
        String followId = record.containsKey("id") ? record.getString("id") : null;
        String displayName = null;
        if (record.containsKey("followedUserInfo")) {
          JSONObject userInfo = record.getJSONObject("followedUserInfo");
          if (userInfo != null) {
            displayName =
                userInfo.containsKey("nickName")
                    ? userInfo.getString("nickName")
                    : (userInfo.containsKey("userName") ? userInfo.getString("userName") : null);
          }
        }

        Double latestGlucose = null;
        if (record.containsKey("followedDeviceGlucoseDataPO")
            && record.get("followedDeviceGlucoseDataPO") != null) {
          JSONObject glucoseData = record.getJSONObject("followedDeviceGlucoseDataPO");
          if (glucoseData != null
              && glucoseData.containsKey("latestGlucoseValue")
              && glucoseData.get("latestGlucoseValue") != null) {
            latestGlucose = glucoseData.getDoubleValue("latestGlucoseValue");
          }
        }

        subjects.add(
            VendorSubject.builder()
                .subjectId(followId) // SiSensing uses followId as the identifier
                .deviceId(null) // not needed; glucose fetched by followId
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

  /**
   * Lightweight realtime fetch: one GET to /follow/list returns latest glucose for ALL monitored
   * subjects via followedDeviceGlucoseDataPO.
   *
   * @return map of followId (subjectId) -> VendorGlucoseData
   */
  public Map<String, VendorGlucoseData> getRealtimeFromFollowList(String accessToken) {
    log.debug("[SiSensing] getRealtimeFromFollowList");
    try {
      String body =
          webClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
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
        String followId = record.containsKey("id") ? record.getString("id") : null;
        if (followId == null) {
          continue;
        }

        JSONObject glucoseData = record.getJSONObject("followedDeviceGlucoseDataPO");
        if (glucoseData == null) {
          continue;
        }
        log.debug("[SiSensing] followId={}, glucoseDataPO={}", followId, glucoseData);

        if (!glucoseData.containsKey("latestGlucoseValue")
            || glucoseData.get("latestGlucoseValue") == null) {
          continue;
        }

        double glucose = glucoseData.getDoubleValue("latestGlucoseValue");

        // Extract timestamp: try common field names
        Instant readingTime = null;
        for (String field :
            List.of("latestGlucoseTime", "monitorTime", "updateTime", "createTime")) {
          if (glucoseData.containsKey(field) && glucoseData.get(field) != null) {
            long ts = glucoseData.getLongValue(field);
            if (ts > 0) {
              readingTime = Instant.ofEpochMilli(ts);
              break;
            }
          }
        }
        if (readingTime == null) {
          continue; // no timestamp = unusable
        }

        int trendRaw = extractTrendRaw(glucoseData);

        result.put(
            followId,
            VendorGlucoseData.builder()
                .glucoseMmol(glucose)
                .readingTime(readingTime)
                .trendDirection(mapSiSensingTrend(trendRaw))
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
  public VendorGlucoseData getLatestGlucose(
      String accessToken, String vendorUserId, VendorSubject subject) {
    List<VendorGlucoseData> history = getHistoricalGlucose(accessToken, vendorUserId, subject);
    if (history.isEmpty()) {
      throw new VendorException("监测对象暂无血糖数据：" + subject.getSubjectId());
    }
    return history.get(history.size() - 1);
  }

  @Override
  public List<VendorGlucoseData> getHistoricalGlucose(
      String accessToken, String vendorUserId, VendorSubject subject) {
    try {
      String body =
          webClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
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
        if (detail == null || detail.isBlank()) {
          throw new VendorException("硅基轻享接口返回异常：获取历史血糖数据");
        }
        throw new VendorException("硅基轻享接口返回异常：获取历史血糖数据：" + detail);
      }

      JSONObject payload = extractResponseData(response);
      JSONArray glucoseDataList = payload == null ? null : payload.getJSONArray("glucoseDataList");
      if (glucoseDataList == null || glucoseDataList.isEmpty()) {
        return Collections.emptyList();
      }

      // Take first device's data (primary device)
      JSONObject firstDevice = glucoseDataList.getJSONObject(0);
      JSONArray glucoseInfos = firstDevice.getJSONArray("glucoseInfos");
      if (glucoseInfos == null || glucoseInfos.isEmpty()) {
        return Collections.emptyList();
      }

      List<VendorGlucoseData> readings = new ArrayList<>();
      for (int i = 0; i < glucoseInfos.size(); i++) {
        JSONObject point = glucoseInfos.getJSONObject(i);
        if (point.containsKey("effective") && !point.getBooleanValue("effective", true)) {
          continue;
        }
        double glucoseMmol = point.getDoubleValue("v");
        long timestampMs = point.getLongValue("t");
        int trendRaw = extractTrendRaw(point);
        TrendDirection trend = mapSiSensingTrend(trendRaw);

        readings.add(
            VendorGlucoseData.builder()
                .glucoseMmol(glucoseMmol)
                .readingTime(Instant.ofEpochMilli(timestampMs))
                .trendDirection(trend)
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

  private VendorException apiError(String operation, WebClientResponseException e) {
    String detail = extractErrorDetail(e.getResponseBodyAsString());
    String message = String.format("硅基轻享接口请求失败（%s，HTTP %s）", operation, e.getStatusCode().value());
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

  private String extractLoginToken(JSONObject response) {
    JSONObject data = extractResponseData(response);
    if (data == null) {
      return firstText(response, "token", "accessToken");
    }
    // If data is a simple string value in the envelope
    Object dataRaw = response.get("data");
    if (dataRaw instanceof String) {
      return (String) dataRaw;
    }
    String nested = firstText(data, "token", "accessToken", "authorization", "bearerToken");
    if (nested != null) {
      return nested;
    }
    return firstText(response, "token", "accessToken");
  }

  private boolean isFailureResponse(JSONObject response) {
    if (!isEnvelopeResponse(response)) {
      return false;
    }

    Boolean success = response.getBoolean("success");
    if (success != null) {
      return !success;
    }

    Object code = response.get("code");
    if (code == null) {
      return false;
    }

    if (code instanceof Number) {
      return ((Number) code).intValue() != 200;
    }

    String codeText = code.toString();
    return !"200".equals(codeText) && !"0".equals(codeText) && !"OK".equalsIgnoreCase(codeText);
  }

  private boolean isEnvelopeResponse(JSONObject response) {
    return response != null
        && (response.containsKey("success")
            || response.containsKey("code")
            || response.containsKey("msg")
            || response.containsKey("errorData"));
  }

  private JSONObject extractResponseData(JSONObject response) {
    if (response == null) {
      return null;
    }
    if (!isEnvelopeResponse(response)) {
      return response;
    }
    Object data = response.get("data");
    if (data == null) {
      return null;
    }
    if (data instanceof JSONObject) {
      return (JSONObject) data;
    }
    return null;
  }

  private String firstText(JSONObject node, String... fieldNames) {
    if (node == null) {
      return null;
    }
    for (String fieldName : fieldNames) {
      String value = node.getString(fieldName);
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private int extractTrendRaw(JSONObject node) {
    if (node == null) {
      return 0;
    }
    for (String field : List.of("bloodGlucoseTrend", "trend", "s", "arrowType")) {
      if (node.containsKey(field) && node.get(field) != null) {
        return node.getIntValue(field, 0);
      }
    }
    return 0;
  }

  private String normalizeCredential(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  private void buildHeaders(org.springframework.http.HttpHeaders headers, String accessToken) {
    buildCommonHeaders(headers);
    String normalizedToken = VendorTokenNormalizer.normalize(accessToken);
    headers.setBearerAuth(normalizedToken);
  }

  private void buildCommonHeaders(org.springframework.http.HttpHeaders headers) {
    headers.set("version", "2.0");
    headers.set("Sib-Agent", "GJQX&02.24.00.00&iphone&26.2&iPad8,6");
    headers.set("User-Agent", "ECO/3.8 (iPad; iOS 26.2; Scale/2.00)");
    headers.set("Accept", "*/*");
    headers.set("Content-Type", "application/json;charset=UTF-8");
    headers.set("lang", "zh_CN");
    headers.set("TimeZone", "Asia/Shanghai");
  }

  /**
   * SiSensing trend mapping (extended): 3=DoubleUp, 2=SingleUp, 1=FortyFiveUp, 0=Flat,
   * -1=FortyFiveDown, -2=SingleDown, -3=DoubleDown.
   *
   * <p>Note: the exact vendor values are not fully documented; we map conservatively and use NONE
   * for unrecognized values so that the self-calculated trend (GlucoseTrendCalculator) takes
   * precedence.
   */
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

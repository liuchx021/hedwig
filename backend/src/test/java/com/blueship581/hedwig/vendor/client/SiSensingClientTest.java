package com.blueship581.hedwig.vendor.client;

import com.blueship581.hedwig.exception.VendorException;
import com.blueship581.hedwig.vendor.model.VendorGlucoseData;
import com.blueship581.hedwig.vendor.model.VendorLoginRequest;
import com.blueship581.hedwig.vendor.model.VendorSubject;
import com.blueship581.hedwig.vendor.model.VendorTokenInfo;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SiSensingClientTest {

    @Test
    void loginReturnsValidatedTokenInfoWhenVendorRespondsWithToken() {
        SiSensingClient client = new SiSensingClient(builderFor(request -> {
            String path = request.url().getPath();
            if (path.endsWith("/lite-sense-app/user/password/login")) {
                return okJson("""
                        {"code":200,"success":true,"data":{"token":"plain-token-value","userId":"1841703608321131087"}}
                        """);
            }
            if (path.endsWith("/auth/token/info")) {
                return okJson("""
                        {"code":200,"msg":"成功","success":true,"data":{"token":"plain-token-value","userId":"1841703608321131087","expireTime":"4102444800000"}}
                        """);
            }
            return Mono.error(new IllegalStateException("Unexpected path: " + path));
        }));

        VendorTokenInfo tokenInfo = client.login(VendorLoginRequest.builder()
                .username("13800138000")
                .password("secret")
                .build());

        assertEquals("plain-token-value", tokenInfo.getToken());
        assertEquals("1841703608321131087", tokenInfo.getUserId());
        assertTrue(tokenInfo.isValid());
    }

    @Test
    void validateTokenReturnsInvalidWhenVendorRespondsWithBusinessFailure() {
        SiSensingClient client = new SiSensingClient(builderFor(request -> {
            String path = request.url().getPath();
            if (path.endsWith("/auth/token/info")) {
                return okJson("""
                        {"code":401,"msg":"token失效","success":false,"data":null}
                        """);
            }
            return Mono.error(new IllegalStateException("Unexpected path: " + path));
        }));

        VendorTokenInfo tokenInfo = client.validateToken("plain-token-value");

        assertFalse(tokenInfo.isValid());
    }

    @Test
    void loginThrowsVendorExceptionWhenVendorRejectsCredentials() {
        SiSensingClient client = new SiSensingClient(builderFor(request -> {
            String path = request.url().getPath();
            if (path.endsWith("/lite-sense-app/user/password/login")) {
                return okJson("""
                        {"code":202008,"msg":"用户未注册","data":null,"success":false}
                        """);
            }
            return Mono.error(new IllegalStateException("Unexpected path: " + path));
        }));

        VendorException exception = assertThrows(VendorException.class, () -> client.login(
                VendorLoginRequest.builder()
                        .username("13800138000")
                        .password("secret")
                        .build()
        ));

        assertTrue(exception.getMessage().contains("用户未注册"));
    }

    @Test
    void validateTokenThrowsVendorExceptionWhenVendorServiceIsUnavailable() {
        SiSensingClient client = new SiSensingClient(builderFor(request -> {
            String path = request.url().getPath();
            if (path.endsWith("/auth/token/info")) {
                return errorJson(HttpStatus.SERVICE_UNAVAILABLE, """
                        {"message":"服务暂时不可用，请稍后重试"}
                        """);
            }
            return Mono.error(new IllegalStateException("Unexpected path: " + path));
        }));

        VendorException exception = assertThrows(
                VendorException.class,
                () -> client.validateToken("plain-token-value")
        );

        assertTrue(exception.getMessage().contains("校验访问令牌"));
        assertTrue(exception.getMessage().contains("503"));
    }

    @Test
    void getMonitoredSubjectsReadsWrappedRecordsPayload() {
        SiSensingClient client = new SiSensingClient(builderFor(request -> {
            String path = request.url().getPath();
            if (path.endsWith("/lite-sense-app/follow/list")) {
                return okJson("""
                        {
                          "code": 200,
                          "success": true,
                          "data": {
                            "records": [
                              {
                                "id": "follow-1",
                                "followedUserInfo": {
                                  "nickName": "铁柱"
                                },
                                "followedDeviceGlucoseDataPO": {
                                  "latestGlucoseValue": 6.8
                                }
                              }
                            ]
                          }
                        }
                        """);
            }
            return Mono.error(new IllegalStateException("Unexpected path: " + path));
        }));

        List<VendorSubject> subjects = client.getMonitoredSubjects("plain-token-value", "user-1");

        assertEquals(1, subjects.size());
        assertEquals("follow-1", subjects.get(0).getSubjectId());
        assertEquals("铁柱", subjects.get(0).getDisplayName());
        assertEquals(6.8, subjects.get(0).getLatestGlucoseMmol());
    }

    @Test
    void getHistoricalGlucoseReadsWrappedPayload() {
        SiSensingClient client = new SiSensingClient(builderFor(request -> {
            String path = request.url().getPath();
            if (path.endsWith("/lite-sense-app/follow/glucose")) {
                return okJson("""
                        {
                          "code": 200,
                          "success": true,
                          "data": {
                            "glucoseDataList": [
                              {
                                "glucoseInfos": [
                                  {
                                    "effective": true,
                                    "v": 6.4,
                                    "t": 1773927834250,
                                    "s": 1
                                  }
                                ]
                              }
                            ]
                          }
                        }
                        """);
            }
            return Mono.error(new IllegalStateException("Unexpected path: " + path));
        }));

        List<VendorGlucoseData> readings = client.getHistoricalGlucose(
                "plain-token-value",
                "user-1",
                VendorSubject.builder().subjectId("follow-1").build()
        );

        assertEquals(1, readings.size());
        assertEquals(6.4, readings.get(0).getGlucoseMmol());
    }

    private WebClient.Builder builderFor(ExchangeFunction exchangeFunction) {
        return WebClient.builder().exchangeFunction(exchangeFunction);
    }

    private Mono<ClientResponse> okJson(String body) {
        return Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build());
    }

    private Mono<ClientResponse> errorJson(HttpStatus status, String body) {
        return Mono.just(ClientResponse.create(status)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build());
    }
}

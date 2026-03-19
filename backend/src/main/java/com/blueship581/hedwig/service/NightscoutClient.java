package com.blueship581.hedwig.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;

/**
 * Client for pushing glucose data to a Nightscout server.
 * POST /api/v1/entries with header api-secret = SHA1(secret)
 */
@Slf4j
@Component
public class NightscoutClient {

    @Value("${nightscout.url:}")
    private String nightscoutUrl;

    @Value("${nightscout.api-secret:}")
    private String nightscoutApiSecret;

    @Value("${nightscout.enabled:false}")
    private boolean enabled;

    @Value("${TZ:Asia/Shanghai}")
    private String timezone;

    private final WebClient.Builder webClientBuilder;

    public NightscoutClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public boolean isEnabled() {
        return enabled && nightscoutUrl != null && !nightscoutUrl.isBlank();
    }

    /**
     * Push a list of SGV entries to Nightscout.
     * Returns the number of successfully pushed entries.
     */
    public int pushEntries(List<SgvEntry> entries) {
        if (!isEnabled()) {
            log.debug("Nightscout sync is disabled, skipping push of {} entries", entries.size());
            return 0;
        }

        try {
            String apiSecretHash = sha1(nightscoutApiSecret);
            WebClient client = webClientBuilder.baseUrl(nightscoutUrl).build();

            client.post()
                    .uri("/api/v1/entries")
                    .header("api-secret", apiSecretHash)
                    .header("Content-Type", "application/json")
                    .bodyValue(entries)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Pushed {} entries to Nightscout", entries.size());
            return entries.size();

        } catch (WebClientResponseException e) {
            log.error("Nightscout push failed with status {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return 0;
        } catch (Exception e) {
            log.error("Nightscout push failed", e);
            return 0;
        }
    }

    private String sha1(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("系统不支持 Nightscout 所需的 SHA-1 摘要算法", e);
        }
    }

    public SgvEntry toSgvEntry(double glucoseMmol, Instant readingTime, String direction, String device) {
        int mgdl = (int) Math.round(glucoseMmol * 18.0182);
        String dateStr = DateTimeFormatter.ISO_OFFSET_DATE_TIME
                .withZone(ZoneId.of(timezone))
                .format(readingTime);
        return SgvEntry.builder()
                .type("sgv")
                .sgv(mgdl)
                .date(readingTime.toEpochMilli())
                .dateString(dateStr)
                .direction(direction)
                .device(device)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SgvEntry {
        @Builder.Default
        private String type = "sgv";
        private int sgv;
        private long date;
        private String dateString;
        private String direction;
        private String device;
    }
}

package com.blueship581.hedwig.service;

import com.blueship581.hedwig.domain.entity.NightscoutTarget;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Client for pushing glucose data to a Nightscout server.
 * Supports both dynamic targets (from DB) and legacy config-based mode.
 */
@Slf4j
@Component
public class NightscoutClient {

    private static final double MMOL_TO_MGDL_FACTOR = 18.0182;

    @Value("${TZ:Asia/Shanghai}")
    private String timezone;

    private final WebClient.Builder webClientBuilder;

    public NightscoutClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    /**
     * Push entries to a specific Nightscout target (DB-driven).
     * Returns the number of successfully pushed entries.
     */
    public int pushEntries(NightscoutTarget target, List<SgvEntry> entries) {
        try {
            WebClient client = webClientBuilder.baseUrl(target.getBaseUrl()).build();

            client.post()
                    .uri("/api/v1/entries")
                    .header("api-secret", target.getApiSecretSha1())
                    .header("Content-Type", "application/json")
                    .bodyValue(entries)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Pushed {} entries to Nightscout target '{}' ({})",
                    entries.size(), target.getName(), target.getBaseUrl());
            return entries.size();

        } catch (WebClientResponseException e) {
            log.error("Nightscout push failed for target '{}' with status {}: {}",
                    target.getName(), e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Nightscout push failed for target '{}'", target.getName(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Test connectivity to a Nightscout target by fetching server status.
     */
    public boolean testConnection(NightscoutTarget target) {
        try {
            WebClient client = webClientBuilder.baseUrl(target.getBaseUrl()).build();

            client.get()
                    .uri("/api/v1/status")
                    .header("api-secret", target.getApiSecretSha1())
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            return true;
        } catch (Exception e) {
            log.warn("Nightscout connection test failed for target '{}': {}",
                    target.getName(), e.getMessage());
            return false;
        }
    }

    /**
     * Build an SgvEntry from mmol/L glucose value.
     * Converts mmol to mg/dL internally.
     */
    public SgvEntry toSgvEntry(double glucoseMmol, Instant readingTime, String direction, String device) {
        int mgdl = (int) Math.round(glucoseMmol * MMOL_TO_MGDL_FACTOR);
        return buildSgvEntry(mgdl, readingTime, direction, device);
    }

    /**
     * Build an SgvEntry from a pre-computed mg/dL value (avoids redundant conversion).
     */
    public SgvEntry toSgvEntryFromMgdl(double glucoseMgdl, Instant readingTime, String direction, String device) {
        return buildSgvEntry((int) Math.round(glucoseMgdl), readingTime, direction, device);
    }

    private SgvEntry buildSgvEntry(int mgdl, Instant readingTime, String direction, String device) {
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

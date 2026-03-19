package com.blueship581.hedwig.controller;

import com.blueship581.hedwig.domain.enums.TokenStatus;
import com.blueship581.hedwig.domain.repository.GlucoseReadingRepository;
import com.blueship581.hedwig.domain.repository.VendorConnectionRepository;
import com.blueship581.hedwig.service.NightscoutClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/status")
@RequiredArgsConstructor
public class StatusController {

    private final VendorConnectionRepository connectionRepository;
    private final GlucoseReadingRepository readingRepository;
    private final NightscoutClient nightscoutClient;

    /**
     * GET /api/status
     * Returns overall system health and statistics.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> status() {
        long totalConnections = connectionRepository.count();
        long activeConnections = connectionRepository.findByTokenStatusNot(TokenStatus.EXPIRED).size();
        long expiringSoon = connectionRepository
                .findAllByTokenStatusIn(List.of(TokenStatus.EXPIRING_SOON)).size();
        long totalReadings = readingRepository.count();
        long pendingSync = readingRepository.findByPushedToNightscoutFalse().size();

        return ResponseEntity.ok(Map.of(
                "timestamp", Instant.now().toString(),
                "status", "UP",
                "connections", Map.of(
                        "total", totalConnections,
                        "active", activeConnections,
                        "expiringSoon", expiringSoon
                ),
                "readings", Map.of(
                        "total", totalReadings,
                        "pendingNightscoutSync", pendingSync
                ),
                "nightscout", Map.of(
                        "enabled", nightscoutClient.isEnabled()
                )
        ));
    }
}

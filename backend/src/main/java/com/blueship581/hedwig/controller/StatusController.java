package com.blueship581.hedwig.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.blueship581.hedwig.domain.entity.GlucoseReading;
import com.blueship581.hedwig.domain.entity.VendorConnection;
import com.blueship581.hedwig.domain.enums.TokenStatus;
import com.blueship581.hedwig.domain.mapper.GlucoseReadingMapper;
import com.blueship581.hedwig.domain.mapper.VendorConnectionMapper;
import com.blueship581.hedwig.service.NightscoutTargetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/status")
@RequiredArgsConstructor
public class StatusController {

  private final VendorConnectionMapper connectionMapper;
  private final GlucoseReadingMapper readingMapper;
  private final NightscoutTargetService targetService;

  @GetMapping
  public ResponseEntity<Map<String, Object>> status() {
    long totalConnections = connectionMapper.selectCount(null);
    long activeConnections =
        connectionMapper.selectCount(
            Wrappers.lambdaQuery(VendorConnection.class)
                .ne(VendorConnection::getTokenStatus, TokenStatus.EXPIRED));
    long expiringSoon =
        connectionMapper.selectCount(
            Wrappers.lambdaQuery(VendorConnection.class)
                .eq(VendorConnection::getTokenStatus, TokenStatus.EXPIRING_SOON));
    long totalReadings = readingMapper.selectCount(null);
    long pendingSync =
        readingMapper.selectCount(
            Wrappers.lambdaQuery(GlucoseReading.class)
                .eq(GlucoseReading::getPushedToNightscout, false));

    int activeTargets = targetService.getAllActiveTargets().size();

    return ResponseEntity.ok(
        Map.of(
            "timestamp", Instant.now().toString(),
            "status", "UP",
            "connections",
                Map.of(
                    "total", totalConnections,
                    "active", activeConnections,
                    "expiringSoon", expiringSoon),
            "readings",
                Map.of(
                    "total", totalReadings,
                    "pendingNightscoutSync", pendingSync),
            "nightscout", Map.of("activeTargets", activeTargets)));
  }
}

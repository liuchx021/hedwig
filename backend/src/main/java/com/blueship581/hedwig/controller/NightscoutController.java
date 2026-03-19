package com.blueship581.hedwig.controller;

import com.blueship581.hedwig.service.NightscoutSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/nightscout")
@RequiredArgsConstructor
public class NightscoutController {

    private final NightscoutSyncService nightscoutSyncService;

    /**
     * Manually trigger Nightscout sync of all pending readings.
     * POST /api/nightscout/sync
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> sync() {
        int pushed = nightscoutSyncService.syncPendingReadings();
        return ResponseEntity.ok(Map.of(
                "pushed", pushed,
                "message", pushed > 0 ? "同步完成" : "当前没有待同步的血糖数据，或 Nightscout 同步未启用"
        ));
    }
}

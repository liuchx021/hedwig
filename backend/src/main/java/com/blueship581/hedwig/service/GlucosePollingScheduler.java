package com.blueship581.hedwig.service;

import com.blueship581.hedwig.domain.entity.MonitoredSubject;
import com.blueship581.hedwig.domain.entity.VendorConnection;
import com.blueship581.hedwig.domain.enums.TokenStatus;
import com.blueship581.hedwig.domain.repository.MonitoredSubjectRepository;
import com.blueship581.hedwig.domain.repository.VendorConnectionRepository;
import com.blueship581.hedwig.vendor.client.OttaiClient;
import com.blueship581.hedwig.vendor.client.VendorClient;
import com.blueship581.hedwig.vendor.client.VendorClientFactory;
import com.blueship581.hedwig.vendor.model.VendorGlucoseData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Adaptive-window glucose polling scheduler.
 *
 * <p>A fast 2-second clock tick drives all connections, but actual vendor API
 * requests are gated by {@link PollingStateManager} which implements a
 * multi-phase adaptive window around the predicted next data arrival time.
 *
 * <p>Maintenance tasks (token expiry checks, subject sync) run on a separate
 * low-frequency schedule to avoid polluting the polling loop.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GlucosePollingScheduler {

    private final VendorConnectionRepository connectionRepository;
    private final MonitoredSubjectRepository subjectRepository;
    private final GlucoseService glucoseService;
    private final NightscoutSyncService nightscoutSyncService;
    private final TokenExpiryMonitorService tokenExpiryMonitorService;
    private final VendorConnectionService vendorConnectionService;
    private final VendorClientFactory vendorClientFactory;
    private final PollingStateManager pollingState;

    // ════════════════════════════════════════════════════════════════════════
    // Fast clock — 2s tick, actual requests gated by PollingStateManager
    // ════════════════════════════════════════════════════════════════════════

    @Scheduled(fixedDelay = 2000)
    public void tick() {
        List<VendorConnection> activeConnections = connectionRepository
                .findByTokenStatusNot(TokenStatus.EXPIRED);

        for (VendorConnection connection : activeConnections) {
            if (!pollingState.shouldPollNow(connection.getId())) {
                continue;
            }

            log.debug("Polling connection {} (phase={}, health={})",
                    connection.getId(),
                    pollingState.getCurrentPhase(connection.getId()),
                    pollingState.getHealth(connection.getId()));

            pollConnection(connection);
        }

        // Push any pending readings to Nightscout (cheap local check)
        int synced = nightscoutSyncService.syncPendingReadings();
        if (synced > 0) {
            log.info("Nightscout sync: pushed {} readings", synced);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Slow maintenance — 10 min: token check + subject sync
    // ════════════════════════════════════════════════════════════════════════

    @Scheduled(fixedDelay = 600_000)
    public void maintenance() {
        tokenExpiryMonitorService.checkAll();

        List<VendorConnection> active = connectionRepository
                .findByTokenStatusNot(TokenStatus.EXPIRED);

        for (VendorConnection conn : active) {
            try {
                vendorConnectionService.syncSubjects(conn);
            } catch (Exception e) {
                log.warn("Subject sync failed for connection {}: {}",
                        conn.getId(), e.getMessage());
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Polling logic
    // ════════════════════════════════════════════════════════════════════════

    private void pollConnection(VendorConnection connection) {
        List<MonitoredSubject> subjects = subjectRepository
                .findByVendorConnectionIdAndIsActive(connection.getId(), true);
        if (subjects.isEmpty()) {
            return;
        }

        VendorClient client = vendorClientFactory.getClient(connection.getVendorType());

        if (client instanceof OttaiClient ottaiClient) {
            pollViaRelatives(ottaiClient, connection, subjects);
        } else {
            pollPerSubject(connection, subjects);
        }
    }

    /**
     * Ottai path: one lightweight /relatives call returns realtime glucose
     * for ALL subjects (with receiveTime + arrowType).
     */
    private void pollViaRelatives(OttaiClient ottaiClient,
                                  VendorConnection connection,
                                  List<MonitoredSubject> subjects) {
        try {
            Map<String, VendorGlucoseData> realtimeData =
                    ottaiClient.getRealtimeFromRelatives(
                            connection.getAccessToken(), connection.getVendorUserId());

            boolean hasNew = false;
            Instant latestReceiveTime = null;

            for (MonitoredSubject subject : subjects) {
                VendorGlucoseData data = realtimeData.get(subject.getVendorSubjectId());
                if (data == null) {
                    continue;
                }
                try {
                    if (glucoseService.saveIfNewReading(subject.getId(), data)) {
                        hasNew = true;
                        log.info("New glucose via relatives: subject={}, glucose={}, time={}",
                                subject.getDisplayName(), data.getGlucoseMmol(),
                                data.getReadingTime());
                        if (latestReceiveTime == null
                                || data.getReadingTime().isAfter(latestReceiveTime)) {
                            latestReceiveTime = data.getReadingTime();
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to save glucose for subject {}: {}",
                            subject.getId(), e.getMessage());
                }
            }

            if (hasNew && latestReceiveTime != null) {
                pollingState.onDataReceived(connection.getId(), latestReceiveTime);
                connection.setLastSyncedAt(Instant.now());
                connectionRepository.save(connection);
            }

        } catch (Exception e) {
            log.error("Failed to poll via relatives for connection {}: {}",
                    connection.getId(), e.getMessage());
            pollingState.onError(connection.getId());
        }
    }

    /**
     * Fallback for non-Ottai vendors: per-subject polling via heavy endpoint.
     */
    private void pollPerSubject(VendorConnection connection,
                                List<MonitoredSubject> subjects) {
        boolean hasNew = false;

        for (MonitoredSubject subject : subjects) {
            try {
                if (glucoseService.pollLatest(connection.getId(), subject.getId())) {
                    hasNew = true;
                }
            } catch (Exception e) {
                log.error("Failed to poll glucose for subject {} (connection {}): {}",
                        subject.getId(), connection.getId(), e.getMessage());
            }
        }
        if (hasNew) {
            pollingState.onDataReceived(connection.getId(), Instant.now());
        }
    }
}

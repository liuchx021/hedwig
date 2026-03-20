package com.blueship581.hedwig.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-connection adaptive polling state manager.
 *
 * <p>Phases relative to T_next (= lastReceiveTime + dataInterval + meanOffset):
 * <pre>
 *  IDLE          EARLY_PROBE       CORE_WINDOW    LATE_CHASE    TIMEOUT_BACKOFF
 * |──────────────|─────────────────|──────────────|─────────────|───────────────→
 *             T-25s              T-5s           T+5s          T+30s
 *               8s/req            4s/req         6s/req       exponential
 * </pre>
 */
@Slf4j
@Component
public class PollingStateManager {

    @Value("${polling.early-probe-start-sec:25}")
    private long earlyProbeStartSec;

    @Value("${polling.core-radius-sec:5}")
    private long coreRadiusSec;

    @Value("${polling.late-end-sec:30}")
    private long lateEndSec;

    @Value("${polling.early-interval-sec:8}")
    private int earlyIntervalSec;

    @Value("${polling.core-interval-sec:4}")
    private int coreIntervalSec;

    @Value("${polling.late-interval-sec:6}")
    private int lateIntervalSec;

    @Value("${polling.max-backoff-sec:60}")
    private int maxBackoffSec;

    @Value("${polling.interval-ms:300000}")
    private long dataIntervalMs;

    private static final int MIN_HISTORY_FOR_ADAPTATION = 5;
    private static final int MAX_HISTORY = 20;

    @Value("${polling.degraded-timeout-count:2}")
    private int degradedThreshold;

    @Value("${polling.disconnected-timeout-count:5}")
    private int disconnectedThreshold;

    private final ConcurrentHashMap<Long, PollingState> states = new ConcurrentHashMap<>();

    /**
     * Returns true when the scheduler should fire a poll for this connection.
     */
    public boolean shouldPollNow(Long connectionId) {
        PollingState s = states.computeIfAbsent(connectionId, id -> new PollingState());
        Instant now = Instant.now();

        if (s.health == ConnectionHealth.DISCONNECTED) {
            return isIntervalElapsed(s, now, maxBackoffSec);
        }

        // No baseline yet — poll at early interval to get a first reading
        if (s.nextExpectedTime == null) {
            return isIntervalElapsed(s, now, earlyIntervalSec);
        }

        long meanOffsetMs = computeMeanOffsetMs(s);
        Instant tNext = s.nextExpectedTime.plusMillis(meanOffsetMs);
        long secToNext = now.getEpochSecond() - tNext.getEpochSecond();

        if (secToNext < -earlyProbeStartSec) {
            s.currentPhase = Phase.IDLE;
            return false;
        }

        if (secToNext < -coreRadiusSec) {
            s.currentPhase = Phase.EARLY_PROBE;
            return isIntervalElapsed(s, now, earlyIntervalSec);
        }

        if (secToNext <= coreRadiusSec) {
            s.currentPhase = Phase.CORE_WINDOW;
            return isIntervalElapsed(s, now, coreIntervalSec);
        }

        if (secToNext <= lateEndSec) {
            s.currentPhase = Phase.LATE_CHASE;
            return isIntervalElapsed(s, now, lateIntervalSec);
        }

        // TIMEOUT_BACKOFF
        s.currentPhase = Phase.TIMEOUT_BACKOFF;
        int backoff = computeBackoffSec(s);
        boolean fire = isIntervalElapsed(s, now, backoff);
        if (fire) {
            s.consecutiveTimeouts++;
            if (s.consecutiveTimeouts >= disconnectedThreshold) {
                if (s.health != ConnectionHealth.DISCONNECTED) {
                    log.warn("[PollingState] connection={} DISCONNECTED ({} timeouts)",
                            connectionId, s.consecutiveTimeouts);
                    s.health = ConnectionHealth.DISCONNECTED;
                }
            } else if (s.consecutiveTimeouts >= degradedThreshold) {
                s.health = ConnectionHealth.DEGRADED;
            }
        }
        return fire;
    }

    /**
     * New data arrived — reset counters, record offset, advance window.
     *
     * @return true if this was a recovery from DISCONNECTED state (caller should backfill).
     */
    public boolean onDataReceived(Long connectionId, Instant receiveTime) {
        PollingState s = states.computeIfAbsent(connectionId, id -> new PollingState());

        boolean wasDisconnected = (s.health == ConnectionHealth.DISCONNECTED);

        if (s.nextExpectedTime != null) {
            long offsetMs = Instant.now().toEpochMilli() - s.nextExpectedTime.toEpochMilli();
            s.offsetHistory.addLast(offsetMs);
            if (s.offsetHistory.size() > MAX_HISTORY) {
                s.offsetHistory.removeFirst();
            }
        }

        if (wasDisconnected) {
            log.info("[PollingState] connection={} RECOVERED — backfill needed", connectionId);
            s.offsetHistory.clear();
        }

        s.lastReceiveTime = receiveTime;
        s.nextExpectedTime = receiveTime.plusMillis(dataIntervalMs);
        s.consecutiveTimeouts = 0;
        s.health = ConnectionHealth.HEALTHY;
        s.currentPhase = Phase.IDLE;
        log.debug("[PollingState] connection={} nextExpected={}", connectionId, s.nextExpectedTime);
        return wasDisconnected;
    }

    /**
     * @return true if this connection has never received data (cold start).
     */
    public boolean isColdStart(Long connectionId) {
        PollingState s = states.get(connectionId);
        return s == null || s.lastReceiveTime == null;
    }

    /**
     * Poll succeeded but returned no new data (same reading as before).
     * Keeps the connection healthy and advances the window so we don't
     * falsely degrade into TIMEOUT_BACKOFF / DISCONNECTED.
     */
    public void onPollSuccessNoNewData(Long connectionId) {
        PollingState s = states.computeIfAbsent(connectionId, id -> new PollingState());
        s.consecutiveTimeouts = 0;
        if (s.health != ConnectionHealth.HEALTHY) {
            log.info("[PollingState] connection={} restored to HEALTHY (poll OK, no new data)", connectionId);
        }
        s.health = ConnectionHealth.HEALTHY;

        // Advance the window forward so we don't keep polling in the past
        if (s.nextExpectedTime != null && Instant.now().isAfter(s.nextExpectedTime)) {
            s.nextExpectedTime = Instant.now().plusMillis(dataIntervalMs);
            s.currentPhase = Phase.IDLE;
            log.debug("[PollingState] connection={} advanced nextExpected={}", connectionId, s.nextExpectedTime);
        }
    }

    /**
     * A poll threw an exception.
     */
    public void onError(Long connectionId) {
        PollingState s = states.computeIfAbsent(connectionId, id -> new PollingState());
        s.consecutiveTimeouts++;
        if (s.consecutiveTimeouts >= disconnectedThreshold) {
            s.health = ConnectionHealth.DISCONNECTED;
        }
    }

    public void remove(Long connectionId) {
        states.remove(connectionId);
    }

    public Phase getCurrentPhase(Long connectionId) {
        PollingState s = states.get(connectionId);
        return s == null ? Phase.IDLE : s.currentPhase;
    }

    public ConnectionHealth getHealth(Long connectionId) {
        PollingState s = states.get(connectionId);
        return s == null ? ConnectionHealth.HEALTHY : s.health;
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private boolean isIntervalElapsed(PollingState s, Instant now, int intervalSec) {
        if (s.lastFireTime == null
                || now.getEpochSecond() - s.lastFireTime.getEpochSecond() >= intervalSec) {
            s.lastFireTime = now;
            return true;
        }
        return false;
    }

    private int computeBackoffSec(PollingState s) {
        int base = earlyIntervalSec * 2;
        int backoff = (int) Math.min(base * Math.pow(1.5, s.consecutiveTimeouts), maxBackoffSec);
        return Math.max(backoff, base);
    }

    private long computeMeanOffsetMs(PollingState s) {
        if (s.offsetHistory.size() < MIN_HISTORY_FOR_ADAPTATION) {
            return 0;
        }
        return (long) s.offsetHistory.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    // ── inner types ─────────────────────────────────────────────────────────

    public enum Phase {
        IDLE, EARLY_PROBE, CORE_WINDOW, LATE_CHASE, TIMEOUT_BACKOFF
    }

    public enum ConnectionHealth {
        HEALTHY, DEGRADED, DISCONNECTED
    }

    private static class PollingState {
        Instant lastReceiveTime;
        Instant nextExpectedTime;
        Phase currentPhase = Phase.IDLE;
        ConnectionHealth health = ConnectionHealth.HEALTHY;
        Instant lastFireTime;
        int consecutiveTimeouts = 0;
        final LinkedList<Long> offsetHistory = new LinkedList<>();
    }
}

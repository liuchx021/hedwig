package com.blueship581.hedwig.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.blueship581.hedwig.domain.entity.VendorConnection;
import com.blueship581.hedwig.domain.enums.TokenStatus;
import com.blueship581.hedwig.domain.mapper.VendorConnectionMapper;
import com.blueship581.hedwig.vendor.client.VendorClient;
import com.blueship581.hedwig.vendor.client.VendorClientFactory;
import com.blueship581.hedwig.vendor.model.VendorTokenInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenExpiryMonitorService {

    private static final long EXPIRING_SOON_DAYS = 7;

    private final VendorConnectionMapper connectionMapper;
    private final VendorClientFactory vendorClientFactory;

    @Transactional
    public void checkAll() {
        // Check ACTIVE and EXPIRING_SOON connections
        List<VendorConnection> connections = connectionMapper.selectList(
                Wrappers.lambdaQuery(VendorConnection.class)
                        .in(VendorConnection::getTokenStatus,
                                List.of(TokenStatus.ACTIVE, TokenStatus.EXPIRING_SOON)));

        Instant now = Instant.now();
        Instant expiringThreshold = now.plus(EXPIRING_SOON_DAYS, ChronoUnit.DAYS);

        for (VendorConnection conn : connections) {
            if (conn.getTokenExpiresAt() == null) {
                continue;
            }
            if (conn.getTokenExpiresAt().isBefore(now)) {
                // Token appears expired by local record — try to refresh before giving up
                if (tryRefreshToken(conn)) {
                    log.info("Token for connection {} ({}) auto-refreshed successfully",
                            conn.getId(), conn.getVendorType());
                } else {
                    log.warn("Token for connection {} ({}) has expired and refresh failed",
                            conn.getId(), conn.getVendorType());
                    conn.setTokenStatus(TokenStatus.EXPIRED);
                }
            } else if (conn.getTokenExpiresAt().isBefore(expiringThreshold)) {
                // Proactively refresh tokens expiring soon
                if (tryRefreshToken(conn)) {
                    log.info("Token for connection {} ({}) proactively refreshed",
                            conn.getId(), conn.getVendorType());
                } else {
                    log.warn("Token for connection {} ({}) expires on {} (within {} days), refresh failed",
                            conn.getId(), conn.getVendorType(), conn.getTokenExpiresAt(), EXPIRING_SOON_DAYS);
                    conn.setTokenStatus(TokenStatus.EXPIRING_SOON);
                }
            } else {
                conn.setTokenStatus(TokenStatus.ACTIVE);
            }
            connectionMapper.updateById(conn);
        }

        // Also try to recover EXPIRED connections
        recoverExpiredConnections();
    }

    /**
     * Attempt to recover connections that were previously marked EXPIRED.
     * Some vendors (e.g., SiSensing) may still accept the token after the
     * locally-recorded expiry time if it was refreshed server-side.
     */
    private void recoverExpiredConnections() {
        List<VendorConnection> expired = connectionMapper.selectList(
                Wrappers.lambdaQuery(VendorConnection.class)
                        .eq(VendorConnection::getTokenStatus, TokenStatus.EXPIRED));

        for (VendorConnection conn : expired) {
            if (tryRefreshToken(conn)) {
                log.info("Recovered EXPIRED connection {} ({}) — token is still valid",
                        conn.getId(), conn.getVendorType());
                connectionMapper.updateById(conn);
            }
        }
    }

    /**
     * Call validateToken on the vendor API. If the token is still valid
     * (or was refreshed server-side), update the connection with the new
     * expiry and mark it ACTIVE.
     *
     * @return true if the token was successfully refreshed
     */
    private boolean tryRefreshToken(VendorConnection conn) {
        try {
            VendorClient client = vendorClientFactory.getClient(conn.getVendorType());
            VendorTokenInfo tokenInfo = client.validateToken(conn.getAccessToken());

            if (tokenInfo.isValid()) {
                if (tokenInfo.getToken() != null && !tokenInfo.getToken().isBlank()) {
                    conn.setAccessToken(tokenInfo.getToken());
                }
                conn.setTokenExpiresAt(tokenInfo.getExpiresAt());
                conn.setTokenStatus(TokenStatus.ACTIVE);
                return true;
            }
        } catch (Exception e) {
            log.debug("Token refresh failed for connection {} ({}): {}",
                    conn.getId(), conn.getVendorType(), e.getMessage());
        }
        return false;
    }
}

package com.blueship581.hedwig.service;

import com.blueship581.hedwig.domain.entity.VendorConnection;
import com.blueship581.hedwig.domain.enums.TokenStatus;
import com.blueship581.hedwig.domain.repository.VendorConnectionRepository;
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

    private final VendorConnectionRepository connectionRepository;

    @Transactional
    public void checkAll() {
        List<VendorConnection> connections = connectionRepository.findAllByTokenStatusIn(
                List.of(TokenStatus.ACTIVE, TokenStatus.EXPIRING_SOON));

        Instant now = Instant.now();
        Instant expiringThreshold = now.plus(EXPIRING_SOON_DAYS, ChronoUnit.DAYS);

        for (VendorConnection conn : connections) {
            if (conn.getTokenExpiresAt() == null) {
                continue;
            }
            if (conn.getTokenExpiresAt().isBefore(now)) {
                log.warn("Token for connection {} ({}) has expired", conn.getId(), conn.getVendorType());
                conn.setTokenStatus(TokenStatus.EXPIRED);
            } else if (conn.getTokenExpiresAt().isBefore(expiringThreshold)) {
                log.warn("Token for connection {} ({}) expires on {} (within {} days)",
                        conn.getId(), conn.getVendorType(), conn.getTokenExpiresAt(), EXPIRING_SOON_DAYS);
                conn.setTokenStatus(TokenStatus.EXPIRING_SOON);
            } else {
                conn.setTokenStatus(TokenStatus.ACTIVE);
            }
            connectionRepository.save(conn);
        }
    }
}

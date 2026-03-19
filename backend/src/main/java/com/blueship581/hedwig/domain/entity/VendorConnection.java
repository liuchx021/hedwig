package com.blueship581.hedwig.domain.entity;

import com.blueship581.hedwig.domain.enums.TokenStatus;
import com.blueship581.hedwig.domain.enums.VendorType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "vendor_connections")
public class VendorConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gatewayUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VendorType vendorType;

    @Column(nullable = false, length = 2048)
    private String accessToken;

    // The user's ID on the vendor's platform (e.g., Ottai userId, SiSensing userId)
    private String vendorUserId;

    private Instant tokenExpiresAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TokenStatus tokenStatus = TokenStatus.ACTIVE;

    private Instant lastSyncedAt;
}

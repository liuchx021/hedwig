package com.blueship581.hedwig.domain.entity;

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
@Table(name = "monitored_subjects")
public class MonitoredSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long vendorConnectionId;

    // The subject's user ID on the vendor platform (e.g., Ottai fromUserId)
    @Column(nullable = false)
    private String vendorSubjectId;

    // The subject's device ID on the vendor platform (e.g., Ottai fromUserDeviceId)
    private String vendorDeviceId;

    private String displayName;

    @Builder.Default
    private Boolean isActive = true;

    // Absolute time when the sensor expires (computed from vendor's restDeviceTime)
    private Instant sensorExpiresAt;
}

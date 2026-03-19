package com.blueship581.hedwig.domain.entity;

import com.blueship581.hedwig.domain.enums.TrendDirection;
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
@Table(name = "glucose_readings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_subject_reading_time",
                columnNames = {"monitoredSubjectId", "readingTime"}
        ))
public class GlucoseReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long monitoredSubjectId;

    @Column(nullable = false)
    private Double glucoseMmol;

    @Column(nullable = false)
    private Double glucoseMgdl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TrendDirection trendDirection = TrendDirection.NONE;

    @Column(nullable = false)
    private Instant readingTime;

    @Builder.Default
    private Boolean pushedToNightscout = false;
}

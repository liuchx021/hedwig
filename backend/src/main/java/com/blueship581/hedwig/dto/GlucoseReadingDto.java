package com.blueship581.hedwig.dto;

import com.blueship581.hedwig.domain.enums.TrendDirection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlucoseReadingDto {
    private Long id;
    private Long monitoredSubjectId;
    private Double glucoseMmol;
    private Double glucoseMgdl;
    private TrendDirection trendDirection;
    private Double delta;
    private Instant readingTime;
    private Boolean pushedToNightscout;
}

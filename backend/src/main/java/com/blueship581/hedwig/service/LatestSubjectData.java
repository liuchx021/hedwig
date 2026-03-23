package com.blueship581.hedwig.service;

import com.blueship581.hedwig.dto.GlucoseReadingDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LatestSubjectData {

    private Long subjectId;

    private GlucoseReadingDto latestReading;

    private Instant updatedAt;
}

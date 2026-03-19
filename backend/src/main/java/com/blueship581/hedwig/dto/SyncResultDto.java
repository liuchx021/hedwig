package com.blueship581.hedwig.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncResultDto {
    private int syncedCount;
    private Instant timeRangeStart;
    private Instant timeRangeEnd;
}

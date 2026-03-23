package com.blueship581.hedwig.service;

import com.blueship581.hedwig.domain.entity.GlucoseReading;
import com.blueship581.hedwig.domain.entity.NightscoutTarget;
import com.blueship581.hedwig.domain.enums.TrendDirection;
import com.blueship581.hedwig.domain.mapper.GlucoseReadingMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NightscoutSyncServiceTest {

    @Test
    void syncPendingReadingsUsesDerivedTrendInsteadOfStoredVendorTrend() {
        GlucoseReadingMapper readingMapper = mock(GlucoseReadingMapper.class);
        NightscoutClient nightscoutClient = mock(NightscoutClient.class);
        NightscoutTargetService targetService = mock(NightscoutTargetService.class);
        NightscoutSyncService service = new NightscoutSyncService(
                readingMapper,
                nightscoutClient,
                targetService
        );

        Instant previousTime = Instant.parse("2026-03-23T11:55:00Z");
        Instant latestTime = Instant.parse("2026-03-23T12:00:00Z");
        GlucoseReading latest = GlucoseReading.builder()
                .id(2L)
                .monitoredSubjectId(9L)
                .glucoseMmol(6.9)
                .glucoseMgdl(124.2)
                .trendDirection(TrendDirection.DOUBLE_DOWN)
                .readingTime(latestTime)
                .pushedToNightscout(false)
                .build();
        GlucoseReading previous = GlucoseReading.builder()
                .id(1L)
                .monitoredSubjectId(9L)
                .glucoseMmol(6.5)
                .glucoseMgdl(117.0)
                .trendDirection(TrendDirection.FLAT)
                .readingTime(previousTime)
                .pushedToNightscout(false)
                .build();
        NightscoutTarget target = NightscoutTarget.builder()
                .id(1L)
                .name("default")
                .baseUrl("https://nightscout.example.com")
                .build();

        when(targetService.getAllActiveTargets()).thenReturn(List.of(target));
        when(readingMapper.selectList(any()))
                .thenReturn(List.of(latest, previous))
                .thenReturn(List.of(latest, previous));
        when(nightscoutClient.toSgvEntry(anyDouble(), any(Instant.class), anyString(), anyString()))
                .thenAnswer(invocation -> NightscoutClient.SgvEntry.builder()
                        .sgv((int) Math.round(invocation.getArgument(0, Double.class) * 18.0182))
                        .date(invocation.getArgument(1, Instant.class).toEpochMilli())
                        .direction(invocation.getArgument(2, String.class))
                        .device(invocation.getArgument(3, String.class))
                        .build());
        when(nightscoutClient.pushEntries(eq(target), anyList())).thenReturn(2);

        service.syncPendingReadings();

        verify(nightscoutClient).pushEntries(eq(target), argThat(entries ->
                entries.stream().anyMatch(entry ->
                        entry.getDate() == latestTime.toEpochMilli()
                                && "FortyFiveUp".equals(entry.getDirection()))
        ));
        verify(readingMapper, times(2)).updateById(any(GlucoseReading.class));
    }
}

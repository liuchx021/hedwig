package com.blueship581.hedwig.service;

import com.blueship581.hedwig.domain.entity.GlucoseReading;
import com.blueship581.hedwig.domain.entity.MonitoredSubject;
import com.blueship581.hedwig.domain.entity.NightscoutTarget;
import com.blueship581.hedwig.domain.entity.VendorConnection;
import com.blueship581.hedwig.domain.enums.TrendDirection;
import com.blueship581.hedwig.domain.mapper.GlucoseReadingMapper;
import com.blueship581.hedwig.domain.mapper.MonitoredSubjectMapper;
import com.blueship581.hedwig.domain.mapper.VendorConnectionMapper;
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
    void syncPendingReadingsUsesDerivedTrendAndRespectsOwnership() {
        GlucoseReadingMapper readingMapper = mock(GlucoseReadingMapper.class);
        MonitoredSubjectMapper subjectMapper = mock(MonitoredSubjectMapper.class);
        VendorConnectionMapper connectionMapper = mock(VendorConnectionMapper.class);
        NightscoutClient nightscoutClient = mock(NightscoutClient.class);
        NightscoutTargetService targetService = mock(NightscoutTargetService.class);
        NightscoutSyncService service = new NightscoutSyncService(
                readingMapper,
                subjectMapper,
                connectionMapper,
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
                .trendDirection(TrendDirection.DOUBLE_DOWN) // vendor trend (should be ignored)
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

        // Target belongs to user 100
        NightscoutTarget target = NightscoutTarget.builder()
                .id(1L)
                .gatewayUserId(100L)
                .name("default")
                .baseUrl("https://nightscout.example.com")
                .build();

        // Subject 9 belongs to connection 50, which belongs to user 100
        MonitoredSubject subject = MonitoredSubject.builder()
                .id(9L)
                .vendorConnectionId(50L)
                .vendorSubjectId("vs-9")
                .build();
        VendorConnection connection = VendorConnection.builder()
                .id(50L)
                .gatewayUserId(100L)
                .build();

        when(targetService.getAllActiveTargets()).thenReturn(List.of(target));
        // First call: pending readings; Second call: for trend derivation
        when(readingMapper.selectList(any()))
                .thenReturn(List.of(latest, previous))  // pending
                .thenReturn(List.of(latest, previous)); // trend derivation
        when(subjectMapper.selectList(any())).thenReturn(List.of(subject));
        when(connectionMapper.selectList(any())).thenReturn(List.of(connection));
        when(nightscoutClient.toSgvEntryFromMgdl(anyDouble(), any(Instant.class), anyString(), anyString()))
                .thenAnswer(invocation -> NightscoutClient.SgvEntry.builder()
                        .sgv((int) Math.round(invocation.getArgument(0, Double.class) * 18.0182))
                        .date(invocation.getArgument(1, Instant.class).toEpochMilli())
                        .direction(invocation.getArgument(2, String.class))
                        .device(invocation.getArgument(3, String.class))
                        .build());
        when(nightscoutClient.pushEntries(eq(target), anyList())).thenReturn(2);

        service.syncPendingReadings();

        // Should use derived trend FortyFiveUp (delta=0.4 over 5 min), not vendor DOUBLE_DOWN
        verify(nightscoutClient).pushEntries(eq(target), argThat(entries ->
                entries.stream().anyMatch(entry ->
                        entry.getDate() == latestTime.toEpochMilli()
                                && "FortyFiveUp".equals(entry.getDirection()))
        ));
        // Both readings should be marked as pushed
        verify(readingMapper, times(2)).updateById(any(GlucoseReading.class));
    }
}

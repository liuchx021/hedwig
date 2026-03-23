package com.blueship581.hedwig.service;

import com.blueship581.hedwig.domain.entity.GlucoseReading;
import com.blueship581.hedwig.domain.enums.TrendDirection;
import com.blueship581.hedwig.domain.mapper.GlucoseReadingMapper;
import com.blueship581.hedwig.domain.mapper.MonitoredSubjectMapper;
import com.blueship581.hedwig.domain.mapper.VendorConnectionMapper;
import com.blueship581.hedwig.dto.GlucoseReadingDto;
import com.blueship581.hedwig.util.GlucoseConverter;
import com.blueship581.hedwig.vendor.client.VendorClientFactory;
import com.blueship581.hedwig.vendor.model.VendorGlucoseData;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GlucoseServiceTest {

    @Test
    void getLatestReadingDerivesTrendFromPreviousReading() {
        GlucoseReadingMapper readingMapper = mock(GlucoseReadingMapper.class);
        MonitoredSubjectMapper subjectMapper = mock(MonitoredSubjectMapper.class);
        VendorConnectionMapper connectionMapper = mock(VendorConnectionMapper.class);
        VendorClientFactory vendorClientFactory = mock(VendorClientFactory.class);
        GlucoseConverter glucoseConverter = mock(GlucoseConverter.class);
        LatestSubjectDataCache latestSubjectDataCache = new LatestSubjectDataCache();
        GlucoseService service = new GlucoseService(
                readingMapper,
                subjectMapper,
                connectionMapper,
                vendorClientFactory,
                glucoseConverter,
                latestSubjectDataCache
        );

        Instant latestTime = Instant.parse("2026-03-23T12:00:00Z");
        Instant previousTime = Instant.parse("2026-03-23T11:55:00Z");
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

        when(readingMapper.selectList(any())).thenReturn(List.of(latest, previous));

        GlucoseReadingDto dto = service.getLatestReading(9L);

        assertEquals(TrendDirection.FORTY_FIVE_UP, dto.getTrendDirection());
        assertEquals(latestTime, dto.getReadingTime());
    }

    @Test
    void saveIfNewReadingUpdatesExistingReadingWhenTrendChanges() {
        GlucoseReadingMapper readingMapper = mock(GlucoseReadingMapper.class);
        MonitoredSubjectMapper subjectMapper = mock(MonitoredSubjectMapper.class);
        VendorConnectionMapper connectionMapper = mock(VendorConnectionMapper.class);
        VendorClientFactory vendorClientFactory = mock(VendorClientFactory.class);
        GlucoseConverter glucoseConverter = mock(GlucoseConverter.class);
        LatestSubjectDataCache latestSubjectDataCache = new LatestSubjectDataCache();
        GlucoseService service = new GlucoseService(
                readingMapper,
                subjectMapper,
                connectionMapper,
                vendorClientFactory,
                glucoseConverter,
                latestSubjectDataCache
        );

        Instant readingTime = Instant.parse("2026-03-22T12:00:00Z");
        GlucoseReading existing = GlucoseReading.builder()
                .id(1L)
                .monitoredSubjectId(9L)
                .glucoseMmol(6.8)
                .glucoseMgdl(122.4)
                .trendDirection(TrendDirection.FLAT)
                .readingTime(readingTime)
                .pushedToNightscout(true)
                .build();

        when(glucoseConverter.mmolToMgdl(6.8)).thenReturn(122.4);
        when(readingMapper.selectList(any())).thenReturn(List.of(existing));

        boolean isNew = service.saveIfNewReading(9L, VendorGlucoseData.builder()
                .glucoseMmol(6.8)
                .readingTime(readingTime)
                .trendDirection(TrendDirection.DOUBLE_DOWN)
                .build());

        assertFalse(isNew);
        verify(readingMapper, never()).insert(org.mockito.ArgumentMatchers.<GlucoseReading>any());
        verify(readingMapper).updateById(argThat((GlucoseReading reading) ->
                reading.getId().equals(1L)
                        && reading.getTrendDirection() == TrendDirection.DOUBLE_DOWN
                        && Boolean.FALSE.equals(reading.getPushedToNightscout())));
    }

    @Test
    void getLatestReadingUsesCachedSnapshotWhenAvailable() {
        GlucoseReadingMapper readingMapper = mock(GlucoseReadingMapper.class);
        MonitoredSubjectMapper subjectMapper = mock(MonitoredSubjectMapper.class);
        VendorConnectionMapper connectionMapper = mock(VendorConnectionMapper.class);
        VendorClientFactory vendorClientFactory = mock(VendorClientFactory.class);
        GlucoseConverter glucoseConverter = mock(GlucoseConverter.class);
        LatestSubjectDataCache latestSubjectDataCache = new LatestSubjectDataCache();
        latestSubjectDataCache.put(LatestSubjectData.builder()
                .subjectId(9L)
                .latestReading(GlucoseReadingDto.builder()
                        .id(2L)
                        .monitoredSubjectId(9L)
                        .glucoseMmol(6.9)
                        .readingTime(Instant.parse("2026-03-23T12:00:00Z"))
                        .trendDirection(TrendDirection.FORTY_FIVE_UP)
                        .build())
                .updatedAt(Instant.parse("2026-03-23T12:00:10Z"))
                .build());
        GlucoseService service = new GlucoseService(
                readingMapper,
                subjectMapper,
                connectionMapper,
                vendorClientFactory,
                glucoseConverter,
                latestSubjectDataCache
        );

        GlucoseReadingDto dto = service.getLatestReading(9L);

        assertEquals(6.9, dto.getGlucoseMmol());
        verify(readingMapper, never()).selectList(any());
    }

    @Test
    void saveIfNewReadingRefreshesLatestCache() {
        GlucoseReadingMapper readingMapper = mock(GlucoseReadingMapper.class);
        MonitoredSubjectMapper subjectMapper = mock(MonitoredSubjectMapper.class);
        VendorConnectionMapper connectionMapper = mock(VendorConnectionMapper.class);
        VendorClientFactory vendorClientFactory = mock(VendorClientFactory.class);
        GlucoseConverter glucoseConverter = mock(GlucoseConverter.class);
        LatestSubjectDataCache latestSubjectDataCache = new LatestSubjectDataCache();
        GlucoseService service = new GlucoseService(
                readingMapper,
                subjectMapper,
                connectionMapper,
                vendorClientFactory,
                glucoseConverter,
                latestSubjectDataCache
        );

        Instant latestTime = Instant.parse("2026-03-23T12:00:00Z");
        GlucoseReading storedLatest = GlucoseReading.builder()
                .id(10L)
                .monitoredSubjectId(9L)
                .glucoseMmol(6.6)
                .glucoseMgdl(118.8)
                .readingTime(latestTime)
                .pushedToNightscout(false)
                .build();

        when(glucoseConverter.mmolToMgdl(6.6)).thenReturn(118.8);
        when(readingMapper.selectList(any()))
                .thenReturn(List.of())
                .thenReturn(List.of(storedLatest));

        boolean isNew = service.saveIfNewReading(9L, VendorGlucoseData.builder()
                .glucoseMmol(6.6)
                .readingTime(latestTime)
                .trendDirection(TrendDirection.FLAT)
                .build());

        assertTrue(isNew);
        assertEquals(6.6, latestSubjectDataCache.get(9L).orElseThrow().getLatestReading().getGlucoseMmol());

        GlucoseReadingDto dto = service.getLatestReading(9L);
        assertEquals(6.6, dto.getGlucoseMmol());
        verify(readingMapper, times(2)).selectList(any());
    }
}

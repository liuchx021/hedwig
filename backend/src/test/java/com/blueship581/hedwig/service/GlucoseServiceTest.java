package com.blueship581.hedwig.service;

import com.blueship581.hedwig.domain.entity.GlucoseReading;
import com.blueship581.hedwig.domain.enums.TrendDirection;
import com.blueship581.hedwig.domain.mapper.GlucoseReadingMapper;
import com.blueship581.hedwig.domain.mapper.MonitoredSubjectMapper;
import com.blueship581.hedwig.domain.mapper.VendorConnectionMapper;
import com.blueship581.hedwig.util.GlucoseConverter;
import com.blueship581.hedwig.vendor.client.VendorClientFactory;
import com.blueship581.hedwig.vendor.model.VendorGlucoseData;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GlucoseServiceTest {

    @Test
    void saveIfNewReadingUpdatesExistingReadingWhenTrendChanges() {
        GlucoseReadingMapper readingMapper = mock(GlucoseReadingMapper.class);
        MonitoredSubjectMapper subjectMapper = mock(MonitoredSubjectMapper.class);
        VendorConnectionMapper connectionMapper = mock(VendorConnectionMapper.class);
        VendorClientFactory vendorClientFactory = mock(VendorClientFactory.class);
        GlucoseConverter glucoseConverter = mock(GlucoseConverter.class);
        GlucoseService service = new GlucoseService(
                readingMapper,
                subjectMapper,
                connectionMapper,
                vendorClientFactory,
                glucoseConverter
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
}

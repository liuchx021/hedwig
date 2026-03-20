package com.blueship581.hedwig.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.blueship581.hedwig.domain.entity.GlucoseReading;
import com.blueship581.hedwig.domain.entity.MonitoredSubject;
import com.blueship581.hedwig.domain.entity.VendorConnection;
import com.blueship581.hedwig.domain.mapper.GlucoseReadingMapper;
import com.blueship581.hedwig.domain.mapper.MonitoredSubjectMapper;
import com.blueship581.hedwig.domain.mapper.VendorConnectionMapper;
import com.blueship581.hedwig.dto.GlucoseReadingDto;
import com.blueship581.hedwig.dto.SyncResultDto;
import com.blueship581.hedwig.exception.ResourceNotFoundException;
import com.blueship581.hedwig.util.GlucoseConverter;
import com.blueship581.hedwig.vendor.client.VendorClient;
import com.blueship581.hedwig.vendor.client.VendorClientFactory;
import com.blueship581.hedwig.vendor.model.VendorGlucoseData;
import com.blueship581.hedwig.vendor.model.VendorSubject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GlucoseService {

    private final GlucoseReadingMapper readingMapper;
    private final MonitoredSubjectMapper subjectMapper;
    private final VendorConnectionMapper connectionMapper;
    private final VendorClientFactory vendorClientFactory;
    private final GlucoseConverter glucoseConverter;

    @Transactional
    public GlucoseReadingDto fetchLatest(Long connectionId, Long subjectId) {
        VendorConnection connection = Optional.ofNullable(connectionMapper.selectById(connectionId))
                .orElseThrow(() -> new ResourceNotFoundException("设备连接不存在：" + connectionId));
        MonitoredSubject subject = Optional.ofNullable(subjectMapper.selectById(subjectId))
                .orElseThrow(() -> new ResourceNotFoundException("监测对象不存在：" + subjectId));

        VendorClient client = vendorClientFactory.getClient(connection.getVendorType());
        VendorSubject vendorSubject = toVendorSubject(subject);

        VendorGlucoseData latest = client.getLatestGlucose(
                connection.getAccessToken(), connection.getVendorUserId(), vendorSubject);

        saveIfNew(subject.getId(), latest);
        GlucoseReading reading = readingMapper.selectList(
                        Wrappers.lambdaQuery(GlucoseReading.class)
                                .eq(GlucoseReading::getMonitoredSubjectId, subject.getId())
                                .orderByDesc(GlucoseReading::getReadingTime)
                                .last("LIMIT 1"))
                .stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("同步后仍未获取到监测对象的血糖读数：" + subjectId));
        return toDto(reading);
    }

    @Transactional
    public List<GlucoseReadingDto> fetchHistory(Long connectionId, Long subjectId) {
        VendorConnection connection = Optional.ofNullable(connectionMapper.selectById(connectionId))
                .orElseThrow(() -> new ResourceNotFoundException("设备连接不存在：" + connectionId));
        MonitoredSubject subject = Optional.ofNullable(subjectMapper.selectById(subjectId))
                .orElseThrow(() -> new ResourceNotFoundException("监测对象不存在：" + subjectId));

        VendorClient client = vendorClientFactory.getClient(connection.getVendorType());
        VendorSubject vendorSubject = toVendorSubject(subject);

        List<VendorGlucoseData> history = client.getHistoricalGlucose(
                connection.getAccessToken(), connection.getVendorUserId(), vendorSubject);

        int saved = 0;
        for (VendorGlucoseData data : history) {
            saveIfNew(subject.getId(), data);
            saved++;
        }
        log.debug("Fetched {} historical readings for subject {}, deduped into DB", saved, subjectId);

        return getReadings(subjectId);
    }

    public List<GlucoseReadingDto> getReadings(Long subjectId) {
        List<GlucoseReading> readings = readingMapper.selectList(
                Wrappers.lambdaQuery(GlucoseReading.class)
                        .eq(GlucoseReading::getMonitoredSubjectId, subjectId)
                        .orderByDesc(GlucoseReading::getReadingTime));

        if (readings.isEmpty()) {
            readings = fetchHistoryEntitiesForSubject(subjectId);
        }

        return readings
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public GlucoseReadingDto getLatestReading(Long subjectId) {
        return readingMapper.selectList(
                        Wrappers.lambdaQuery(GlucoseReading.class)
                                .eq(GlucoseReading::getMonitoredSubjectId, subjectId)
                                .orderByDesc(GlucoseReading::getReadingTime)
                                .last("LIMIT 1"))
                .stream().findFirst()
                .or(() -> {
                    fetchHistoryEntitiesForSubject(subjectId);
                    return readingMapper.selectList(
                                    Wrappers.lambdaQuery(GlucoseReading.class)
                                            .eq(GlucoseReading::getMonitoredSubjectId, subjectId)
                                            .orderByDesc(GlucoseReading::getReadingTime)
                                            .last("LIMIT 1"))
                            .stream().findFirst();
                })
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("未找到监测对象的血糖读数：" + subjectId));
    }

    /**
     * Save a glucose reading obtained externally (e.g. from the Ottai relatives endpoint)
     * if it doesn't already exist. Returns true when new data was persisted.
     */
    @Transactional
    public boolean saveIfNewReading(Long subjectId, VendorGlucoseData data) {
        return saveIfNew(subjectId, data);
    }

    @Transactional
    public SyncResultDto syncHistory(Long subjectId) {
        MonitoredSubject subject = Optional.ofNullable(subjectMapper.selectById(subjectId))
                .orElseThrow(() -> new ResourceNotFoundException("监测对象不存在：" + subjectId));
        VendorConnection connection = Optional.ofNullable(connectionMapper.selectById(subject.getVendorConnectionId()))
                .orElseThrow(() -> new ResourceNotFoundException("设备连接不存在：" + subject.getVendorConnectionId()));

        VendorClient client = vendorClientFactory.getClient(connection.getVendorType());
        VendorSubject vendorSubject = toVendorSubject(subject);

        List<VendorGlucoseData> history = client.getHistoricalGlucose(
                connection.getAccessToken(), connection.getVendorUserId(), vendorSubject);

        if (history.isEmpty()) {
            return SyncResultDto.builder().syncedCount(0).build();
        }

        Instant minTime = history.stream()
                .map(VendorGlucoseData::getReadingTime)
                .min(Instant::compareTo).orElseThrow();
        Instant maxTime = history.stream()
                .map(VendorGlucoseData::getReadingTime)
                .max(Instant::compareTo).orElseThrow();

        readingMapper.delete(
                Wrappers.lambdaQuery(GlucoseReading.class)
                        .eq(GlucoseReading::getMonitoredSubjectId, subjectId)
                        .between(GlucoseReading::getReadingTime, minTime, maxTime));

        for (VendorGlucoseData data : history) {
            saveIfNew(subjectId, data);
        }

        connection.setLastSyncedAt(Instant.now());
        connectionMapper.updateById(connection);

        log.info("Synced {} readings for subject {} (range {} ~ {})",
                history.size(), subjectId, minTime, maxTime);

        return SyncResultDto.builder()
                .syncedCount(history.size())
                .timeRangeStart(minTime)
                .timeRangeEnd(maxTime)
                .build();
    }

    /**
     * Poll the latest glucose from vendor and return whether new data was saved.
     * Used by the scheduler to decide polling frequency.
     */
    @Transactional
    public boolean pollLatest(Long connectionId, Long subjectId) {
        VendorConnection connection = Optional.ofNullable(connectionMapper.selectById(connectionId))
                .orElseThrow(() -> new ResourceNotFoundException("设备连接不存在：" + connectionId));
        MonitoredSubject subject = Optional.ofNullable(subjectMapper.selectById(subjectId))
                .orElseThrow(() -> new ResourceNotFoundException("监测对象不存在：" + subjectId));

        VendorClient client = vendorClientFactory.getClient(connection.getVendorType());
        VendorSubject vendorSubject = toVendorSubject(subject);

        VendorGlucoseData latest = client.getLatestGlucose(
                connection.getAccessToken(), connection.getVendorUserId(), vendorSubject);

        boolean isNew = saveIfNew(subject.getId(), latest);
        if (isNew) {
            connection.setLastSyncedAt(Instant.now());
            connectionMapper.updateById(connection);
        }
        return isNew;
    }

    private boolean saveIfNew(Long subjectId, VendorGlucoseData data) {
        boolean exists = readingMapper.exists(
                Wrappers.lambdaQuery(GlucoseReading.class)
                        .eq(GlucoseReading::getMonitoredSubjectId, subjectId)
                        .eq(GlucoseReading::getReadingTime, data.getReadingTime()));
        if (exists) {
            return false;
        }

        double mgdl = glucoseConverter.mmolToMgdl(data.getGlucoseMmol());
        GlucoseReading reading = GlucoseReading.builder()
                .monitoredSubjectId(subjectId)
                .glucoseMmol(data.getGlucoseMmol())
                .glucoseMgdl(mgdl)
                .trendDirection(data.getTrendDirection())
                .readingTime(data.getReadingTime())
                .pushedToNightscout(false)
                .build();
        readingMapper.insert(reading);
        return true;
    }

    private List<GlucoseReading> fetchHistoryEntitiesForSubject(Long subjectId) {
        MonitoredSubject subject = Optional.ofNullable(subjectMapper.selectById(subjectId))
                .orElseThrow(() -> new ResourceNotFoundException("监测对象不存在：" + subjectId));
        VendorConnection connection = Optional.ofNullable(connectionMapper.selectById(subject.getVendorConnectionId()))
                .orElseThrow(() -> new ResourceNotFoundException("设备连接不存在：" + subject.getVendorConnectionId()));

        VendorClient client = vendorClientFactory.getClient(connection.getVendorType());
        VendorSubject vendorSubject = toVendorSubject(subject);
        List<VendorGlucoseData> history = client.getHistoricalGlucose(
                connection.getAccessToken(), connection.getVendorUserId(), vendorSubject);

        for (VendorGlucoseData data : history) {
            saveIfNew(subjectId, data);
        }

        connection.setLastSyncedAt(Instant.now());
        connectionMapper.updateById(connection);

        return readingMapper.selectList(
                Wrappers.lambdaQuery(GlucoseReading.class)
                        .eq(GlucoseReading::getMonitoredSubjectId, subjectId)
                        .orderByDesc(GlucoseReading::getReadingTime));
    }

    private VendorSubject toVendorSubject(MonitoredSubject s) {
        return VendorSubject.builder()
                .subjectId(s.getVendorSubjectId())
                .deviceId(s.getVendorDeviceId())
                .displayName(s.getDisplayName())
                .build();
    }

    private GlucoseReadingDto toDto(GlucoseReading r) {
        return GlucoseReadingDto.builder()
                .id(r.getId())
                .monitoredSubjectId(r.getMonitoredSubjectId())
                .glucoseMmol(r.getGlucoseMmol())
                .glucoseMgdl(r.getGlucoseMgdl())
                .trendDirection(r.getTrendDirection())
                .readingTime(r.getReadingTime())
                .pushedToNightscout(r.getPushedToNightscout())
                .build();
    }
}

package com.blueship581.hedwig.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.blueship581.hedwig.domain.entity.GlucoseReading;
import com.blueship581.hedwig.domain.entity.NightscoutTarget;
import com.blueship581.hedwig.domain.enums.TrendDirection;
import com.blueship581.hedwig.domain.mapper.GlucoseReadingMapper;
import com.blueship581.hedwig.util.GlucoseTrendCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NightscoutSyncService {

    private final GlucoseReadingMapper readingMapper;
    private final NightscoutClient nightscoutClient;
    private final NightscoutTargetService targetService;

    /**
     * 同步待推送的血糖数据到所有 ACTIVE 的 Nightscout 目标。
     * 返回总推送条数。
     */
    @Transactional
    public int syncPendingReadings() {
        List<NightscoutTarget> activeTargets = targetService.getAllActiveTargets();
        if (activeTargets.isEmpty()) {
            return 0;
        }

        List<GlucoseReading> pending = readingMapper.selectList(
                Wrappers.lambdaQuery(GlucoseReading.class)
                        .eq(GlucoseReading::getPushedToNightscout, false));
        if (pending.isEmpty()) {
            return 0;
        }

        Map<Long, TrendDirection> derivedTrends = deriveNightscoutTrends(pending);
        List<NightscoutClient.SgvEntry> entries = pending.stream()
                .map(r -> nightscoutClient.toSgvEntry(
                        r.getGlucoseMmol(),
                        r.getReadingTime(),
                        trendToNightscout(derivedTrends.getOrDefault(r.getId(), TrendDirection.NONE)),
                        "hedwig"
                ))
                .collect(Collectors.toList());

        int totalPushed = 0;

        for (NightscoutTarget target : activeTargets) {
            // 如果目标绑定了特定监测对象，只推送该对象的数据
            List<NightscoutClient.SgvEntry> targetEntries;
            List<GlucoseReading> targetReadings;

            if (target.getMonitoredSubjectId() != null) {
                targetReadings = pending.stream()
                        .filter(r -> r.getMonitoredSubjectId().equals(target.getMonitoredSubjectId()))
                        .collect(Collectors.toList());
                targetEntries = targetReadings.stream()
                        .map(r -> nightscoutClient.toSgvEntry(
                                r.getGlucoseMmol(),
                                r.getReadingTime(),
                                trendToNightscout(derivedTrends.getOrDefault(r.getId(), TrendDirection.NONE)),
                                "hedwig"
                        ))
                        .collect(Collectors.toList());
            } else {
                targetReadings = pending;
                targetEntries = entries;
            }

            if (targetEntries.isEmpty()) {
                continue;
            }

            try {
                int pushed = nightscoutClient.pushEntries(target, targetEntries);
                if (pushed > 0) {
                    targetService.updatePushStatus(target.getId(), true, null);
                    totalPushed += pushed;
                }
            } catch (Exception e) {
                log.error("Failed to push to Nightscout target '{}': {}",
                        target.getName(), e.getMessage());
                targetService.updatePushStatus(target.getId(), false, e.getMessage());
            }
        }

        // 标记所有 pending 为已推送（只要至少有一个目标推送成功）
        if (totalPushed > 0) {
            pending.forEach(r -> {
                r.setPushedToNightscout(true);
                readingMapper.updateById(r);
            });
            log.info("Marked {} readings as pushed to Nightscout ({} targets)",
                    pending.size(), activeTargets.size());
        }

        return totalPushed;
    }

    private Map<Long, TrendDirection> deriveNightscoutTrends(List<GlucoseReading> pending) {
        Set<Long> subjectIds = pending.stream()
                .map(GlucoseReading::getMonitoredSubjectId)
                .collect(Collectors.toSet());
        if (subjectIds.isEmpty()) {
            return Map.of();
        }

        List<GlucoseReading> subjectReadings = readingMapper.selectList(
                Wrappers.lambdaQuery(GlucoseReading.class)
                        .in(GlucoseReading::getMonitoredSubjectId, subjectIds)
                        .orderByDesc(GlucoseReading::getReadingTime));

        Map<Long, TrendDirection> trendByReadingId = new HashMap<>();
        Map<Long, List<GlucoseReading>> readingsBySubject = subjectReadings.stream()
                .collect(Collectors.groupingBy(GlucoseReading::getMonitoredSubjectId));

        readingsBySubject.values().forEach(readings -> {
            for (int index = 0; index < readings.size(); index++) {
                GlucoseReading current = readings.get(index);
                GlucoseReading previous = index + 1 < readings.size() ? readings.get(index + 1) : null;
                if (current.getId() == null) {
                    continue;
                }
                trendByReadingId.put(
                        current.getId(),
                        GlucoseTrendCalculator.calculate(
                                current.getGlucoseMmol(),
                                previous == null ? null : previous.getGlucoseMmol()
                        )
                );
            }
        });

        return trendByReadingId;
    }

    private String trendToNightscout(TrendDirection trendDirection) {
        return switch (trendDirection) {
            case DOUBLE_UP -> "DoubleUp";
            case SINGLE_UP -> "SingleUp";
            case FORTY_FIVE_UP -> "FortyFiveUp";
            case FLAT -> "Flat";
            case FORTY_FIVE_DOWN -> "FortyFiveDown";
            case SINGLE_DOWN -> "SingleDown";
            case DOUBLE_DOWN -> "DoubleDown";
            default -> "NONE";
        };
    }
}

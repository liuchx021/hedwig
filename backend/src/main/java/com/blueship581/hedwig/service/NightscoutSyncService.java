package com.blueship581.hedwig.service;

import com.blueship581.hedwig.domain.entity.GlucoseReading;
import com.blueship581.hedwig.domain.repository.GlucoseReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NightscoutSyncService {

    private final GlucoseReadingRepository readingRepository;
    private final NightscoutClient nightscoutClient;

    @Transactional
    public int syncPendingReadings() {
        if (!nightscoutClient.isEnabled()) {
            return 0;
        }

        List<GlucoseReading> pending = readingRepository.findByPushedToNightscoutFalse();
        if (pending.isEmpty()) {
            return 0;
        }

        List<NightscoutClient.SgvEntry> entries = pending.stream()
                .map(r -> nightscoutClient.toSgvEntry(
                        r.getGlucoseMmol(),
                        r.getReadingTime(),
                        trendToNightscout(r.getTrendDirection().name()),
                        "hedwig"
                ))
                .collect(Collectors.toList());

        int pushed = nightscoutClient.pushEntries(entries);
        if (pushed > 0) {
            pending.forEach(r -> r.setPushedToNightscout(true));
            readingRepository.saveAll(pending);
            log.info("Marked {} readings as pushed to Nightscout", pushed);
        }
        return pushed;
    }

    private String trendToNightscout(String trendName) {
        return switch (trendName) {
            case "DOUBLE_UP" -> "DoubleUp";
            case "SINGLE_UP" -> "SingleUp";
            case "FORTY_FIVE_UP" -> "FortyFiveUp";
            case "FLAT" -> "Flat";
            case "FORTY_FIVE_DOWN" -> "FortyFiveDown";
            case "SINGLE_DOWN" -> "SingleDown";
            case "DOUBLE_DOWN" -> "DoubleDown";
            default -> "NONE";
        };
    }
}

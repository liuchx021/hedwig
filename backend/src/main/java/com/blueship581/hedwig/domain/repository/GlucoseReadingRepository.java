package com.blueship581.hedwig.domain.repository;

import com.blueship581.hedwig.domain.entity.GlucoseReading;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface GlucoseReadingRepository extends JpaRepository<GlucoseReading, Long> {

    List<GlucoseReading> findByMonitoredSubjectIdOrderByReadingTimeDesc(Long monitoredSubjectId);

    Optional<GlucoseReading> findTopByMonitoredSubjectIdOrderByReadingTimeDesc(Long monitoredSubjectId);

    boolean existsByMonitoredSubjectIdAndReadingTime(Long monitoredSubjectId, Instant readingTime);

    List<GlucoseReading> findByMonitoredSubjectIdAndReadingTimeAfterOrderByReadingTimeAsc(
            Long monitoredSubjectId, Instant after);

    List<GlucoseReading> findByPushedToNightscoutFalse();

    void deleteByMonitoredSubjectIdAndReadingTimeBetween(
            Long monitoredSubjectId, Instant start, Instant end);
}

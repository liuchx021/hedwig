package com.blueship581.hedwig.service;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LatestSubjectDataCache {

    private final ConcurrentHashMap<Long, LatestSubjectData> cache = new ConcurrentHashMap<>();

    public Optional<LatestSubjectData> get(Long subjectId) {
        return Optional.ofNullable(cache.get(subjectId));
    }

    public void put(LatestSubjectData data) {
        cache.put(data.getSubjectId(), data);
    }

    public void evict(Long subjectId) {
        cache.remove(subjectId);
    }
}

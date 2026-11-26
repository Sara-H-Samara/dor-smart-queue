package com.sara.backend.services;

import java.util.Set;

public interface OpenCounterFinder {

    boolean hasOpenCounter(Long serviceId);

    int countOpenCounters(Long serviceId);

    Set<Long> findServiceIdsWithOpenCounter();
}

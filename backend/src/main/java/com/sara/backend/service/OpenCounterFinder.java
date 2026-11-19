package com.sara.backend.service;

import java.util.Set;

public interface OpenCounterFinder {

    boolean hasOpenCounter(Long serviceId);

    Set<Long> findServiceIdsWithOpenCounter();
}

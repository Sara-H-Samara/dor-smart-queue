package com.sara.backend.service.impl;

import com.sara.backend.repository.CounterRepository;
import com.sara.backend.service.OpenCounterFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class OpenCounterFinderImpl implements OpenCounterFinder {

    @Autowired
    private CounterRepository counterRepository;

    @Override
    public boolean hasOpenCounter(Long serviceId) {
        return findServiceIdsWithOpenCounter().contains(serviceId);
    }

    @Override
    public Set<Long> findServiceIdsWithOpenCounter() {
        return counterRepository.findIdsOfServicesWithOpenCounter();
    }
}

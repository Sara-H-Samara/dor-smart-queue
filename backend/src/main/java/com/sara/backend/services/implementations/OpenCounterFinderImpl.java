package com.sara.backend.services.implementations;

import com.sara.backend.repositories.CounterRepository;
import com.sara.backend.services.OpenCounterFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class OpenCounterFinderImpl implements OpenCounterFinder {

    @Autowired
    private CounterRepository counterRepository;

    @Override
    public boolean hasOpenCounter(Long serviceId) {
        return countOpenCounters(serviceId) > 0;
    }

    @Override
    public int countOpenCounters(Long serviceId) {
        return Math.toIntExact(counterRepository.countOpenCountersOfService(serviceId));
    }

    @Override
    public Set<Long> findServiceIdsWithOpenCounter() {
        return counterRepository.findIdsOfServicesWithOpenCounter();
    }
}

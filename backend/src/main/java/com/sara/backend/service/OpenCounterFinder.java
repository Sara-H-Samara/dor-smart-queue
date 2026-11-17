package com.sara.backend.service;

import com.sara.backend.repository.CounterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class OpenCounterFinder {

    @Autowired
    private CounterRepository counterRepository;

    public boolean hasOpenCounter(Long serviceId) {
        return findServiceIdsWithOpenCounter().contains(serviceId);
    }

    public Set<Long> findServiceIdsWithOpenCounter() {
        return counterRepository.findIdsOfServicesWithOpenCounter();
    }
}

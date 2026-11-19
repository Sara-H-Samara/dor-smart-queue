package com.sara.backend.dtos.responses;

import com.sara.backend.entities.Counter;

public record CounterResponse(Long id, String name, boolean active, Long serviceId, String serviceName) {

    public static CounterResponse from(Counter counter) {
        return new CounterResponse(
                counter.getId(),
                counter.getName(),
                counter.isActive(),
                counter.getServiceType().getId(),
                counter.getServiceType().getName());
    }
}

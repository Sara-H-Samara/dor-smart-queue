package com.sara.backend.dtos.responses;

import com.sara.backend.entities.Counter;

public record AvailableCounterResponse(Long id, String name, Long serviceId, String serviceName) {

    public static AvailableCounterResponse from(Counter counter) {
        return new AvailableCounterResponse(
                counter.getId(),
                counter.getName(),
                counter.getServiceType().getId(),
                counter.getServiceType().getName());
    }
}

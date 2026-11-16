package com.sara.backend.dto.response;

import com.sara.backend.entity.Counter;

public record AvailableCounterResponse(Long id, String name, Long serviceId, String serviceName) {

    public static AvailableCounterResponse from(Counter counter) {
        return new AvailableCounterResponse(
                counter.getId(),
                counter.getName(),
                counter.getServiceType().getId(),
                counter.getServiceType().getName());
    }
}

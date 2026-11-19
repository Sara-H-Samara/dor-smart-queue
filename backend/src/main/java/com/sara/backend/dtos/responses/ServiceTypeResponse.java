package com.sara.backend.dtos.responses;

import com.sara.backend.entities.ServiceType;

public record ServiceTypeResponse(Long id, String name, String code, boolean active) {

    public static ServiceTypeResponse from(ServiceType serviceType) {
        return new ServiceTypeResponse(serviceType.getId(), serviceType.getName(), serviceType.getCode(), serviceType.isActive());
    }
}

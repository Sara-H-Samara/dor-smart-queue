package com.sara.backend.dto.response;

import com.sara.backend.entity.ServiceType;

public record ServiceTypeResponse(Long id, String name, boolean active) {

    public static ServiceTypeResponse from(ServiceType serviceType) {
        return new ServiceTypeResponse(serviceType.getId(), serviceType.getName(), serviceType.isActive());
    }
}

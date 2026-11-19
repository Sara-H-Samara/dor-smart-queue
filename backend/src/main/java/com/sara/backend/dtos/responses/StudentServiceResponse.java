package com.sara.backend.dtos.responses;

import com.sara.backend.entities.ServiceType;

public record StudentServiceResponse(Long id, String name, String code, boolean active, boolean hasOpenCounter) {

    public static StudentServiceResponse from(ServiceType serviceType, boolean hasOpenCounter) {
        return new StudentServiceResponse(
                serviceType.getId(), serviceType.getName(), serviceType.getCode(), serviceType.isActive(), hasOpenCounter);
    }
}

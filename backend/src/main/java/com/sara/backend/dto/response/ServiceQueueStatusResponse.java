package com.sara.backend.dto.response;

import com.sara.backend.entity.ServiceType;

import java.util.List;

public record ServiceQueueStatusResponse(
        String serviceName,
        String serviceCode,
        long waitingCount,
        List<CalledTicketOnScreenResponse> calledTickets
) {

    public static ServiceQueueStatusResponse from(
            ServiceType serviceType, long waitingCount, List<CalledTicketOnScreenResponse> calledTickets) {
        return new ServiceQueueStatusResponse(
                serviceType.getName(), serviceType.getCode(), waitingCount, calledTickets);
    }
}

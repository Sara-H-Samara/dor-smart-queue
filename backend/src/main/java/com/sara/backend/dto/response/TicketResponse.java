package com.sara.backend.dto.response;

import com.sara.backend.entity.Ticket;
import com.sara.backend.entity.TicketStatus;

import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        String ticketCode,
        Long serviceId,
        String serviceName,
        TicketStatus status,
        LocalDateTime createdAt
) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getServiceType().getCode() + "-" + String.format("%03d", ticket.getNumber()),
                ticket.getServiceType().getId(),
                ticket.getServiceType().getName(),
                ticket.getStatus(),
                ticket.getCreatedAt());
    }
}

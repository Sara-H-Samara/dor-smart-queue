package com.sara.backend.dtos.responses;

import com.sara.backend.entities.Ticket;
import com.sara.backend.entities.TicketDistance;
import com.sara.backend.entities.TicketStatus;

import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        String ticketCode,
        Long serviceId,
        String serviceName,
        TicketStatus status,
        LocalDateTime createdAt,
        TicketDistance distance
) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                formatTicketCode(ticket),
                ticket.getServiceType().getId(),
                ticket.getServiceType().getName(),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                ticket.getDistance());
    }

    public static String formatTicketCode(Ticket ticket) {
        return formatTicketCode(ticket.getServiceType().getCode(), ticket.getNumber());
    }

    public static String formatTicketCode(String serviceCode, int number) {
        return serviceCode + "-" + String.format("%03d", number);
    }
}

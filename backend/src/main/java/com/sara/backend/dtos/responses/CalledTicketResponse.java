package com.sara.backend.dtos.responses;

import com.sara.backend.entities.Ticket;
import com.sara.backend.entities.TicketStatus;

import java.time.LocalDateTime;

public record CalledTicketResponse(
        Long id,
        String ticketCode,
        String studentName,
        TicketStatus status,
        LocalDateTime calledAt,
        String counterName
) {

    public static CalledTicketResponse from(Ticket ticket) {
        return new CalledTicketResponse(
                ticket.getId(),
                TicketResponse.formatTicketCode(ticket),
                ticket.getStudent().getFullName(),
                ticket.getStatus(),
                ticket.getCalledAt(),
                ticket.getCounter() == null ? null : ticket.getCounter().getName());
    }
}

package com.sara.backend.dtos.responses;

import com.sara.backend.entities.Ticket;

import java.time.LocalDateTime;

public record WaitingTicketResponse(String ticketCode, LocalDateTime createdAt) {

    public static WaitingTicketResponse from(Ticket ticket) {
        return new WaitingTicketResponse(TicketResponse.formatTicketCode(ticket), ticket.getCreatedAt());
    }
}

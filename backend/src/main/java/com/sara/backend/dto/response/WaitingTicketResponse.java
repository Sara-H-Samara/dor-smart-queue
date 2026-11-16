package com.sara.backend.dto.response;

import com.sara.backend.entity.Ticket;

import java.time.LocalDateTime;

public record WaitingTicketResponse(String ticketCode, LocalDateTime createdAt) {

    public static WaitingTicketResponse from(Ticket ticket) {
        return new WaitingTicketResponse(TicketResponse.formatTicketCode(ticket), ticket.getCreatedAt());
    }
}

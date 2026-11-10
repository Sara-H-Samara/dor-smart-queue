package com.sara.backend.dto.response;

import com.sara.backend.entity.Ticket;
import com.sara.backend.entity.TicketStatus;

public record MyTicketResponse(
        String ticketCode,
        String serviceName,
        TicketStatus status,
        Integer position,
        Integer peopleAhead,
        Integer estimatedWaitMinutes
) {

    /** Builds the response for a WAITING ticket. */
    public static MyTicketResponse waiting(Ticket ticket, int peopleAhead, int minutesPerPerson) {
        int position = peopleAhead + 1;
        return new MyTicketResponse(
                code(ticket),
                ticket.getServiceType().getName(),
                ticket.getStatus(),
                position,
                peopleAhead,
                position * minutesPerPerson);
    }

    /** Builds the response for a CALLED ticket: no queue information applies. */
    public static MyTicketResponse called(Ticket ticket) {
        return new MyTicketResponse(
                code(ticket),
                ticket.getServiceType().getName(),
                ticket.getStatus(),
                null,
                null,
                null);
    }

    private static String code(Ticket ticket) {
        return ticket.getServiceType().getCode() + "-" + String.format("%03d", ticket.getNumber());
    }
}

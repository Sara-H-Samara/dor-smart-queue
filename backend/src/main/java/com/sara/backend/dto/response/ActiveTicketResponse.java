package com.sara.backend.dto.response;

import com.sara.backend.entity.Ticket;
import com.sara.backend.entity.TicketStatus;

public record ActiveTicketResponse(
        String ticketCode,
        String serviceName,
        TicketStatus status,
        Integer position,
        Integer peopleAhead,
        Integer estimatedWaitMinutes
) {

    /** Builds the response for a WAITING ticket. */
    public static ActiveTicketResponse forWaitingTicket(Ticket ticket, int peopleAhead, int minutesPerPerson) {
        int position = peopleAhead + 1;
        return new ActiveTicketResponse(
                TicketResponse.formatTicketCode(ticket),
                ticket.getServiceType().getName(),
                ticket.getStatus(),
                position,
                peopleAhead,
                position * minutesPerPerson);
    }

    /** Builds the response for a CALLED ticket: no queue information applies. */
    public static ActiveTicketResponse forCalledTicket(Ticket ticket) {
        return new ActiveTicketResponse(
                TicketResponse.formatTicketCode(ticket),
                ticket.getServiceType().getName(),
                ticket.getStatus(),
                null,
                null,
                null);
    }
}

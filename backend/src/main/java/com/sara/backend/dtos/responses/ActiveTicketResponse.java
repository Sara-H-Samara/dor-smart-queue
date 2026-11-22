package com.sara.backend.dtos.responses;

import com.sara.backend.entities.Ticket;
import com.sara.backend.entities.TicketStatus;

public record ActiveTicketResponse(
        String ticketCode,
        String serviceName,
        TicketStatus status,
        Integer position,
        Integer peopleAhead,
        Integer estimatedWaitMinutes,
        String counterName,
        boolean hasOpenCounter
) {

    public static ActiveTicketResponse forWaitingTicket(
            Ticket ticket, int peopleAhead, int minutesPerPerson, boolean hasOpenCounter) {
        int position = peopleAhead + 1;
        return new ActiveTicketResponse(
                TicketResponse.formatTicketCode(ticket),
                ticket.getServiceType().getName(),
                ticket.getStatus(),
                position,
                peopleAhead,
                hasOpenCounter ? position * minutesPerPerson : null,
                null,
                hasOpenCounter);
    }

    public static ActiveTicketResponse forCalledTicket(Ticket ticket) {
        return new ActiveTicketResponse(
                TicketResponse.formatTicketCode(ticket),
                ticket.getServiceType().getName(),
                ticket.getStatus(),
                null,
                null,
                null,
                ticket.getCounter() == null ? null : ticket.getCounter().getName(),
                true);
    }
}

package com.sara.backend.dtos.responses;

import com.sara.backend.repositories.NextTicketRow;

public record NextTicketOnScreenResponse(String ticketCode, String serviceName) {

    public static NextTicketOnScreenResponse from(NextTicketRow row) {
        return new NextTicketOnScreenResponse(
                TicketResponse.formatTicketCode(row.serviceCode(), row.number()), row.serviceName());
    }
}

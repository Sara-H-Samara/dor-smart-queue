package com.sara.backend.dto.response;

import com.sara.backend.repository.NextTicketRow;

public record NextTicketOnScreenResponse(String ticketCode, String serviceName) {

    public static NextTicketOnScreenResponse from(NextTicketRow row) {
        return new NextTicketOnScreenResponse(
                TicketResponse.formatTicketCode(row.serviceCode(), row.number()), row.serviceName());
    }
}

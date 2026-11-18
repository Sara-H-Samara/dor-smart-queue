package com.sara.backend.dto.response;

import com.sara.backend.repository.CalledTicketRow;

public record CalledTicketOnScreenResponse(String ticketCode, String counterName) {

    public static CalledTicketOnScreenResponse from(CalledTicketRow row) {
        return new CalledTicketOnScreenResponse(
                TicketResponse.formatTicketCode(row.serviceCode(), row.number()), row.counterName());
    }
}

package com.sara.backend.dtos.responses;

import com.sara.backend.repositories.OpenCounterRow;

public record CounterOnScreenResponse(String counterName, String serviceName, String ticketCode) {

    public static CounterOnScreenResponse from(OpenCounterRow row, String ticketCode) {
        return new CounterOnScreenResponse(row.counterName(), row.serviceName(), ticketCode);
    }
}

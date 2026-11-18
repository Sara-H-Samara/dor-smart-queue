package com.sara.backend.dto.response;

import com.sara.backend.repository.OpenCounterRow;

public record CounterOnScreenResponse(String counterName, String serviceName, String ticketCode) {

    public static CounterOnScreenResponse from(OpenCounterRow row, String ticketCode) {
        return new CounterOnScreenResponse(row.counterName(), row.serviceName(), ticketCode);
    }
}

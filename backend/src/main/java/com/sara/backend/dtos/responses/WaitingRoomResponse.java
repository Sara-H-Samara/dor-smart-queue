package com.sara.backend.dtos.responses;

import java.util.List;

public record WaitingRoomResponse(
        List<CounterOnScreenResponse> counters,
        List<NextTicketOnScreenResponse> nextTickets,
        long moreWaitingCount
) {
}

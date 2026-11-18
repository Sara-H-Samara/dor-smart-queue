package com.sara.backend.dto.response;

import java.util.List;

public record WaitingRoomResponse(
        List<CounterOnScreenResponse> counters,
        List<NextTicketOnScreenResponse> nextTickets,
        long moreWaitingCount
) {
}

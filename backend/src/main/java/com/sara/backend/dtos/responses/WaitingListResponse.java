package com.sara.backend.dtos.responses;

import java.util.List;

public record WaitingListResponse(long waitingCount, List<WaitingTicketResponse> tickets) {
}

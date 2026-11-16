package com.sara.backend.dto.response;

import java.util.List;

public record WaitingListResponse(long waitingCount, List<WaitingTicketResponse> tickets) {
}

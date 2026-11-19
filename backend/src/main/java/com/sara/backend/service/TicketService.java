package com.sara.backend.service;

import com.sara.backend.dto.request.TakeTicketRequest;
import com.sara.backend.dto.response.ActiveTicketResponse;
import com.sara.backend.dto.response.TicketResponse;

public interface TicketService {

    TicketResponse takeTicket(String email, TakeTicketRequest request);

    ActiveTicketResponse getActiveTicket(String email);

    void cancelActiveTicket(String email);
}

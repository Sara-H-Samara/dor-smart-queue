package com.sara.backend.services;

import com.sara.backend.dtos.requests.TakeTicketRequest;
import com.sara.backend.dtos.responses.ActiveTicketResponse;
import com.sara.backend.dtos.responses.TicketResponse;

public interface TicketService {

    TicketResponse takeTicket(String email, TakeTicketRequest request);

    ActiveTicketResponse getActiveTicket(String email);

    void cancelActiveTicket(String email);
}

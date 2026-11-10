package com.sara.backend.controller;

import com.sara.backend.dto.request.TakeTicketRequest;
import com.sara.backend.dto.response.TicketResponse;
import com.sara.backend.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse take(Authentication authentication, @Valid @RequestBody TakeTicketRequest request) {
        return ticketService.takeTicket(authentication.getName(), request);
    }
}

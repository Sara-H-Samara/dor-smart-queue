package com.sara.backend.controller;

import com.sara.backend.dto.response.CalledTicketResponse;
import com.sara.backend.dto.response.WaitingListResponse;
import com.sara.backend.service.StaffDeskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class StaffDeskController {

    @Autowired
    private StaffDeskService staffDeskService;

    @PostMapping("/counters/{counterId}/call-next")
    public ResponseEntity<CalledTicketResponse> callNext(@PathVariable Long counterId) {
        return staffDeskService.callNextStudent(counterId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/counters/{counterId}/current")
    public ResponseEntity<CalledTicketResponse> currentStudent(@PathVariable Long counterId) {
        return staffDeskService.findCurrentStudent(counterId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/counters/{counterId}/waiting")
    public WaitingListResponse waitingList(@PathVariable Long counterId) {
        return staffDeskService.listWaitingTickets(counterId);
    }

    @PostMapping("/tickets/{ticketId}/done")
    public CalledTicketResponse markDone(@PathVariable Long ticketId) {
        return staffDeskService.markTicketDone(ticketId);
    }

    @PostMapping("/tickets/{ticketId}/no-show")
    public CalledTicketResponse markNoShow(@PathVariable Long ticketId) {
        return staffDeskService.markTicketNoShow(ticketId);
    }
}

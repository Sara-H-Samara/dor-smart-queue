package com.sara.backend.services;

import com.sara.backend.dtos.responses.CalledTicketResponse;
import com.sara.backend.dtos.responses.WaitingListResponse;

import java.util.Optional;

public interface StaffDeskService {

    Optional<CalledTicketResponse> callNextStudent(Long counterId);

    Optional<CalledTicketResponse> findCurrentStudent(Long counterId);

    WaitingListResponse listWaitingTickets(Long counterId);

    CalledTicketResponse markTicketDone(Long ticketId);

    CalledTicketResponse markTicketNoShow(Long ticketId);
}

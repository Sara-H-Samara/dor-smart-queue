package com.sara.backend.service;

import com.sara.backend.dto.response.CalledTicketResponse;
import com.sara.backend.dto.response.WaitingListResponse;

import java.util.Optional;

public interface StaffDeskService {

    Optional<CalledTicketResponse> callNextStudent(Long counterId);

    Optional<CalledTicketResponse> findCurrentStudent(Long counterId);

    WaitingListResponse listWaitingTickets(Long counterId);

    CalledTicketResponse markTicketDone(Long ticketId);

    CalledTicketResponse markTicketNoShow(Long ticketId);
}

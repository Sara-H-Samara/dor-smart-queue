package com.sara.backend.services.implementations;

import com.sara.backend.dtos.responses.CalledTicketResponse;
import com.sara.backend.dtos.responses.WaitingListResponse;
import com.sara.backend.dtos.responses.WaitingTicketResponse;
import com.sara.backend.entities.Counter;
import com.sara.backend.entities.Ticket;
import com.sara.backend.entities.TicketStatus;
import com.sara.backend.repositories.CounterRepository;
import com.sara.backend.repositories.TicketRepository;
import com.sara.backend.services.StaffDeskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class StaffDeskServiceImpl implements StaffDeskService {

    private static final int WAITING_LIST_LIMIT = 20;

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Override
    @Transactional
    public Optional<CalledTicketResponse> callNextStudent(Long counterId) {
        Counter counter = counterRepository.findByIdWithWriteLock(counterId)
                .orElseThrow(this::counterNotFound);
        rejectUnavailableCounter(counter);

        if (ticketRepository.existsByCounterIdAndStatus(counterId, TicketStatus.CALLED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This counter is already serving a student.");
        }

        List<Ticket> oldestWaitingTickets = ticketRepository.findOldestWaitingTicketSkippingLocked(
                counter.getServiceType().getId(), LocalDate.now(), PageRequest.of(0, 1));
        if (oldestWaitingTickets.isEmpty()) {
            return Optional.empty();
        }

        Ticket ticket = oldestWaitingTickets.get(0);
        ticket.setStatus(TicketStatus.CALLED);
        ticket.setCounter(counter);
        ticket.setCalledAt(LocalDateTime.now());
        return Optional.of(CalledTicketResponse.from(ticketRepository.save(ticket)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CalledTicketResponse> findCurrentStudent(Long counterId) {
        if (!counterRepository.existsById(counterId)) {
            throw counterNotFound();
        }
        return ticketRepository.findFirstByCounterIdAndStatusOrderByCalledAtDesc(counterId, TicketStatus.CALLED)
                .map(CalledTicketResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public WaitingListResponse listWaitingTickets(Long counterId) {
        Counter counter = counterRepository.findById(counterId).orElseThrow(this::counterNotFound);
        Long serviceId = counter.getServiceType().getId();
        LocalDate today = LocalDate.now();

        long waitingCount = ticketRepository.countByServiceTypeIdAndTicketDateAndStatus(
                serviceId, today, TicketStatus.WAITING);
        List<WaitingTicketResponse> firstWaitingTickets = ticketRepository
                .findByServiceTypeIdAndTicketDateAndStatusOrderByNumberAsc(
                        serviceId, today, TicketStatus.WAITING, PageRequest.of(0, WAITING_LIST_LIMIT))
                .stream()
                .map(WaitingTicketResponse::from)
                .toList();
        return new WaitingListResponse(waitingCount, firstWaitingTickets);
    }

    @Override
    @Transactional
    public CalledTicketResponse markTicketDone(Long ticketId) {
        return changeStatusOfCalledTicket(ticketId, TicketStatus.DONE, "Only a called ticket can be marked done.");
    }

    @Override
    @Transactional
    public CalledTicketResponse markTicketNoShow(Long ticketId) {
        return changeStatusOfCalledTicket(
                ticketId, TicketStatus.NO_SHOW, "Only a called ticket can be marked as no-show.");
    }

    private CalledTicketResponse changeStatusOfCalledTicket(
            Long ticketId, TicketStatus newStatus, String rejectionMessage) {
        Ticket ticket = ticketRepository.findByIdWithWriteLock(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found."));
        if (!ticket.getStatus().canChangeTo(newStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, rejectionMessage);
        }
        ticket.setStatus(newStatus);
        return CalledTicketResponse.from(ticketRepository.save(ticket));
    }

    private ResponseStatusException counterNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Counter not found.");
    }

    private void rejectUnavailableCounter(Counter counter) {
        if (!counter.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This counter is not active.");
        }
        if (!counter.getServiceType().isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The service of this counter is not active.");
        }
    }
}

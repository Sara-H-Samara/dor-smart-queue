package com.sara.backend.service;

import com.sara.backend.dto.request.TakeTicketRequest;
import com.sara.backend.dto.response.ActiveTicketResponse;
import com.sara.backend.dto.response.TicketResponse;
import com.sara.backend.entity.ServiceType;
import com.sara.backend.entity.Ticket;
import com.sara.backend.entity.TicketStatus;
import com.sara.backend.entity.User;
import com.sara.backend.repository.ServiceTypeRepository;
import com.sara.backend.repository.TicketRepository;
import com.sara.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class TicketService {

    // Release 1 uses a fixed estimate; Release 2 will use real service times.
    private static final int ESTIMATED_MINUTES_PER_PERSON = 5;

    // WAITING and CALLED = the ticket is still active.
    private static final List<TicketStatus> ACTIVE_STATUSES = List.of(TicketStatus.WAITING, TicketStatus.CALLED);

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceTypeRepository serviceTypeRepository;

    @Transactional
    public TicketResponse takeTicket(String email, TakeTicketRequest request) {
        Long serviceId = request.serviceId();

        // Lock order is always: student first, then service (avoids deadlocks).
        Long studentId = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."))
                .getId();
        User student = userRepository.findByIdWithWriteLock(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));

        if (ticketRepository.existsByStudentIdAndStatusIn(student.getId(), ACTIVE_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have an active ticket.");
        }

        ServiceType serviceType = serviceTypeRepository.findByIdWithWriteLock(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found."));
        if (!serviceType.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This service is not available.");
        }

        LocalDate today = LocalDate.now();
        Integer lastNumberToday = ticketRepository.findHighestNumberForDay(serviceId, today);

        Ticket ticket = new Ticket();
        ticket.setStudent(student);
        ticket.setServiceType(serviceType);
        ticket.setNumber((lastNumberToday == null ? 0 : lastNumberToday) + 1);
        ticket.setTicketDate(today);
        ticket.setStatus(TicketStatus.WAITING);

        try {
            return TicketResponse.from(ticketRepository.saveAndFlush(ticket));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Please try again.");
        }
    }

    @Transactional(readOnly = true)
    public ActiveTicketResponse getActiveTicket(String email) {
        Ticket ticket = findActiveTicketOrThrow(email);

        if (ticket.getStatus() == TicketStatus.CALLED) {
            return ActiveTicketResponse.forCalledTicket(ticket);
        }

        int peopleAhead = (int) ticketRepository.countWaitingAhead(
                ticket.getServiceType().getId(), ticket.getTicketDate(), ticket.getNumber());
        return ActiveTicketResponse.forWaitingTicket(ticket, peopleAhead, ESTIMATED_MINUTES_PER_PERSON);
    }

    @Transactional
    public void cancelActiveTicket(String email) {
        Ticket ticket = findActiveTicketOrThrow(email);

        if (ticket.getStatus() == TicketStatus.CALLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A called ticket cannot be cancelled.");
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        ticketRepository.save(ticket);
    }

    private Ticket findActiveTicketOrThrow(String email) {
        Long studentId = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."))
                .getId();

        return ticketRepository.findFirstByStudentIdAndStatusIn(studentId, ACTIVE_STATUSES)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "You do not have an active ticket."));
    }
}

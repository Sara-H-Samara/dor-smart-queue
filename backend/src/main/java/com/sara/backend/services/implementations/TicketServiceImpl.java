package com.sara.backend.services.implementations;

import com.sara.backend.dtos.requests.TakeTicketRequest;
import com.sara.backend.dtos.responses.ActiveTicketResponse;
import com.sara.backend.dtos.responses.TicketResponse;
import com.sara.backend.entities.ServiceType;
import com.sara.backend.entities.Ticket;
import com.sara.backend.entities.TicketDistance;
import com.sara.backend.entities.TicketStatus;
import com.sara.backend.entities.User;
import com.sara.backend.repositories.ServiceTypeRepository;
import com.sara.backend.repositories.TicketRepository;
import com.sara.backend.repositories.UserRepository;
import com.sara.backend.services.OpenCounterFinder;
import com.sara.backend.services.TicketService;
import com.sara.backend.services.TurnAlertPolicy;
import com.sara.backend.streams.QueueChangeAnnouncer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class TicketServiceImpl implements TicketService {

    private static final int ESTIMATED_MINUTES_PER_PERSON = 5;

    private static final TicketDistance DISTANCE_WHEN_NOT_CHOSEN = TicketDistance.NEAR;

    private static final List<TicketStatus> ACTIVE_STATUSES = List.of(TicketStatus.WAITING, TicketStatus.CALLED);

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceTypeRepository serviceTypeRepository;

    @Autowired
    private OpenCounterFinder openCounterFinder;

    @Autowired
    private TurnAlertPolicy turnAlertPolicy;

    @Autowired
    private QueueChangeAnnouncer queueChangeAnnouncer;

    @Override
    @Transactional
    public TicketResponse takeTicket(String email, TakeTicketRequest request) {
        Long serviceId = request.serviceId();

        Long studentId = findStudentIdOrThrow(email);
        User student = userRepository.findByIdWithWriteLock(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));

        LocalDate today = LocalDate.now();
        if (ticketRepository.existsByStudentIdAndTicketDateAndStatusIn(student.getId(), today, ACTIVE_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have an active ticket.");
        }

        ServiceType serviceType = serviceTypeRepository.findByIdWithWriteLock(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found."));
        if (!serviceType.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This service is not available.");
        }
        if (!openCounterFinder.hasOpenCounter(serviceId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "No counter is open for " + serviceType.getName() + " right now.");
        }

        Integer lastNumberToday = ticketRepository.findHighestNumberForDay(serviceId, today);

        Ticket ticket = new Ticket();
        ticket.setStudent(student);
        ticket.setServiceType(serviceType);
        ticket.setNumber((lastNumberToday == null ? 0 : lastNumberToday) + 1);
        ticket.setTicketDate(today);
        ticket.setStatus(TicketStatus.WAITING);
        ticket.setDistance(request.distance() == null ? DISTANCE_WHEN_NOT_CHOSEN : request.distance());

        try {
            Ticket savedTicket = ticketRepository.saveAndFlush(ticket);
            queueChangeAnnouncer.announceTicketChanged();
            return TicketResponse.from(savedTicket);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Please try again.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ActiveTicketResponse getActiveTicket(String email) {
        Ticket ticket = findActiveTicketOrThrow(email);

        if (ticket.getStatus() == TicketStatus.CALLED) {
            return ActiveTicketResponse.forCalledTicket(ticket);
        }

        Long serviceId = ticket.getServiceType().getId();
        int peopleAhead = (int) ticketRepository.countWaitingAhead(
                serviceId, ticket.getTicketDate(), ticket.getNumber());
        boolean hasOpenCounter = openCounterFinder.hasOpenCounter(serviceId);
        boolean turnIsNear = hasOpenCounter && turnAlertPolicy.isTurnNear(peopleAhead, ticket.getDistance());
        return ActiveTicketResponse.forWaitingTicket(
                ticket,
                peopleAhead,
                ESTIMATED_MINUTES_PER_PERSON,
                hasOpenCounter,
                turnIsNear,
                turnAlertPolicy.peopleAheadLimitFor(ticket.getDistance()));
    }

    @Override
    @Transactional
    public void cancelActiveTicket(String email) {
        Long studentId = findStudentIdOrThrow(email);
        userRepository.findByIdWithWriteLock(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
        Ticket ticket = ticketRepository.findActiveTicketOfStudentWithWriteLock(
                        studentId, LocalDate.now(), ACTIVE_STATUSES)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "You do not have an active ticket."));

        if (!ticket.getStatus().canChangeTo(TicketStatus.CANCELLED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A called ticket cannot be cancelled.");
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        ticketRepository.save(ticket);
        queueChangeAnnouncer.announceTicketChanged();
    }

    private Long findStudentIdOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."))
                .getId();
    }

    private Ticket findActiveTicketOrThrow(String email) {
        Long studentId = findStudentIdOrThrow(email);

        return ticketRepository.findFirstByStudentIdAndTicketDateAndStatusIn(
                        studentId, LocalDate.now(), ACTIVE_STATUSES)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "You do not have an active ticket."));
    }
}

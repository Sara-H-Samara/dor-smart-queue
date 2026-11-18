package com.sara.backend.service;

import com.sara.backend.dto.response.CounterOnScreenResponse;
import com.sara.backend.dto.response.NextTicketOnScreenResponse;
import com.sara.backend.dto.response.TicketResponse;
import com.sara.backend.dto.response.WaitingRoomResponse;
import com.sara.backend.repository.CalledTicketRow;
import com.sara.backend.repository.CounterRepository;
import com.sara.backend.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WaitingRoomService {

    private static final int NEXT_TICKETS_LIMIT = 3;

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public WaitingRoomResponse buildTodayWaitingRoom() {
        LocalDate today = LocalDate.now();
        Map<Long, String> newestCalledTicketCodeByCounterId = findNewestCalledTicketCodeByCounterId(today);

        List<CounterOnScreenResponse> counters = counterRepository.findOpenCounterRows().stream()
                .map(row -> CounterOnScreenResponse.from(row, newestCalledTicketCodeByCounterId.get(row.counterId())))
                .toList();
        List<NextTicketOnScreenResponse> nextTickets = ticketRepository
                .findOldestWaitingTicketRowsOfDay(today, PageRequest.of(0, NEXT_TICKETS_LIMIT))
                .stream()
                .map(NextTicketOnScreenResponse::from)
                .toList();
        long moreWaitingCount = Math.max(0, ticketRepository.countWaitingTicketsOfDay(today) - nextTickets.size());

        return new WaitingRoomResponse(counters, nextTickets, moreWaitingCount);
    }

    private Map<Long, String> findNewestCalledTicketCodeByCounterId(LocalDate day) {
        return ticketRepository.findCalledTicketRowsOfDay(day).stream()
                .collect(Collectors.toMap(
                        CalledTicketRow::counterId,
                        row -> TicketResponse.formatTicketCode(row.serviceCode(), row.number()),
                        (newestTicketCode, olderTicketCode) -> newestTicketCode));
    }
}

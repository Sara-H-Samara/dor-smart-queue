package com.sara.backend.service;

import com.sara.backend.dto.response.CalledTicketOnScreenResponse;
import com.sara.backend.dto.response.ServiceQueueStatusResponse;
import com.sara.backend.repository.CalledTicketRow;
import com.sara.backend.repository.ServiceTypeRepository;
import com.sara.backend.repository.TicketRepository;
import com.sara.backend.repository.WaitingCountRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ServiceQueueStatusService {

    @Autowired
    private ServiceTypeRepository serviceTypeRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public List<ServiceQueueStatusResponse> listTodayServiceQueueStatuses() {
        LocalDate today = LocalDate.now();
        Map<Long, Long> waitingCountByServiceId = findWaitingCountByServiceId(today);
        Map<Long, List<CalledTicketOnScreenResponse>> calledTicketsByServiceId =
                findCalledTicketsByServiceId(today);

        return serviceTypeRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(serviceType -> ServiceQueueStatusResponse.from(
                        serviceType,
                        waitingCountByServiceId.getOrDefault(serviceType.getId(), 0L),
                        calledTicketsByServiceId.getOrDefault(serviceType.getId(), List.of())))
                .toList();
    }

    private Map<Long, Long> findWaitingCountByServiceId(LocalDate day) {
        return ticketRepository.countWaitingTicketsPerService(day).stream()
                .collect(Collectors.toMap(WaitingCountRow::serviceId, WaitingCountRow::waitingCount));
    }

    private Map<Long, List<CalledTicketOnScreenResponse>> findCalledTicketsByServiceId(LocalDate day) {
        return ticketRepository.findCalledTicketRowsOfDay(day).stream()
                .collect(Collectors.groupingBy(
                        CalledTicketRow::serviceId,
                        Collectors.mapping(CalledTicketOnScreenResponse::from, Collectors.toList())));
    }
}

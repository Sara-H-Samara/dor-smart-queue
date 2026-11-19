package com.sara.backend.services.implementations;

import com.sara.backend.dtos.requests.CounterRequest;
import com.sara.backend.dtos.requests.SetActiveRequest;
import com.sara.backend.dtos.responses.AvailableCounterResponse;
import com.sara.backend.dtos.responses.CounterResponse;
import com.sara.backend.entities.Counter;
import com.sara.backend.entities.ServiceType;
import com.sara.backend.entities.TicketStatus;
import com.sara.backend.repositories.CounterRepository;
import com.sara.backend.repositories.ServiceTypeRepository;
import com.sara.backend.repositories.TicketRepository;
import com.sara.backend.services.CounterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CounterServiceImpl implements CounterService {

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private ServiceTypeRepository serviceTypeRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Override
    public List<CounterResponse> list() {
        return counterRepository.findAllByOrderByNameAsc().stream().map(CounterResponse::from).toList();
    }

    @Override
    public List<AvailableCounterResponse> listAvailableCounters() {
        return counterRepository.findByActiveTrueAndServiceTypeActiveTrueOrderByNameAsc().stream()
                .map(AvailableCounterResponse::from)
                .toList();
    }

    @Override
    public CounterResponse create(CounterRequest request) {
        String name = trimAndValidateName(request.name());
        if (counterRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Counter name already exists.");
        }
        ServiceType serviceType = findServiceOrThrow(request.serviceId());
        rejectInactiveService(serviceType);

        Counter counter = new Counter();
        counter.setName(name);
        counter.setServiceType(serviceType);
        return CounterResponse.from(counterRepository.save(counter));
    }

    @Override
    @Transactional
    public CounterResponse update(Long id, CounterRequest request) {
        ServiceType serviceType = serviceTypeRepository.findByIdWithWriteLock(request.serviceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found."));
        Counter counter = findWithWriteLockOrThrow(id);
        String name = trimAndValidateName(request.name());
        if (counterRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Counter name already exists.");
        }
        if (!serviceType.getId().equals(counter.getServiceType().getId())) {
            rejectInactiveService(serviceType);
            rejectWhileStudentIsCalled(counter);
        }

        counter.setName(name);
        counter.setServiceType(serviceType);
        return CounterResponse.from(counterRepository.save(counter));
    }

    @Override
    @Transactional
    public CounterResponse updateActive(Long id, SetActiveRequest request) {
        Counter counter = findWithWriteLockOrThrow(id);
        if (!request.active()) {
            rejectWhileStudentIsCalled(counter);
        }
        counter.setActive(request.active());
        return CounterResponse.from(counterRepository.save(counter));
    }

    private void rejectWhileStudentIsCalled(Counter counter) {
        if (ticketRepository.existsByCounterIdAndStatus(counter.getId(), TicketStatus.CALLED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Finish the called student first.");
        }
    }

    private String trimAndValidateName(String name) {
        String trimmedName = name.trim();
        if (trimmedName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must be 1 to 100 characters.");
        }
        return trimmedName;
    }

    private ServiceType findServiceOrThrow(Long serviceId) {
        return serviceTypeRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found."));
    }

    private void rejectInactiveService(ServiceType serviceType) {
        if (!serviceType.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This service is not available.");
        }
    }

    private Counter findWithWriteLockOrThrow(Long id) {
        return counterRepository.findByIdWithWriteLock(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Counter not found."));
    }
}

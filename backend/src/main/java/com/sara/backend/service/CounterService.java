package com.sara.backend.service;

import com.sara.backend.dto.request.CounterRequest;
import com.sara.backend.dto.request.SetActiveRequest;
import com.sara.backend.dto.response.AvailableCounterResponse;
import com.sara.backend.dto.response.CounterResponse;
import com.sara.backend.entity.Counter;
import com.sara.backend.entity.ServiceType;
import com.sara.backend.repository.CounterRepository;
import com.sara.backend.repository.ServiceTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CounterService {

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private ServiceTypeRepository serviceTypeRepository;

    public List<CounterResponse> list() {
        return counterRepository.findAllByOrderByNameAsc().stream().map(CounterResponse::from).toList();
    }

    public List<AvailableCounterResponse> listAvailableCounters() {
        return counterRepository.findByActiveTrueAndServiceTypeActiveTrueOrderByNameAsc().stream()
                .map(AvailableCounterResponse::from)
                .toList();
    }

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

    public CounterResponse update(Long id, CounterRequest request) {
        Counter counter = findOrThrow(id);
        String name = trimAndValidateName(request.name());
        if (counterRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Counter name already exists.");
        }
        ServiceType serviceType = findServiceOrThrow(request.serviceId());
        if (!serviceType.getId().equals(counter.getServiceType().getId())) {
            rejectInactiveService(serviceType);
        }

        counter.setName(name);
        counter.setServiceType(serviceType);
        return CounterResponse.from(counterRepository.save(counter));
    }

    public CounterResponse updateActive(Long id, SetActiveRequest request) {
        Counter counter = findOrThrow(id);
        counter.setActive(request.active());
        return CounterResponse.from(counterRepository.save(counter));
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

    private Counter findOrThrow(Long id) {
        return counterRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Counter not found."));
    }
}

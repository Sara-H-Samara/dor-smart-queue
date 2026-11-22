package com.sara.backend.services.implementations;

import com.sara.backend.dtos.requests.SetActiveRequest;
import com.sara.backend.dtos.requests.NameRequest;
import com.sara.backend.dtos.requests.ServiceTypeCreateRequest;
import com.sara.backend.dtos.responses.ServiceTypeResponse;
import com.sara.backend.dtos.responses.StudentServiceResponse;
import com.sara.backend.entities.ServiceType;
import com.sara.backend.entities.TicketStatus;
import com.sara.backend.repositories.CounterRepository;
import com.sara.backend.repositories.ServiceTypeRepository;
import com.sara.backend.repositories.TicketRepository;
import com.sara.backend.services.OpenCounterFinder;
import com.sara.backend.services.ServiceTypeService;
import com.sara.backend.streams.QueueChangeAnnouncer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class ServiceTypeServiceImpl implements ServiceTypeService {

    @Autowired
    private ServiceTypeRepository serviceTypeRepository;

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private OpenCounterFinder openCounterFinder;

    @Autowired
    private QueueChangeAnnouncer queueChangeAnnouncer;

    @Override
    public List<ServiceTypeResponse> list() {
        return serviceTypeRepository.findAllByOrderByNameAsc().stream().map(ServiceTypeResponse::from).toList();
    }

    @Override
    public List<StudentServiceResponse> listActive() {
        Set<Long> serviceIdsWithOpenCounter = openCounterFinder.findServiceIdsWithOpenCounter();
        return serviceTypeRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(serviceType -> StudentServiceResponse.from(
                        serviceType, serviceIdsWithOpenCounter.contains(serviceType.getId())))
                .toList();
    }

    @Override
    public ServiceTypeResponse create(ServiceTypeCreateRequest request) {
        String name = request.name().trim();
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must be 1 to 100 characters.");
        }
        if (serviceTypeRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Service name already exists.");
        }
        String code = request.code().trim().toUpperCase();
        if (serviceTypeRepository.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This code is already used by another service.");
        }

        ServiceType serviceType = new ServiceType();
        serviceType.setName(name);
        serviceType.setCode(code);
        try {
            ServiceType createdServiceType = serviceTypeRepository.save(serviceType);
            queueChangeAnnouncer.announceConfigurationChanged();
            return ServiceTypeResponse.from(createdServiceType);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Service name or code already exists.");
        }
    }

    @Override
    public ServiceTypeResponse rename(Long id, NameRequest request) {
        ServiceType serviceType = findOrThrow(id);
        String name = request.name().trim();
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must be 1 to 100 characters.");
        }
        if (serviceTypeRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Service name already exists.");
        }

        serviceType.setName(name);
        ServiceType renamedServiceType = serviceTypeRepository.save(serviceType);
        queueChangeAnnouncer.announceConfigurationChanged();
        return ServiceTypeResponse.from(renamedServiceType);
    }

    @Override
    @Transactional
    public ServiceTypeResponse updateActive(Long id, SetActiveRequest request) {
        ServiceType serviceType = serviceTypeRepository.findByIdWithWriteLock(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found."));
        if (!request.active()) {
            lockCountersOfService(id);
            if (ticketRepository.existsByCounterServiceTypeIdAndTicketDateAndStatus(
                    id, LocalDate.now(), TicketStatus.CALLED)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Finish the called student first.");
            }
        }
        serviceType.setActive(request.active());
        ServiceType updatedServiceType = serviceTypeRepository.save(serviceType);
        queueChangeAnnouncer.announceConfigurationChanged();
        return ServiceTypeResponse.from(updatedServiceType);
    }

    private void lockCountersOfService(Long serviceId) {
        counterRepository.findByServiceTypeIdWithWriteLock(serviceId);
    }

    private ServiceType findOrThrow(Long id) {
        return serviceTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found."));
    }
}

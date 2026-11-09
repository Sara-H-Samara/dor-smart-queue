package com.sara.backend.service;

import com.sara.backend.dto.request.ActiveRequest;
import com.sara.backend.dto.request.NameRequest;
import com.sara.backend.dto.response.ServiceTypeResponse;
import com.sara.backend.entity.ServiceType;
import com.sara.backend.repository.ServiceTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ServiceTypeService {

    @Autowired
    private ServiceTypeRepository repository;

    public List<ServiceTypeResponse> list() {
        return repository.findAllByOrderByNameAsc().stream().map(ServiceTypeResponse::from).toList();
    }

    public ServiceTypeResponse create(NameRequest request) {
        String name = request.name().trim();
        if (name.isEmpty() || name.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must be 1 to 100 characters.");
        }
        if (repository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Service name already exists.");
        }

        ServiceType entity = new ServiceType();
        entity.setName(name);
        return ServiceTypeResponse.from(repository.save(entity));
    }

    public ServiceTypeResponse rename(Long id, NameRequest request) {
        ServiceType entity = find(id);
        String name = request.name().trim();
        if (name.isEmpty() || name.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must be 1 to 100 characters.");
        }
        if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Service name already exists.");
        }

        entity.setName(name);
        return ServiceTypeResponse.from(repository.save(entity));
    }

    public ServiceTypeResponse setActive(Long id, ActiveRequest request) {
        ServiceType entity = find(id);
        entity.setActive(request.active());
        return ServiceTypeResponse.from(repository.save(entity));
    }

    private ServiceType find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found."));
    }
}

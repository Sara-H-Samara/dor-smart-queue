package com.sara.backend.service;

import com.sara.backend.dto.request.ActiveRequest;
import com.sara.backend.dto.request.NameRequest;
import com.sara.backend.dto.response.CounterResponse;
import com.sara.backend.entity.Counter;
import com.sara.backend.repository.CounterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CounterService {

    @Autowired
    private CounterRepository repository;

    public List<CounterResponse> list() {
        return repository.findAllByOrderByNameAsc().stream().map(CounterResponse::from).toList();
    }

    public CounterResponse create(NameRequest request) {
        String name = request.name().trim();
        if (name.isEmpty() || name.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must be 1 to 100 characters.");
        }
        if (repository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Counter name already exists.");
        }

        Counter entity = new Counter();
        entity.setName(name);
        return CounterResponse.from(repository.save(entity));
    }

    public CounterResponse rename(Long id, NameRequest request) {
        Counter entity = find(id);
        String name = request.name().trim();
        if (name.isEmpty() || name.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must be 1 to 100 characters.");
        }
        if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Counter name already exists.");
        }

        entity.setName(name);
        return CounterResponse.from(repository.save(entity));
    }

    public CounterResponse setActive(Long id, ActiveRequest request) {
        Counter entity = find(id);
        entity.setActive(request.active());
        return CounterResponse.from(repository.save(entity));
    }

    private Counter find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Counter not found."));
    }
}

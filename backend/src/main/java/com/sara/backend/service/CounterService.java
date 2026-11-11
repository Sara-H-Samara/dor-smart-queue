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
    private CounterRepository counterRepository;

    public List<CounterResponse> list() {
        return counterRepository.findAllByOrderByNameAsc().stream().map(CounterResponse::from).toList();
    }

    public CounterResponse create(NameRequest request) {
        String name = request.name().trim();
        if (name.isEmpty() || name.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must be 1 to 100 characters.");
        }
        if (counterRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Counter name already exists.");
        }

        Counter counter = new Counter();
        counter.setName(name);
        return CounterResponse.from(counterRepository.save(counter));
    }

    public CounterResponse rename(Long id, NameRequest request) {
        Counter counter = findOrThrow(id);
        String name = request.name().trim();
        if (name.isEmpty() || name.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must be 1 to 100 characters.");
        }
        if (counterRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Counter name already exists.");
        }

        counter.setName(name);
        return CounterResponse.from(counterRepository.save(counter));
    }

    public CounterResponse updateActive(Long id, ActiveRequest request) {
        Counter counter = findOrThrow(id);
        counter.setActive(request.active());
        return CounterResponse.from(counterRepository.save(counter));
    }

    private Counter findOrThrow(Long id) {
        return counterRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Counter not found."));
    }
}

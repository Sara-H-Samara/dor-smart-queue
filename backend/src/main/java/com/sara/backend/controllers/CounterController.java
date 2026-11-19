package com.sara.backend.controllers;

import com.sara.backend.dtos.requests.CounterRequest;
import com.sara.backend.dtos.requests.SetActiveRequest;
import com.sara.backend.dtos.responses.CounterResponse;
import com.sara.backend.services.CounterService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/counters")
public class CounterController {

    @Autowired
    private CounterService counterService;

    @GetMapping
    public List<CounterResponse> list() {
        return counterService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CounterResponse create(@Valid @RequestBody CounterRequest request) {
        return counterService.create(request);
    }

    @PutMapping("/{id}")
    public CounterResponse update(@PathVariable Long id, @Valid @RequestBody CounterRequest request) {
        return counterService.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public CounterResponse setActive(@PathVariable Long id, @Valid @RequestBody SetActiveRequest request) {
        return counterService.updateActive(id, request);
    }
}

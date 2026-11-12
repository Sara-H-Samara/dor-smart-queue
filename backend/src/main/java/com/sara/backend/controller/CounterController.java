package com.sara.backend.controller;

import com.sara.backend.dto.request.SetActiveRequest;
import com.sara.backend.dto.request.NameRequest;
import com.sara.backend.dto.response.CounterResponse;
import com.sara.backend.service.CounterService;
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
    public CounterResponse create(@Valid @RequestBody NameRequest request) {
        return counterService.create(request);
    }

    @PutMapping("/{id}")
    public CounterResponse rename(@PathVariable Long id, @Valid @RequestBody NameRequest request) {
        return counterService.rename(id, request);
    }

    @PatchMapping("/{id}/active")
    public CounterResponse setActive(@PathVariable Long id, @Valid @RequestBody SetActiveRequest request) {
        return counterService.updateActive(id, request);
    }
}

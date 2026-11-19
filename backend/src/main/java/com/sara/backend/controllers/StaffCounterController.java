package com.sara.backend.controllers;

import com.sara.backend.dtos.responses.AvailableCounterResponse;
import com.sara.backend.services.CounterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/counters")
public class StaffCounterController {

    @Autowired
    private CounterService counterService;

    @GetMapping
    public List<AvailableCounterResponse> listAvailableCounters() {
        return counterService.listAvailableCounters();
    }
}

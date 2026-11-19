package com.sara.backend.services;

import com.sara.backend.dtos.requests.CounterRequest;
import com.sara.backend.dtos.requests.SetActiveRequest;
import com.sara.backend.dtos.responses.AvailableCounterResponse;
import com.sara.backend.dtos.responses.CounterResponse;

import java.util.List;

public interface CounterService {

    List<CounterResponse> list();

    List<AvailableCounterResponse> listAvailableCounters();

    CounterResponse create(CounterRequest request);

    CounterResponse update(Long id, CounterRequest request);

    CounterResponse updateActive(Long id, SetActiveRequest request);
}

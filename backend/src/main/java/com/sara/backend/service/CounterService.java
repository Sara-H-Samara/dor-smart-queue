package com.sara.backend.service;

import com.sara.backend.dto.request.CounterRequest;
import com.sara.backend.dto.request.SetActiveRequest;
import com.sara.backend.dto.response.AvailableCounterResponse;
import com.sara.backend.dto.response.CounterResponse;

import java.util.List;

public interface CounterService {

    List<CounterResponse> list();

    List<AvailableCounterResponse> listAvailableCounters();

    CounterResponse create(CounterRequest request);

    CounterResponse update(Long id, CounterRequest request);

    CounterResponse updateActive(Long id, SetActiveRequest request);
}

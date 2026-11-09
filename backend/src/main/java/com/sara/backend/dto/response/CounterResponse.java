package com.sara.backend.dto.response;

import com.sara.backend.entity.Counter;

public record CounterResponse(Long id, String name, boolean active) {

    public static CounterResponse from(Counter counter) {
        return new CounterResponse(counter.getId(), counter.getName(), counter.isActive());
    }
}

package com.sara.backend.controller;

import com.sara.backend.dto.request.ActiveRequest;
import com.sara.backend.dto.request.NameRequest;
import com.sara.backend.dto.request.ServiceTypeCreateRequest;
import com.sara.backend.dto.response.ServiceTypeResponse;
import com.sara.backend.service.ServiceTypeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/services")
public class ServiceTypeController {

    @Autowired
    private ServiceTypeService serviceTypeService;

    @GetMapping
    public List<ServiceTypeResponse> list() {
        return serviceTypeService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceTypeResponse create(@Valid @RequestBody ServiceTypeCreateRequest request) {
        return serviceTypeService.create(request);
    }

    @PutMapping("/{id}")
    public ServiceTypeResponse rename(@PathVariable Long id, @Valid @RequestBody NameRequest request) {
        return serviceTypeService.rename(id, request);
    }

    @PatchMapping("/{id}/active")
    public ServiceTypeResponse setActive(@PathVariable Long id, @Valid @RequestBody ActiveRequest request) {
        return serviceTypeService.updateActive(id, request);
    }
}

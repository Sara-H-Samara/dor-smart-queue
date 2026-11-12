package com.sara.backend.controller;

import com.sara.backend.dto.response.ServiceTypeResponse;
import com.sara.backend.service.ServiceTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class StudentServiceController {

    @Autowired
    private ServiceTypeService serviceTypeService;

    @GetMapping
    public List<ServiceTypeResponse> listActiveServices() {
        return serviceTypeService.listActive();
    }
}

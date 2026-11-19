package com.sara.backend.controllers;

import com.sara.backend.dtos.responses.StudentServiceResponse;
import com.sara.backend.services.ServiceTypeService;
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
    public List<StudentServiceResponse> listActiveServices() {
        return serviceTypeService.listActive();
    }
}

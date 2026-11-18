package com.sara.backend.controller;

import com.sara.backend.dto.response.ServiceQueueStatusResponse;
import com.sara.backend.service.ServiceQueueStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/queue-status")
public class ServiceQueueStatusController {

    @Autowired
    private ServiceQueueStatusService serviceQueueStatusService;

    @GetMapping
    public List<ServiceQueueStatusResponse> listServiceQueueStatuses() {
        return serviceQueueStatusService.listTodayServiceQueueStatuses();
    }
}

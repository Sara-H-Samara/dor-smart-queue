package com.sara.backend.services;

import com.sara.backend.dtos.requests.NameRequest;
import com.sara.backend.dtos.requests.ServiceTypeCreateRequest;
import com.sara.backend.dtos.requests.SetActiveRequest;
import com.sara.backend.dtos.responses.ServiceTypeResponse;
import com.sara.backend.dtos.responses.StudentServiceResponse;

import java.util.List;

public interface ServiceTypeService {

    List<ServiceTypeResponse> list();

    List<StudentServiceResponse> listActive();

    ServiceTypeResponse create(ServiceTypeCreateRequest request);

    ServiceTypeResponse rename(Long id, NameRequest request);

    ServiceTypeResponse updateActive(Long id, SetActiveRequest request);
}

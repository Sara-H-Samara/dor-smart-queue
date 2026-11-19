package com.sara.backend.service;

import com.sara.backend.dto.request.NameRequest;
import com.sara.backend.dto.request.ServiceTypeCreateRequest;
import com.sara.backend.dto.request.SetActiveRequest;
import com.sara.backend.dto.response.ServiceTypeResponse;
import com.sara.backend.dto.response.StudentServiceResponse;

import java.util.List;

public interface ServiceTypeService {

    List<ServiceTypeResponse> list();

    List<StudentServiceResponse> listActive();

    ServiceTypeResponse create(ServiceTypeCreateRequest request);

    ServiceTypeResponse rename(Long id, NameRequest request);

    ServiceTypeResponse updateActive(Long id, SetActiveRequest request);
}

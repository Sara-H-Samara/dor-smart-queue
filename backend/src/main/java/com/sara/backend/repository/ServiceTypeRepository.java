package com.sara.backend.repository;

import com.sara.backend.entity.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServiceTypeRepository extends JpaRepository<ServiceType, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByCodeIgnoreCase(String code);

    List<ServiceType> findAllByOrderByNameAsc();

    List<ServiceType> findByActiveTrueOrderByNameAsc();
}

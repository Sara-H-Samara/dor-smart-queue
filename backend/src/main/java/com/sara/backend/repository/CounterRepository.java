package com.sara.backend.repository;

import com.sara.backend.entity.Counter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CounterRepository extends JpaRepository<Counter, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<Counter> findAllByOrderByNameAsc();

    List<Counter> findByActiveTrueOrderByNameAsc();

    List<Counter> findByActiveTrueAndServiceTypeActiveTrueOrderByNameAsc();

    @Query("select distinct c.serviceType.id from Counter c where c.active = true and c.serviceType.active = true")
    Set<Long> findIdsOfServicesWithOpenCounter();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Counter c where c.serviceType.id = :serviceId order by c.id asc")
    List<Counter> findByServiceTypeIdWithWriteLock(@Param("serviceId") Long serviceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Counter c where c.id = :id")
    Optional<Counter> findByIdWithWriteLock(@Param("id") Long id);
}

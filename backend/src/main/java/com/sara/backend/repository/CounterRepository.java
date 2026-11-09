package com.sara.backend.repository;

import com.sara.backend.entity.Counter;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CounterRepository extends JpaRepository<Counter, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<Counter> findAllByOrderByNameAsc();

    List<Counter> findByActiveTrueOrderByNameAsc();
}

package com.sara.backend.repositories;

import com.sara.backend.entities.Role;
import com.sara.backend.entities.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("select u.role from User u where u.email = :email")
    Optional<Role> findRoleByEmail(@Param("email") String email);

    @Query("select u from User u"
            + " where lower(u.fullName) like lower(concat('%', :search, '%')) escape '\\'"
            + " or lower(u.email) like lower(concat('%', :search, '%')) escape '\\'")
    Page<User> findByNameOrEmailContaining(@Param("search") String search, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdWithWriteLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.role = :role order by u.id")
    List<User> findByRoleWithWriteLock(@Param("role") Role role);
}

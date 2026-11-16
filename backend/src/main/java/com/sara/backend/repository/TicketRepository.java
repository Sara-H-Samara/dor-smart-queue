package com.sara.backend.repository;

import com.sara.backend.entity.Ticket;
import com.sara.backend.entity.TicketStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    boolean existsByStudentIdAndStatusIn(Long studentId, Collection<TicketStatus> statuses);

    Optional<Ticket> findFirstByStudentIdAndStatusIn(Long studentId, Collection<TicketStatus> statuses);

    boolean existsByCounterIdAndStatus(Long counterId, TicketStatus status);

    Optional<Ticket> findFirstByCounterIdAndStatusOrderByCalledAtDesc(Long counterId, TicketStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t where t.id = :id")
    Optional<Ticket> findByIdWithWriteLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t where t.student.id = :studentId and t.status in :statuses")
    Optional<Ticket> findActiveTicketOfStudentWithWriteLock(
            @Param("studentId") Long studentId, @Param("statuses") Collection<TicketStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select t from Ticket t where t.serviceType.id = :serviceId and t.ticketDate = :date"
            + " and t.status = com.sara.backend.entity.TicketStatus.WAITING order by t.number asc")
    List<Ticket> findOldestWaitingTicketSkippingLocked(
            @Param("serviceId") Long serviceId, @Param("date") LocalDate date, Pageable pageable);

    List<Ticket> findByServiceTypeIdAndTicketDateAndStatusOrderByNumberAsc(
            Long serviceId, LocalDate ticketDate, TicketStatus status, Pageable pageable);

    long countByServiceTypeIdAndTicketDateAndStatus(Long serviceId, LocalDate ticketDate, TicketStatus status);

    @Query("select max(t.number) from Ticket t where t.serviceType.id = :serviceId and t.ticketDate = :date")
    Integer findHighestNumberForDay(@Param("serviceId") Long serviceId, @Param("date") LocalDate date);

    @Query("select count(t) from Ticket t where t.serviceType.id = :serviceId and t.ticketDate = :date"
            + " and t.status = com.sara.backend.entity.TicketStatus.WAITING and t.number < :ticketNumber")
    long countWaitingAhead(
            @Param("serviceId") Long serviceId,
            @Param("date") LocalDate date,
            @Param("ticketNumber") int ticketNumber);
}

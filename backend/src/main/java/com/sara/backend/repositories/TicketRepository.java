package com.sara.backend.repositories;

import com.sara.backend.entities.Ticket;
import com.sara.backend.entities.TicketStatus;
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

    boolean existsByStudentIdAndTicketDateAndStatusIn(
            Long studentId, LocalDate ticketDate, Collection<TicketStatus> statuses);

    Optional<Ticket> findFirstByStudentIdAndTicketDateAndStatusIn(
            Long studentId, LocalDate ticketDate, Collection<TicketStatus> statuses);

    boolean existsByCounterIdAndTicketDateAndStatus(Long counterId, LocalDate ticketDate, TicketStatus status);

    boolean existsByCounterServiceTypeIdAndTicketDateAndStatus(
            Long serviceId, LocalDate ticketDate, TicketStatus status);

    Optional<Ticket> findFirstByCounterIdAndTicketDateAndStatusOrderByCalledAtDesc(
            Long counterId, LocalDate ticketDate, TicketStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t where t.id = :id")
    Optional<Ticket> findByIdWithWriteLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t where t.student.id = :studentId and t.ticketDate = :ticketDate"
            + " and t.status in :statuses")
    Optional<Ticket> findActiveTicketOfStudentWithWriteLock(
            @Param("studentId") Long studentId,
            @Param("ticketDate") LocalDate ticketDate,
            @Param("statuses") Collection<TicketStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select t from Ticket t where t.serviceType.id = :serviceId and t.ticketDate = :date"
            + " and t.status = com.sara.backend.entities.TicketStatus.WAITING order by t.number asc")
    List<Ticket> findOldestWaitingTicketSkippingLocked(
            @Param("serviceId") Long serviceId, @Param("date") LocalDate date, Pageable pageable);

    List<Ticket> findByServiceTypeIdAndTicketDateAndStatusOrderByNumberAsc(
            Long serviceId, LocalDate ticketDate, TicketStatus status, Pageable pageable);

    long countByServiceTypeIdAndTicketDateAndStatus(Long serviceId, LocalDate ticketDate, TicketStatus status);

    @Query("select new com.sara.backend.repositories.CalledTicketRow("
            + "t.counter.id, t.serviceType.code, t.number)"
            + " from Ticket t where t.ticketDate = :date"
            + " and t.status = com.sara.backend.entities.TicketStatus.CALLED and t.counter is not null"
            + " order by t.calledAt desc")
    List<CalledTicketRow> findCalledTicketRowsOfDay(@Param("date") LocalDate date);

    @Query("select new com.sara.backend.repositories.NextTicketRow("
            + "t.serviceType.code, t.number, t.serviceType.name)"
            + " from Ticket t where t.ticketDate = :date"
            + " and t.status = com.sara.backend.entities.TicketStatus.WAITING and t.serviceType.active = true"
            + " order by t.createdAt asc, t.id asc")
    List<NextTicketRow> findOldestWaitingTicketRowsOfDay(@Param("date") LocalDate date, Pageable pageable);

    @Query("select count(t) from Ticket t where t.ticketDate = :date"
            + " and t.status = com.sara.backend.entities.TicketStatus.WAITING and t.serviceType.active = true")
    long countWaitingTicketsOfDay(@Param("date") LocalDate date);

    @Query("select new com.sara.backend.repositories.FinishedTicketRow(t.calledAt, t.finishedAt)"
            + " from Ticket t where t.serviceType.id = :serviceId and t.ticketDate = :date"
            + " and t.status = com.sara.backend.entities.TicketStatus.DONE"
            + " and t.calledAt is not null and t.finishedAt is not null"
            + " order by t.finishedAt desc, t.id desc")
    List<FinishedTicketRow> findRecentFinishedTicketRowsOfService(
            @Param("serviceId") Long serviceId, @Param("date") LocalDate date, Pageable pageable);

    @Query("select max(t.number) from Ticket t where t.serviceType.id = :serviceId and t.ticketDate = :date")
    Integer findHighestNumberForDay(@Param("serviceId") Long serviceId, @Param("date") LocalDate date);

    @Query("select count(t) from Ticket t where t.serviceType.id = :serviceId and t.ticketDate = :date"
            + " and t.status = com.sara.backend.entities.TicketStatus.WAITING and t.number < :ticketNumber")
    long countWaitingAhead(
            @Param("serviceId") Long serviceId,
            @Param("date") LocalDate date,
            @Param("ticketNumber") int ticketNumber);
}

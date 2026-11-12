package com.sara.backend.repository;

import com.sara.backend.entity.Ticket;
import com.sara.backend.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    boolean existsByStudentIdAndStatusIn(Long studentId, Collection<TicketStatus> statuses);

    Optional<Ticket> findFirstByStudentIdAndStatusIn(Long studentId, Collection<TicketStatus> statuses);

    @Query("select max(t.number) from Ticket t where t.serviceType.id = :serviceId and t.ticketDate = :date")
    Integer findHighestNumberForDay(@Param("serviceId") Long serviceId, @Param("date") LocalDate date);

    @Query("select count(t) from Ticket t where t.serviceType.id = :serviceId and t.ticketDate = :date"
            + " and t.status = com.sara.backend.entity.TicketStatus.WAITING and t.number < :ticketNumber")
    long countWaitingAhead(
            @Param("serviceId") Long serviceId,
            @Param("date") LocalDate date,
            @Param("ticketNumber") int ticketNumber);
}

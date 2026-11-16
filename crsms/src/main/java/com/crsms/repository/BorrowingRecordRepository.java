package com.crsms.repository;

import com.crsms.model.borrowing.BorrowingRecord;
import com.crsms.model.enums.BorrowingStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BorrowingRecordRepository extends JpaRepository<BorrowingRecord, Long> {
    Optional<BorrowingRecord> findByReservationId(Long reservationId);

    List<BorrowingRecord> findByBorrowerIdOrderByPickedUpAtDesc(Long borrowerId);

    long countByStatusIn(Collection<BorrowingStatus> statuses);

    @Query("""
            SELECT c.name, COUNT(b.id)
            FROM BorrowingRecord b
            JOIN b.unit u
            JOIN u.listing l
            JOIN l.category c
            GROUP BY c.name
            ORDER BY COUNT(b.id) DESC
            """)
    List<Object[]> categoryCounts();

    @Query("""
            SELECT l.title, COUNT(b.id)
            FROM BorrowingRecord b
            JOIN b.unit u
            JOIN u.listing l
            GROUP BY l.title
            ORDER BY COUNT(b.id) DESC
            """)
    List<Object[]> popularResources();

    @Query("""
            SELECT YEAR(b.pickedUpAt), MONTH(b.pickedUpAt), COUNT(b.id)
            FROM BorrowingRecord b
            GROUP BY YEAR(b.pickedUpAt), MONTH(b.pickedUpAt)
            """)
    List<Object[]> monthlyCounts();

    @Query("""
            SELECT COUNT(b) FROM BorrowingRecord b
            WHERE b.status = com.crsms.model.enums.BorrowingStatus.OVERDUE
               OR (b.status = com.crsms.model.enums.BorrowingStatus.ACTIVE AND b.dueAt < :now)
            """)
    long countOverdue(@Param("now") Instant now);

    @Query("""
            SELECT b FROM BorrowingRecord b
            WHERE b.overdueNotified = false
              AND b.returnedAt IS NULL
              AND b.dueAt < :now
            """)
    List<BorrowingRecord> findNeedingOverdueNotice(@Param("now") Instant now);

    @Query("""
            SELECT b FROM BorrowingRecord b
            WHERE b.status = com.crsms.model.enums.BorrowingStatus.ACTIVE
              AND b.reminderSent = false
              AND b.dueAt > :now
              AND b.dueAt <= :horizon
            """)
    List<BorrowingRecord> findNeedingReminder(@Param("now") Instant now, @Param("horizon") Instant horizon);

    long countByBorrowerIdAndStatusIn(Long borrowerId, Collection<BorrowingStatus> statuses);

    long countByUnitIdAndStatusIn(Long unitId, Collection<BorrowingStatus> statuses);

    List<BorrowingRecord> findAllByOrderByPickedUpAtDesc();
}

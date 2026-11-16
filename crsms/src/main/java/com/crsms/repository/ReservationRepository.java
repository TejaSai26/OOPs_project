package com.crsms.repository;

import com.crsms.model.enums.ReservationStatus;
import com.crsms.model.reservation.Reservation;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByBorrowerIdOrderByCreatedAtDesc(Long borrowerId);

    @Query("""
            SELECT r FROM Reservation r
            WHERE r.unit.listing.owner.id = :ownerId
            ORDER BY r.createdAt DESC
            """)
    List<Reservation> findIncoming(@Param("ownerId") Long ownerId);

    long countByStatusIn(Collection<ReservationStatus> statuses);

    @Query("""
            SELECT COUNT(r) FROM Reservation r
            WHERE r.unit.id = :unitId
              AND r.status IN :statuses
              AND r.startAt < :endAt
              AND r.endAt > :startAt
            """)
    long countOverlapping(@Param("unitId") Long unitId,
                           @Param("startAt") Instant startAt,
                           @Param("endAt") Instant endAt,
                           @Param("statuses") Collection<ReservationStatus> statuses);

    long countByUnitIdAndStatus(Long unitId, ReservationStatus status);

    List<Reservation> findByUnitIdAndStatusOrderByCreatedAtAsc(Long unitId, ReservationStatus status);

    List<Reservation> findByStatusInAndEndAtBefore(Collection<ReservationStatus> statuses, Instant endAt);

    @Query("""
            SELECT COUNT(r) FROM Reservation r
            WHERE r.borrower.id = :borrowerId AND r.status IN :statuses
            """)
    long countOpenForBorrower(@Param("borrowerId") Long borrowerId, @Param("statuses") Collection<ReservationStatus> statuses);

    @Query("""
            SELECT COUNT(r) FROM Reservation r
            WHERE r.unit.listing.owner.id = :ownerId AND r.status = com.crsms.model.enums.ReservationStatus.REQUESTED
            """)
    long countIncomingRequests(@Param("ownerId") Long ownerId);

    @Query("""
            SELECT YEAR(r.createdAt), MONTH(r.createdAt), COUNT(r.id)
            FROM Reservation r
            GROUP BY YEAR(r.createdAt), MONTH(r.createdAt)
            """)
    List<Object[]> monthlyCounts();

    List<Reservation> findAllByOrderByCreatedAtDesc();
}

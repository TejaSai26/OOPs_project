package com.crsms.repository;

import com.crsms.model.enums.PenaltyStatus;
import com.crsms.model.penalty.Penalty;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PenaltyRepository extends JpaRepository<Penalty, Long> {
    Optional<Penalty> findByBorrowingId(Long borrowingId);

    List<Penalty> findByBorrowingIdIn(Collection<Long> borrowingIds);

    List<Penalty> findByBorrowerIdOrderByCreatedAtDesc(Long borrowerId);

    List<Penalty> findAllByOrderByCreatedAtDesc();

    @Query("SELECT SUM(p.amount) FROM Penalty p WHERE p.paymentStatus = :status")
    BigDecimal sumByStatus(@Param("status") PenaltyStatus status);

    @Query("SELECT SUM(p.amount) FROM Penalty p WHERE p.borrower.id = :borrowerId AND p.paymentStatus = :status")
    BigDecimal sumForBorrower(@Param("borrowerId") Long borrowerId, @Param("status") PenaltyStatus status);
}

package com.crsms.repository;

import com.crsms.model.enums.MaintenanceStatus;
import com.crsms.model.maintenance.MaintenanceRequest;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {
    List<MaintenanceRequest> findAllByOrderByCreatedAtDesc();

    List<MaintenanceRequest> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    long countByStatusIn(Collection<MaintenanceStatus> statuses);

    long countByStatus(MaintenanceStatus status);

    @Query("SELECT SUM(m.repairCost) FROM MaintenanceRequest m WHERE m.repairCost IS NOT NULL")
    BigDecimal sumRepairCost();
}

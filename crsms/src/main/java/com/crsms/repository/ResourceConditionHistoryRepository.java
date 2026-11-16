package com.crsms.repository;

import com.crsms.model.resource.ResourceConditionHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceConditionHistoryRepository extends JpaRepository<ResourceConditionHistory, Long> {
    List<ResourceConditionHistory> findByUnitIdOrderByRecordedAtDesc(Long unitId);
}

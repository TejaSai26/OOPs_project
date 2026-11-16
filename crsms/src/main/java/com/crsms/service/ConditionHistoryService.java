package com.crsms.service;

import com.crsms.dto.ApiDtos.ConditionHistoryResponse;
import com.crsms.model.enums.ConditionPhase;
import com.crsms.model.enums.ResourceCondition;
import com.crsms.model.resource.ResourceConditionHistory;
import com.crsms.model.resource.ResourceUnit;
import com.crsms.model.user.User;
import com.crsms.repository.ResourceConditionHistoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConditionHistoryService {
    private final ResourceConditionHistoryRepository history;

    public ConditionHistoryService(ResourceConditionHistoryRepository history) {
        this.history = history;
    }

    @Transactional
    public void record(ResourceUnit unit, ResourceCondition condition, ConditionPhase phase,
                       User actor, String note, String photoRef) {
        history.save(new ResourceConditionHistory(unit, condition, phase, note, photoRef, actor));
    }

    @Transactional(readOnly = true)
    public List<ConditionHistoryResponse> forUnit(Long unitId) {
        return history.findByUnitIdOrderByRecordedAtDesc(unitId).stream().map(row -> new ConditionHistoryResponse(
                row.getId(),
                row.getCondition().name(),
                row.getPhase().name(),
                row.getNote(),
                row.getPhotoRef(),
                row.getRecordedBy() == null ? null : row.getRecordedBy().getFullName(),
                row.getRecordedAt())).toList();
    }
}

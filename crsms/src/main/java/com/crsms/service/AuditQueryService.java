package com.crsms.service;

import com.crsms.dto.ApiDtos.AuditResponse;
import com.crsms.model.user.User;
import com.crsms.repository.AuditLogRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditQueryService {
    private final AuditLogRepository audits;

    public AuditQueryService(AuditLogRepository audits) {
        this.audits = audits;
    }

    @Transactional(readOnly = true)
    public List<AuditResponse> search(String action, String entityType, Long actorId, Instant from, Instant to) {
        String actionFilter = blank(action);
        String entityFilter = blank(entityType);
        return audits.search(actionFilter, entityFilter, actorId, from, to).stream().limit(300).map(row -> {
            User actor = row.getActor();
            return new AuditResponse(
                    row.getId(),
                    actor == null ? null : actor.getId(),
                    actor == null ? "System" : actor.getFullName(),
                    row.getAction(),
                    row.getEntityType(),
                    row.getEntityId(),
                    row.getDescription(),
                    row.getCreatedAt());
        }).toList();
    }

    private String blank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

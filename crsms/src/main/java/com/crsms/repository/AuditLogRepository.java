package com.crsms.repository;

import com.crsms.model.audit.AuditLog;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:action IS NULL OR a.action = :action)
              AND (:entityType IS NULL OR a.entityType = :entityType)
              AND (:actorId IS NULL OR a.actor.id = :actorId)
              AND (:from IS NULL OR a.createdAt >= :from)
              AND (:to IS NULL OR a.createdAt <= :to)
            ORDER BY a.createdAt DESC
            """)
    List<AuditLog> search(@Param("action") String action,
                           @Param("entityType") String entityType,
                           @Param("actorId") Long actorId,
                           @Param("from") Instant from,
                           @Param("to") Instant to);
}

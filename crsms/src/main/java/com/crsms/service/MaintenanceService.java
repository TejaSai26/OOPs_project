package com.crsms.service;

import com.crsms.dto.ApiDtos.MaintenanceCreate;
import com.crsms.dto.ApiDtos.MaintenanceResponse;
import com.crsms.dto.ApiDtos.MaintenanceUpdate;
import com.crsms.event.DomainEventPublisher;
import com.crsms.exception.ApiException;
import com.crsms.model.enums.ConditionPhase;
import com.crsms.model.enums.MaintenanceStatus;
import com.crsms.model.enums.NotificationType;
import com.crsms.model.enums.ResourceCondition;
import com.crsms.model.enums.UnitStatus;
import com.crsms.model.enums.UserRole;
import com.crsms.model.maintenance.MaintenanceRequest;
import com.crsms.model.resource.ResourceUnit;
import com.crsms.model.user.User;
import com.crsms.repository.MaintenanceRequestRepository;
import com.crsms.repository.ResourceUnitRepository;
import com.crsms.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenanceService {
    private final MaintenanceRequestRepository requests;
    private final ResourceUnitRepository units;
    private final UserRepository users;
    private final ConditionHistoryService conditions;
    private final DomainEventPublisher events;

    public MaintenanceService(MaintenanceRequestRepository requests, ResourceUnitRepository units,
                              UserRepository users, ConditionHistoryService conditions, DomainEventPublisher events) {
        this.requests = requests;
        this.units = units;
        this.users = users;
        this.conditions = conditions;
        this.events = events;
    }

    @Transactional
    public MaintenanceResponse report(User reporter, MaintenanceCreate body) {
        ResourceUnit unit = units.findById(body.unitId())
                .orElseThrow(() -> new ApiException(404, "Resource unit not found"));
        unit.changeCondition(ResourceCondition.DAMAGED);
        if (unit.getStatus() != UnitStatus.BORROWED) {
            unit.changeStatus(UnitStatus.MAINTENANCE_REQUIRED);
        }
        MaintenanceRequest saved = requests.save(new MaintenanceRequest(unit, reporter, body.description().trim(), body.photoRef()));
        conditions.record(unit, ResourceCondition.DAMAGED, ConditionPhase.DAMAGE_REPORT, reporter, body.description().trim(), body.photoRef());
        User owner = unit.getListing().getOwner();
        events.publish(reporter.getId(), owner.getId(), NotificationType.DAMAGE_REPORT.name(),
                "Damage reported",
                reporter.getFullName() + " reported a problem with " + unit.getAssetCode() + ".",
                "MAINTENANCE_REPORTED", "MaintenanceRequest", saved.getId(),
                "Maintenance request " + saved.getId() + " opened for " + unit.getAssetCode());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceResponse> list() {
        return requests.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MaintenanceResponse> mine(User reporter) {
        return requests.findByReporterIdOrderByCreatedAtDesc(reporter.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public MaintenanceResponse update(User actor, Long id, MaintenanceUpdate body) {
        if (actor.getRole() != UserRole.ADMIN) {
            throw new ApiException(403, "Only an administrator can update maintenance");
        }
        MaintenanceRequest request = requests.findById(id)
                .orElseThrow(() -> new ApiException(404, "Maintenance request not found"));
        MaintenanceStatus status = body.status() == null || body.status().isBlank()
                ? null
                : MaintenanceStatus.valueOf(body.status().trim().toUpperCase(Locale.ROOT));
        request.update(status, body.adminNote());
        User assignee = null;
        if (body.assigneeId() != null) {
            assignee = users.findById(body.assigneeId()).orElseThrow(() -> new ApiException(404, "Assignee not found"));
            if (assignee.getRole() != UserRole.STAFF && assignee.getRole() != UserRole.ADMIN) {
                throw new ApiException(400, "Assignee must be a staff member or administrator");
            }
        }
        request.assign(assignee, body.repairCost(), body.photoRef());
        ResourceUnit unit = request.getUnit();
        if (body.condition() != null && !body.condition().isBlank()) {
            unit.changeCondition(ResourceCondition.valueOf(body.condition().trim().toUpperCase(Locale.ROOT)));
        }
        if (body.unitStatus() != null && !body.unitStatus().isBlank()) {
            unit.changeStatus(UnitStatus.valueOf(body.unitStatus().trim().toUpperCase(Locale.ROOT)));
        } else if (status == MaintenanceStatus.IN_PROGRESS) {
            unit.changeStatus(UnitStatus.UNDER_REPAIR);
            unit.changeCondition(ResourceCondition.UNDER_MAINTENANCE);
        } else if (status == MaintenanceStatus.RESOLVED && unit.getStatus() != UnitStatus.BORROWED) {
            unit.changeStatus(UnitStatus.AVAILABLE);
            if (unit.getCondition() == ResourceCondition.DAMAGED || unit.getCondition() == ResourceCondition.UNDER_MAINTENANCE) {
                unit.changeCondition(ResourceCondition.GOOD);
            }
        }
        conditions.record(unit, unit.getCondition(), ConditionPhase.MAINTENANCE, actor,
                body.adminNote() == null ? "Maintenance update " + request.getStatus() : body.adminNote(),
                request.getPhotoRef());
        if (status == MaintenanceStatus.RESOLVED) {
            events.publish(actor.getId(), request.getReporter().getId(), NotificationType.MAINTENANCE_COMPLETED.name(),
                    "Maintenance completed",
                    unit.getAssetCode() + " is back from maintenance.",
                    "MAINTENANCE_UPDATED", "MaintenanceRequest", request.getId(),
                    "Resolved maintenance " + request.getId() + " on " + unit.getAssetCode());
        } else {
            events.publish(actor.getId(), null, null, null, null,
                    "MAINTENANCE_UPDATED", "MaintenanceRequest", request.getId(),
                    "Updated maintenance " + request.getId() + " to " + request.getStatus());
        }
        return toResponse(request);
    }

    private MaintenanceResponse toResponse(MaintenanceRequest request) {
        return new MaintenanceResponse(
                request.getId(),
                request.getUnit().getId(),
                request.getUnit().getAssetCode(),
                request.getUnit().getListing().getTitle(),
                request.getReporter().getFullName(),
                request.getDescription(),
                request.getStatus().name(),
                request.getAdminNote(),
                request.getUnit().getStatus().name(),
                request.getCreatedAt(),
                request.getAssignee() == null ? null : request.getAssignee().getFullName(),
                request.getRepairCost(),
                request.getPhotoRef());
    }
}

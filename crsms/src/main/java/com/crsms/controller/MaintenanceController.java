package com.crsms.controller;

import com.crsms.dto.ApiDtos.MaintenanceCreate;
import com.crsms.dto.ApiDtos.MaintenanceResponse;
import com.crsms.dto.ApiDtos.MaintenanceUpdate;
import com.crsms.service.CurrentUserService;
import com.crsms.service.MaintenanceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {
    private final MaintenanceService maintenance;
    private final CurrentUserService current;

    public MaintenanceController(MaintenanceService maintenance, CurrentUserService current) {
        this.maintenance = maintenance;
        this.current = current;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaintenanceResponse create(@Valid @RequestBody MaintenanceCreate body) {
        return maintenance.report(current.require(), body);
    }

    @GetMapping("/my")
    public List<MaintenanceResponse> mine() {
        return maintenance.mine(current.require());
    }

    @GetMapping
    public List<MaintenanceResponse> list() {
        return maintenance.list();
    }

    @PutMapping("/{id}")
    public MaintenanceResponse update(@PathVariable Long id, @RequestBody MaintenanceUpdate body) {
        return maintenance.update(current.require(), id, body);
    }
}

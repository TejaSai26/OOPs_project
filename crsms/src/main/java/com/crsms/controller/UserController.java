package com.crsms.controller;

import com.crsms.dto.ApiDtos.DashboardSummary;
import com.crsms.dto.ApiDtos.PasswordChange;
import com.crsms.dto.ApiDtos.ProfileUpdate;
import com.crsms.dto.ApiDtos.UserResponse;
import com.crsms.service.CurrentUserService;
import com.crsms.service.DashboardService;
import com.crsms.service.UserService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final CurrentUserService current;
    private final UserService users;
    private final DashboardService dashboard;

    public UserController(CurrentUserService current, UserService users, DashboardService dashboard) {
        this.current = current;
        this.users = users;
        this.dashboard = dashboard;
    }

    @GetMapping("/me")
    public UserResponse me() {
        return users.toResponse(current.require());
    }

    @PutMapping("/me")
    public UserResponse update(@Valid @RequestBody ProfileUpdate update) {
        return users.updateProfile(current.require(), update);
    }

    @PutMapping("/me/password")
    public Map<String, String> password(@Valid @RequestBody PasswordChange change) {
        users.changePassword(current.require(), change);
        return Map.of("status", "updated");
    }

    @GetMapping("/me/dashboard")
    public DashboardSummary summary() {
        return dashboard.mine(current.require());
    }
}

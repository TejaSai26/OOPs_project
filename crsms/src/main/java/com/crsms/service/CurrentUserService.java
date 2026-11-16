package com.crsms.service;

import com.crsms.exception.ApiException;
import com.crsms.model.user.User;
import com.crsms.repository.UserRepository;
import com.crsms.security.CampusUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final UserRepository users;

    public CurrentUserService(UserRepository users) {
        this.users = users;
    }

    public User require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(401, "Sign in required");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CampusUserDetails details) {
            return users.findById(details.getUser().getId())
                    .orElseThrow(() -> new ApiException(401, "Sign in required"));
        }
        throw new ApiException(401, "Sign in required");
    }
}

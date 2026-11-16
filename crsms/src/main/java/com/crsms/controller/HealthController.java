package com.crsms.controller;

import com.crsms.repository.ResourceCategoryRepository;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private final ResourceCategoryRepository categories;

    public HealthController(ResourceCategoryRepository categories) {
        this.categories = categories;
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "categories", categories.count());
    }
}

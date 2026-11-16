package com.crsms.repository;

import com.crsms.model.resource.ResourceCategory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceCategoryRepository extends JpaRepository<ResourceCategory, Long> {
    Optional<ResourceCategory> findByNameIgnoreCase(String name);
}

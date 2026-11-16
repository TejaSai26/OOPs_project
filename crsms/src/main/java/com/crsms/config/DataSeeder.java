package com.crsms.config;

import com.crsms.model.enums.PricingType;
import com.crsms.model.enums.UserRole;
import com.crsms.model.resource.ResourceCategory;
import com.crsms.model.resource.ResourceListing;
import com.crsms.model.resource.ResourceUnit;
import com.crsms.model.user.Faculty;
import com.crsms.model.user.Staff;
import com.crsms.model.user.Student;
import com.crsms.repository.ResourceCategoryRepository;
import com.crsms.repository.ResourceListingRepository;
import com.crsms.repository.ResourceUnitRepository;
import com.crsms.repository.UserRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    @Bean
    @ConditionalOnProperty(name = "crsms.seed-admin", havingValue = "true")
    CommandLineRunner seed(UserRepository users, PasswordEncoder encoder, Environment env,
                           ResourceCategoryRepository categories, ResourceListingRepository listings,
                           ResourceUnitRepository units) {
        return args -> {
            String adminEmail = env.getProperty("crsms.admin.email", "admin@campus.edu");
            if (users.findByEmailIgnoreCase(adminEmail).isEmpty()) {
                String password = env.getProperty("crsms.admin.password", "changeme");
                String name = env.getProperty("crsms.admin.name", "Campus Administrator");
                users.save(new Staff(adminEmail, encoder.encode(password), name, "Administration",
                        "ADM-0001", "Main office", UserRole.ADMIN));
            }
            if (!env.getProperty("crsms.seed-demo", Boolean.class, false)) {
                return;
            }
            if (users.findByEmailIgnoreCase("alice@campus.edu").isPresent()) {
                return;
            }
            Student alice = users.save(new Student("alice@campus.edu", encoder.encode("Student@123"),
                    "Alice Rahman", "Computer Science", "CS-1001"));
            users.save(new Student("bob@campus.edu", encoder.encode("Student@123"),
                    "Bob Iyer", "Mechanical", "ME-2044"));
            users.save(new Faculty("carol@campus.edu", encoder.encode("Faculty@123"),
                    "Dr. Carol Mendes", "Physics", "FAC-17", "Associate Professor"));
            ResourceCategory bikes = categories.findByNameIgnoreCase("Bicycles").orElseThrow();
            ResourceListing listing = listings.save(new ResourceListing(
                    alice, bikes, "Campus cruiser bicycles",
                    "Two city bikes available for same-day loans between classes.",
                    PricingType.HOURLY, new BigDecimal("15.00")));
            units.save(new ResourceUnit(listing, "BIKE-01", com.crsms.model.enums.ResourceCondition.GOOD, "North gate rack"));
            units.save(new ResourceUnit(listing, "BIKE-02", com.crsms.model.enums.ResourceCondition.GOOD, "North gate rack"));
        };
    }
}

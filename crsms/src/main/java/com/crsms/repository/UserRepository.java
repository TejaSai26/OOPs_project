package com.crsms.repository;

import com.crsms.model.enums.AccountStatus;
import com.crsms.model.enums.UserRole;
import com.crsms.model.user.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByAccountStatus(AccountStatus status);

    long countByRole(UserRole role);
}

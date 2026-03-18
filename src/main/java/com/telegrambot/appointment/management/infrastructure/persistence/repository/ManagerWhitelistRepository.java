package com.telegrambot.appointment.management.infrastructure.persistence.repository;

import com.telegrambot.appointment.management.domain.model.user.ManagerWhitelist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ManagerWhitelistRepository extends JpaRepository<ManagerWhitelist, String> {
    boolean existsByUsername(String username);
    boolean existsByUsernameIgnoreCase(String username);
}

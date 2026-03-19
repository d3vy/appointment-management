package com.telegrambot.appointment.management.infrastructure.persistence.repository.specialist;

import com.telegrambot.appointment.management.domain.model.user.specialist.SpecialistWhitelist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpecialistWhitelistRepository extends JpaRepository<SpecialistWhitelist, String> {
    boolean existsByUsernameIgnoreCase(String username);
}
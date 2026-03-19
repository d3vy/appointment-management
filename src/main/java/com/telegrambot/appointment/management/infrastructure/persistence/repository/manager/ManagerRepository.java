package com.telegrambot.appointment.management.infrastructure.persistence.repository.manager;

import com.telegrambot.appointment.management.domain.model.user.manager.Manager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ManagerRepository extends JpaRepository<Manager, Integer> {
    Optional<Manager> findByTelegramId(Long telegramId);
    boolean existsByTelegramId(Long telegramId);
}

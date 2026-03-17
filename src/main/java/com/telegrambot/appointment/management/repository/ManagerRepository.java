package com.telegrambot.appointment.management.repository;

import com.telegrambot.appointment.management.model.user.Manager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ManagerRepository extends JpaRepository<Manager, Integer> {
    Optional<Manager> findByTelegramId(Long telegramId);
    boolean existsByTelegramId(Long telegramId);
}

package com.telegrambot.appointment.management.repository;

import com.telegrambot.appointment.management.model.user.Specialist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpecialistRepository extends JpaRepository<Specialist, Integer> {
    Optional<Specialist> findByTelegramId(Long telegramId);
    boolean existsByTelegramId(Long telegramId);
}

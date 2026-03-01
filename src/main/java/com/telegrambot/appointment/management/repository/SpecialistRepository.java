package com.telegrambot.appointment.management.repository;

import com.telegrambot.appointment.management.model.user.Specialist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpecialistRepository extends JpaRepository<Specialist, Integer> {
    Optional<Specialist> findByTelegramId(Long telegramId);
}

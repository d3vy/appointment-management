package com.telegrambot.appointment.management.infrastructure.persistence.repository;

import com.telegrambot.appointment.management.domain.model.user.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Integer> {
    Optional<Client> findByTelegramId(Long telegramId);
    boolean existsByTelegramId(Long telegramId);
}

package com.telegrambot.appointment.management.repository;

import com.telegrambot.appointment.management.model.user.Client;
import com.telegrambot.appointment.management.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Integer> {
    Optional<Client> findByTelegramId(Long telegramId);
    boolean existsByTelegramId(Long telegramId);
}

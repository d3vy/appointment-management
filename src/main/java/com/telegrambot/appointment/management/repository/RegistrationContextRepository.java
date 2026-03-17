package com.telegrambot.appointment.management.repository;

import com.telegrambot.appointment.management.model.registration.RegistrationContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Redis-репозиторий для временного состояния регистрации.
 * TTL задан прямо в RegistrationContext (30 минут).
 * Ручная очистка не нужна — Redis удаляет записи автоматически.
 */
@Repository
public interface RegistrationContextRepository extends CrudRepository<RegistrationContext, Long> {
}

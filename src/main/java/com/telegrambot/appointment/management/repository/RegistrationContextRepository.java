package com.telegrambot.appointment.management.repository;

import com.telegrambot.appointment.management.model.registration.RegistrationContext;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationContextRepository extends JpaRepository<RegistrationContext, Long> {
}

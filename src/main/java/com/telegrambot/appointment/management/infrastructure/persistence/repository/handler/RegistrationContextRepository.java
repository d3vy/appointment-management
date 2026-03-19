package com.telegrambot.appointment.management.infrastructure.persistence.repository.handler;

import com.telegrambot.appointment.management.domain.model.registration.RegistrationContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistrationContextRepository extends CrudRepository<RegistrationContext, Long> {
}

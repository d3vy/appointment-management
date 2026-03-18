package com.telegrambot.appointment.management.repository;

import com.telegrambot.appointment.management.model.registration.RegistrationContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistrationContextRepository extends CrudRepository<RegistrationContext, Long> {
}

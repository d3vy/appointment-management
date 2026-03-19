package com.telegrambot.appointment.management.infrastructure.persistence.repository.handler;

import com.telegrambot.appointment.management.domain.model.appointment.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Integer> {
}

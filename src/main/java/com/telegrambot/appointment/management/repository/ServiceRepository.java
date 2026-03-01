package com.telegrambot.appointment.management.repository;

import com.telegrambot.appointment.management.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<Service, Integer> {
}

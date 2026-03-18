package com.telegrambot.appointment.management.repository;

import com.telegrambot.appointment.management.model.appointment.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Integer> {
}

package com.telegrambot.appointment.management.service;

import com.telegrambot.appointment.management.model.user.Manager;
import com.telegrambot.appointment.management.repository.ManagerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ManagerService {

    private final ManagerRepository managerRepository;

    public ManagerService(ManagerRepository managerRepository) {
        this.managerRepository = managerRepository;
    }

    public List<String> getAllManagersUsername() {
        return this.managerRepository.findAll().stream()
                .map(Manager::getUsername)
                .toList();
    }
}

package com.telegrambot.appointment.management.service;

import com.telegrambot.appointment.management.model.UserRole;
import com.telegrambot.appointment.management.model.user.User;
import com.telegrambot.appointment.management.repository.ClientRepository;
import com.telegrambot.appointment.management.repository.ManagerRepository;
import com.telegrambot.appointment.management.repository.SpecialistRepository;
import org.springframework.stereotype.Service;

@Service
public class UserRoleService {
    private final ManagerRepository managerRepository;
    private final SpecialistRepository specialistRepository;
    private final ClientRepository clientRepository;

    public UserRoleService(
            ManagerRepository managerRepository,
            SpecialistRepository specialistRepository,
            ClientRepository clientRepository
    ) {
        this.managerRepository = managerRepository;
        this.specialistRepository = specialistRepository;
        this.clientRepository = clientRepository;
    }

    public UserRole defineUserRoleByTelegramId(Long telegramId) {
        User user = this.clientRepository.findByTelegramId(telegramId).orElse(null);
        if (user == null) {
            user = this.specialistRepository.findByTelegramId(telegramId).orElse(null);
        } else {
            return UserRole.CLIENT;
        }

        if (user == null) {
            user = this.managerRepository.findByTelegramId(telegramId).orElse(null);
        } else {
            return UserRole.SPECIALIST;
        }

        if (user == null) {
            return UserRole.NOT_REGISTERED;
        } else {
            return UserRole.MANAGER;
        }
    }
}

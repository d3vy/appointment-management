package com.telegrambot.appointment.management.service;

import com.telegrambot.appointment.management.model.UserRole;
import com.telegrambot.appointment.management.model.user.User;
import com.telegrambot.appointment.management.repository.ClientRepository;
import com.telegrambot.appointment.management.repository.ManagerRepository;
import com.telegrambot.appointment.management.repository.ManagerWhitelistRepository;
import com.telegrambot.appointment.management.repository.SpecialistRepository;
import org.springframework.stereotype.Service;

@Service
public class UserRoleService {

    private final ClientRepository clientRepository;
    private final SpecialistRepository specialistRepository;
    private final ManagerRepository managerRepository;
    private final ManagerWhitelistRepository whitelistRepository;

    public UserRoleService(
            ClientRepository clientRepository,
            SpecialistRepository specialistRepository,
            ManagerRepository managerRepository,
            ManagerWhitelistRepository whitelistRepository
    ) {
        this.clientRepository = clientRepository;
        this.specialistRepository = specialistRepository;
        this.managerRepository = managerRepository;
        this.whitelistRepository = whitelistRepository;
    }

    public UserRole defineUserRoleByTelegramId(Long telegramId) {
        if (clientRepository.existsByTelegramId(telegramId)) return UserRole.CLIENT;
        if (specialistRepository.existsByTelegramId(telegramId)) return UserRole.SPECIALIST;
        if (managerRepository.existsByTelegramId(telegramId)) return UserRole.MANAGER;
        return UserRole.NOT_REGISTERED;
    }

    public boolean isManagerWhitelisted(String username) {
        if (username == null || username.isBlank()) return false;
        return whitelistRepository.existsByUsernameIgnoreCase(username);
    }
}

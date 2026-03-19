package com.telegrambot.appointment.management.domain.service;

import com.telegrambot.appointment.management.domain.model.user.UserRole;
import com.telegrambot.appointment.management.domain.model.user.UserRoleCache;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.ManagerWhitelistRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.SpecialistWhitelistRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.UserRoleCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRoleService {

    private static final Logger log = LoggerFactory.getLogger(UserRoleService.class);

    private final UserRoleCacheRepository roleCacheRepository;
    private final ManagerWhitelistRepository managerWhitelistRepository;
    private final SpecialistWhitelistRepository specialistWhitelistRepository;

    public UserRoleService(UserRoleCacheRepository roleCacheRepository,
                           ManagerWhitelistRepository managerWhitelistRepository,
                           SpecialistWhitelistRepository specialistWhitelistRepository) {
        this.roleCacheRepository = roleCacheRepository;
        this.managerWhitelistRepository = managerWhitelistRepository;
        this.specialistWhitelistRepository = specialistWhitelistRepository;
    }

    @Transactional(readOnly = true)
    public UserRole defineUserRoleByTelegramId(Long telegramId) {
        return roleCacheRepository.findByTelegramId(telegramId)
                .map(UserRoleCache::getRole)
                .orElse(UserRole.NOT_REGISTERED);
    }

    @Transactional(readOnly = true)
    public boolean isManagerWhitelisted(String username) {
        if (username == null || username.isBlank()) return false;
        boolean whitelisted = managerWhitelistRepository.existsByUsernameIgnoreCase(username);
        log.debug("Whitelist check username={}: {}", username, whitelisted);
        return whitelisted;
    }

    @Transactional
    public void assignRole(Long telegramId, UserRole role) {
        UserRoleCache cache = roleCacheRepository.findByTelegramId(telegramId)
                .orElse(new UserRoleCache(telegramId, role));
        cache.setRole(role);
        roleCacheRepository.save(cache);
        log.info("Assigned role {} to telegramId={}", role, telegramId);
    }
}

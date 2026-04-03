package com.telegrambot.appointment.management.domain.service;

import com.telegrambot.appointment.management.domain.model.user.UserRole;
import com.telegrambot.appointment.management.domain.model.user.UserRoleCache;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.client.ClientRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.manager.ManagerRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.manager.ManagerWhitelistRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.specialist.SpecialistRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.specialist.SpecialistWhitelistRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.user.UserRoleCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRoleService {

    private static final Logger log = LoggerFactory.getLogger(UserRoleService.class);

    private final UserRoleCacheRepository roleCacheRepository;
    private final ManagerRepository managerRepository;
    private final SpecialistRepository specialistRepository;
    private final ClientRepository clientRepository;
    private final ManagerWhitelistRepository managerWhitelistRepository;
    private final SpecialistWhitelistRepository specialistWhitelistRepository;

    public UserRoleService(UserRoleCacheRepository roleCacheRepository,
                           ManagerRepository managerRepository,
                           SpecialistRepository specialistRepository,
                           ClientRepository clientRepository,
                           ManagerWhitelistRepository managerWhitelistRepository,
                           SpecialistWhitelistRepository specialistWhitelistRepository) {
        this.roleCacheRepository = roleCacheRepository;
        this.managerRepository = managerRepository;
        this.specialistRepository = specialistRepository;
        this.clientRepository = clientRepository;
        this.managerWhitelistRepository = managerWhitelistRepository;
        this.specialistWhitelistRepository = specialistWhitelistRepository;
    }

    @Transactional(readOnly = true)
    public UserRole defineUserRoleByTelegramId(Long telegramId) {
        UserRole domainRole = deriveRoleFromDomainTables(telegramId);
        return roleCacheRepository.findByTelegramId(telegramId)
                .map(UserRoleCache::getRole)
                .map(stored -> resolveEffectiveRole(telegramId, stored, domainRole))
                .orElse(domainRole);
    }

    private UserRole resolveEffectiveRole(Long telegramId, UserRole stored, UserRole domainRole) {
        if (stored == domainRole) {
            return domainRole;
        }
        log.warn("User role cache mismatch telegramId={}, cachedRole={}, domainRole={}",
                telegramId, stored, domainRole);
        return domainRole;
    }

    private UserRole deriveRoleFromDomainTables(Long telegramId) {
        if (managerRepository.existsByTelegramId(telegramId)) {
            return UserRole.MANAGER;
        }
        if (specialistRepository.existsByTelegramId(telegramId)) {
            return UserRole.SPECIALIST;
        }
        if (clientRepository.existsByTelegramId(telegramId)) {
            return UserRole.CLIENT;
        }
        return UserRole.NOT_REGISTERED;
    }


    @Transactional(readOnly = true)
    public boolean isManagerWhitelisted(String username) {
        if (username == null || username.isBlank()) return false;
        boolean whitelisted = managerWhitelistRepository.existsByUsernameIgnoreCase(username);
        log.debug("Whitelist check username={}: {}", username, whitelisted);
        return whitelisted;
    }

    @Transactional(readOnly = true)
    public boolean isSpecialistWhitelisted(String username) {
        if (username == null || username.isBlank()) return false;
        boolean whitelisted = specialistWhitelistRepository.existsByUsernameIgnoreCase(username);
        log.debug("Specialist whitelist check username={}: {}", username, whitelisted);
        return whitelisted;
    }

    @Transactional
    public void assignRole(Long telegramId, UserRole role) {
        UserRole domainRole = deriveRoleFromDomainTables(telegramId);
        if (domainRole != role) {
            throw new IllegalStateException(
                    "Cannot assign role " + role + " when domain role is " + domainRole + " for telegramId=" + telegramId);
        }
        UserRoleCache cache = roleCacheRepository.findByTelegramId(telegramId)
                .orElse(new UserRoleCache(telegramId, role));
        cache.setRole(role);
        roleCacheRepository.save(cache);
        log.info("Assigned role {} to telegramId={}", role, telegramId);
    }
}

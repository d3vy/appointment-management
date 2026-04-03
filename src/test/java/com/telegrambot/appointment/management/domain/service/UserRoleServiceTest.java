package com.telegrambot.appointment.management.domain.service;

import com.telegrambot.appointment.management.domain.model.user.UserRole;
import com.telegrambot.appointment.management.domain.model.user.UserRoleCache;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.client.ClientRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.manager.ManagerRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.manager.ManagerWhitelistRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.specialist.SpecialistRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.specialist.SpecialistWhitelistRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.user.UserRoleCacheRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceTest {

    @Mock
    private UserRoleCacheRepository roleCacheRepository;
    @Mock
    private ManagerRepository managerRepository;
    @Mock
    private SpecialistRepository specialistRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private ManagerWhitelistRepository managerWhitelistRepository;
    @Mock
    private SpecialistWhitelistRepository specialistWhitelistRepository;

    @Test
    void defineUserRole_noCache_usesDomainClient() {
        UserRoleService service = service();
        long telegramId = 42L;
        when(roleCacheRepository.findByTelegramId(telegramId)).thenReturn(Optional.empty());
        when(managerRepository.existsByTelegramId(telegramId)).thenReturn(false);
        when(specialistRepository.existsByTelegramId(telegramId)).thenReturn(false);
        when(clientRepository.existsByTelegramId(telegramId)).thenReturn(true);

        assertEquals(UserRole.CLIENT, service.defineUserRoleByTelegramId(telegramId));
    }

    @Test
    void defineUserRole_cachedSpecialistMismatch_domainWins() {
        UserRoleService service = service();
        long telegramId = 99L;
        UserRoleCache cache = new UserRoleCache(telegramId, UserRole.SPECIALIST);
        when(roleCacheRepository.findByTelegramId(telegramId)).thenReturn(Optional.of(cache));
        when(managerRepository.existsByTelegramId(telegramId)).thenReturn(true);

        assertEquals(UserRole.MANAGER, service.defineUserRoleByTelegramId(telegramId));
    }

    @Test
    void assignRole_rejectsWhenDomainMismatch() {
        UserRoleService service = service();
        long telegramId = 7L;
        when(managerRepository.existsByTelegramId(telegramId)).thenReturn(false);
        when(specialistRepository.existsByTelegramId(telegramId)).thenReturn(false);
        when(clientRepository.existsByTelegramId(telegramId)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> service.assignRole(telegramId, UserRole.MANAGER));
        verifyNoInteractions(roleCacheRepository);
    }

    private UserRoleService service() {
        return new UserRoleService(roleCacheRepository, managerRepository, specialistRepository,
                clientRepository, managerWhitelistRepository, specialistWhitelistRepository);
    }
}

package com.telegrambot.appointment.management.infrastructure.persistence.repository.user;

import com.telegrambot.appointment.management.domain.model.user.UserRoleCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRoleCacheRepository extends JpaRepository<UserRoleCache, Long> {
    Optional<UserRoleCache> findByTelegramId(Long telegramId);
}

package com.telegrambot.appointment.management.repository;

import com.telegrambot.appointment.management.model.user.UserRoleCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRoleCacheRepository extends JpaRepository<UserRoleCache, Long> {
    Optional<UserRoleCache> findByTelegramId(Long telegramId);
}

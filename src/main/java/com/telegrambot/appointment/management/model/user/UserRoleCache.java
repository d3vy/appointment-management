package com.telegrambot.appointment.management.model.user;

import jakarta.persistence.*;

@Entity
@Table(name = "user_roles", schema = "public",
        indexes = @Index(name = "idx_user_roles_telegram_id", columnList = "telegram_id", unique = true))
public class UserRoleCache {

    @Id
    @Column(name = "telegram_id", nullable = false)
    private Long telegramId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    public UserRoleCache() {}

    public UserRoleCache(Long telegramId, UserRole role) {
        this.telegramId = telegramId;
        this.role = role;
    }

    public Long getTelegramId() { return telegramId; }
    public void setTelegramId(Long telegramId) { this.telegramId = telegramId; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
}

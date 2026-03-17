package com.telegrambot.appointment.management.model.registration;

import com.telegrambot.appointment.management.model.UserRole;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;

/**
 * Временное состояние регистрации — хранится в Redis с TTL 30 минут.
 *
 * Раньше хранилось в PostgreSQL, что было излишним:
 * 1. Эти данные никогда не джойнятся с другими таблицами.
 * 2. Брошенные регистрации никогда не очищались.
 * 3. Redis TTL делает очистку автоматически.
 */
@RedisHash("registration_context")
public class RegistrationContext implements Serializable {

    @Id
    private Long telegramId;

    private String username;

    private RegistrationStep step;

    private UserRole pendingRole;

    private String firstname;
    private String lastname;
    private String number;

    @TimeToLive
    private long ttl = 1800L; // 30 минут

    public RegistrationContext() {}

    public Long getTelegramId() { return telegramId; }
    public void setTelegramId(Long telegramId) { this.telegramId = telegramId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public RegistrationStep getStep() { return step; }
    public void setStep(RegistrationStep step) { this.step = step; }

    public UserRole getPendingRole() { return pendingRole; }
    public void setPendingRole(UserRole pendingRole) { this.pendingRole = pendingRole; }

    public String getFirstname() { return firstname; }
    public void setFirstname(String firstname) { this.firstname = firstname; }

    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public long getTtl() { return ttl; }
    public void setTtl(long ttl) { this.ttl = ttl; }
}

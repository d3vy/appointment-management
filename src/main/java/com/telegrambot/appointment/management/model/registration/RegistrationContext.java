package com.telegrambot.appointment.management.model.registration;

import com.telegrambot.appointment.management.model.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(schema = "registration", name = "registration_context")
public class RegistrationContext {

    @Id
    @Column(nullable = false, unique = true)
    private Long telegramId;

    private String username;

    @Enumerated(EnumType.STRING)
    private RegistrationStep step;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private String firstname;

    private String lastname;

    private String number;

    @Enumerated(EnumType.STRING)
    @Column(name = "pending_role")
    private UserRole pendingRole;

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public RegistrationStep getStep() {
        return step;
    }

    public void setStep(RegistrationStep step) {
        this.step = step;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Long getTelegramId() {
        return telegramId;
    }

    public void setTelegramId(Long telegramId) {
        this.telegramId = telegramId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public UserRole getPendingRole() {
        return pendingRole;
    }

    public void setPendingRole(UserRole pendingRole) {
        this.pendingRole = pendingRole;
    }
}

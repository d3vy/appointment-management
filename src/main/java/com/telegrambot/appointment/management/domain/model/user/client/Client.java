package com.telegrambot.appointment.management.domain.model.user.client;

import com.telegrambot.appointment.management.domain.model.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(schema = "client", name = "clients")
public class Client extends User {
    @Column(name = "notifications_enabled", nullable = false)
    private boolean notificationsEnabled = true;

    public boolean isNotificationsEnabled() {return notificationsEnabled;}
    public void setNotificationsEnabled(boolean notificationsEnabled) {this.notificationsEnabled = notificationsEnabled;}
}

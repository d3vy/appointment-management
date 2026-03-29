package com.telegrambot.appointment.management.domain.model.user.manager;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(schema = "manager", name = "manager_whitelist")
public class ManagerWhitelist {

    @Id
    @Column(name = "username")
    private String username;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}

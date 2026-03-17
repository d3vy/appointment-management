package com.telegrambot.appointment.management.model.user;

import jakarta.persistence.*;

@Entity
@Table(schema = "manager", name = "manager_whitelist")
public class ManagerWhitelist {

    @Id
    @Column(name = "username")
    private String username;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}

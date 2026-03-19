package com.telegrambot.appointment.management.domain.model.user.specialist;

import jakarta.persistence.*;

@Entity
@Table(schema = "specialist", name = "specialist_whitelist")
public class SpecialistWhitelist {

    @Id
    @Column(name = "username")
    private String username;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
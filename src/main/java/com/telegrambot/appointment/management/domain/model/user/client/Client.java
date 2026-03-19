package com.telegrambot.appointment.management.domain.model.user.client;

import com.telegrambot.appointment.management.domain.model.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(schema = "client", name = "clients")
public class Client extends User {
}

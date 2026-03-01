package com.telegrambot.appointment.management.model.user;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(schema = "client", name = "clients")
public class Client extends User {
}

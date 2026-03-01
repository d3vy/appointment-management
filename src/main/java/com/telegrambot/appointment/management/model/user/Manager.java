package com.telegrambot.appointment.management.model.user;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(schema = "manager", name = "managers")
public class Manager extends User {
}

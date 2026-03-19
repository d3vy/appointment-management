package com.telegrambot.appointment.management.domain.model.user.manager;

import com.telegrambot.appointment.management.domain.model.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(schema = "manager", name = "managers")
public class Manager extends User {
}

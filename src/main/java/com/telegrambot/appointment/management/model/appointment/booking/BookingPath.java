package com.telegrambot.appointment.management.model.appointment.booking;

public enum BookingPath {
    BY_SERVICE,    // услуга → специалисты → день → слот
    BY_SPECIALIST  // специалист → его услуги → день → слот
}

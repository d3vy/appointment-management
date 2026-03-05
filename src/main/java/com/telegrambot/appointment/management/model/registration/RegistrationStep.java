package com.telegrambot.appointment.management.model.registration;

public enum RegistrationStep {
    // Общие шаги
    ENTER_FIRSTNAME,
    ENTER_LASTNAME,
    ENTER_NUMBER,

    // Менеджерский флоу
    MANAGER_ENTER_FIRSTNAME,
    MANAGER_ENTER_LASTNAME,
    MANAGER_ENTER_NUMBER
}
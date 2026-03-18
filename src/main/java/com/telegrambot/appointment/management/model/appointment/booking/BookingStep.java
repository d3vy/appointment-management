package com.telegrambot.appointment.management.model.appointment.booking;

public enum BookingStep {
    SELECT_PATH,           // выбор пути: через услугу или через специалиста
    SELECT_SERVICE,
    SELECT_SPECIALIST,
    SELECT_SPECIALIST_SERVICE,
    SELECT_DAY,
    SELECT_SLOT,           // выбор времени записи
    CONFIRM
}

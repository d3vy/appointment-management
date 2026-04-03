package com.telegrambot.appointment.management.domain.model.notification;

public enum TelegramNotificationOutboxEventType {
    SPECIALIST_NEW_APPOINTMENT,
    SPECIALIST_CLIENT_CANCELLATION,
    SPECIALIST_MANAGER_CANCELLATION,
    CLIENT_MANAGER_CANCELLATION
}

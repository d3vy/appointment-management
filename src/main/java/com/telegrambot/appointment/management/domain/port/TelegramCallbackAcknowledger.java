package com.telegrambot.appointment.management.domain.port;

public interface TelegramCallbackAcknowledger {
    void acknowledge(String callbackQueryId);
}

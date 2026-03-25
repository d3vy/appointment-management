package com.telegrambot.appointment.management.domain.port;

public interface TelegramTextMessageSender {
    void sendText(Long chatId, String text);
}

package com.telegrambot.appointment.management.adapter.telegram.role;

import com.telegrambot.appointment.management.adapter.telegram.TelegramReply;
import com.telegrambot.appointment.management.domain.model.user.UserRole;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

public interface TelegramRoleHandler {

    UserRole role();

    void handleMessage(Message message, TelegramReply reply);

    default void handleContact(Message message, TelegramReply reply) {
    }

    void handleCallback(CallbackQuery callback, TelegramReply reply);
}

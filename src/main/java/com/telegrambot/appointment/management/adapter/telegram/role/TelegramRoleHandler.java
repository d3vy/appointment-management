package com.telegrambot.appointment.management.adapter.telegram.role;

import com.telegrambot.appointment.management.domain.model.user.UserRole;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.function.Consumer;

public interface TelegramRoleHandler {

    UserRole role();

    void handleMessage(Message message, Consumer<SendMessage> sender);

    void handleCallback(CallbackQuery callback, Consumer<SendMessage> sender);
}

package com.telegrambot.appointment.management.adapter.telegram;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

public final class TelegramReply {

    private final Consumer<BotApiMethod<?>> executor;

    public TelegramReply(Consumer<BotApiMethod<?>> executor) {
        this.executor = Objects.requireNonNull(executor);
    }

    public void send(SendMessage message) {
        executor.accept(message);
    }

    public void sendOrEdit(SendMessage message, Integer messageId) {
        if (messageId != null) {
            executor.accept(toEditMessageText(message, messageId));
        } else {
            executor.accept(message);
        }
    }

    public void deleteMessage(String chatId, Integer messageId) {
        if (messageId == null || chatId == null || chatId.isBlank()) {
            return;
        }
        executor.accept(DeleteMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build());
    }

    static EditMessageText toEditMessageText(SendMessage sendMessage, int messageId) {
        EditMessageText edit = EditMessageText.builder()
                .chatId(sendMessage.getChatId())
                .messageId(messageId)
                .text(sendMessage.getText())
                .build();
        if (sendMessage.getParseMode() != null) {
            edit.setParseMode(sendMessage.getParseMode());
        }
        if (Boolean.TRUE.equals(sendMessage.getDisableWebPagePreview())) {
            edit.setDisableWebPagePreview(true);
        }
        if (sendMessage.getReplyMarkup() instanceof InlineKeyboardMarkup inlineMarkup) {
            edit.setReplyMarkup(inlineMarkup);
        } else {
            edit.setReplyMarkup(new InlineKeyboardMarkup(new ArrayList<>()));
        }
        return edit;
    }
}

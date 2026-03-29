package com.telegrambot.appointment.management.adapter.telegram;

import com.telegrambot.appointment.management.adapter.telegram.router.UpdateRouter;
import com.telegrambot.appointment.management.infrastructure.config.BotConfig;
import com.telegrambot.appointment.management.infrastructure.service.TelegramMessageAnchorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
public class AppointmentBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(AppointmentBot.class);

    private final BotConfig botConfig;
    private final UpdateRouter updateRouter;
    private final TelegramMessageAnchorService telegramMessageAnchorService;

    public AppointmentBot(BotConfig botConfig,
                          UpdateRouter updateRouter,
                          TelegramMessageAnchorService telegramMessageAnchorService) {
        this.botConfig = botConfig;
        this.updateRouter = updateRouter;
        this.telegramMessageAnchorService = telegramMessageAnchorService;
    }

    @Override
    public String getBotUsername() { return botConfig.getBotName(); }

    @Override
    public String getBotToken() { return botConfig.getToken(); }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            updateRouter.route(update, new TelegramReply(this::executeTelegramMethod));
        } catch (Exception e) {
            log.error("Unhandled exception while processing update", e);
        }
    }

    public void executeTelegramMethod(BotApiMethod<?> method) {
        try {
            execute(method);
        } catch (TelegramApiException e) {
            if (method instanceof EditMessageText editMessageText) {
                log.warn("EditMessageText failed, anchor cleared and sending new message: {}", e.getMessage());
                forgetAnchorForPrivateChat(editMessageText.getChatId());
                sendFallbackAfterEditFailure(editMessageText);
            } else if (method instanceof DeleteMessage) {
                log.debug("DeleteMessage not applied: {}", e.getMessage());
            } else {
                log.error("Failed to execute {} for chat: {}", method.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    private void forgetAnchorForPrivateChat(Object chatIdRaw) {
        if (chatIdRaw == null) {
            return;
        }
        try {
            telegramMessageAnchorService.forget(Long.parseLong(chatIdRaw.toString()));
        } catch (NumberFormatException ex) {
            log.debug("Anchor skip forget, non-numeric chatId: {}", chatIdRaw);
        }
    }

    private void sendFallbackAfterEditFailure(EditMessageText edit) {
        SendMessage fallback = SendMessage.builder()
                .chatId(edit.getChatId().toString())
                .text(edit.getText())
                .build();
        if (edit.getParseMode() != null) {
            fallback.setParseMode(edit.getParseMode());
        }
        if (Boolean.TRUE.equals(edit.getDisableWebPagePreview())) {
            fallback.setDisableWebPagePreview(true);
        }
        if (edit.getReplyMarkup() != null) {
            fallback.setReplyMarkup(edit.getReplyMarkup());
        }
        try {
            execute(fallback);
        } catch (TelegramApiException ex) {
            log.error("Fallback SendMessage after edit failure: {}", ex.getMessage());
        }
    }

    public void sendMessage(SendMessage message) {
        executeTelegramMethod(message);
    }

    public void executeSendMessageOrThrow(SendMessage message) throws TelegramApiException {
        execute(message);
    }
}

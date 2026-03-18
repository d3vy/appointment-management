package com.telegrambot.appointment.management.adapter.telegram;

import com.telegrambot.appointment.management.adapter.telegram.router.UpdateRouter;
import com.telegrambot.appointment.management.infrastructure.config.BotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
public class AppointmentBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(AppointmentBot.class);

    private final BotConfig botConfig;
    private final UpdateRouter updateRouter;

    public AppointmentBot(BotConfig botConfig, UpdateRouter updateRouter) {
        this.botConfig = botConfig;
        this.updateRouter = updateRouter;
    }

    @Override
    public String getBotUsername() { return botConfig.getBotName(); }

    @Override
    public String getBotToken() { return botConfig.getToken(); }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            updateRouter.route(update, this::sendMessage);
        } catch (Exception e) {
            log.error("Unhandled exception while processing update", e);
        }
    }

    public void sendMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chatId={}: {}", message.getChatId(), e.getMessage());
        }
    }
}

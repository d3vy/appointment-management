package com.telegrambot.appointment.management.config;

import com.telegrambot.appointment.management.service.AppointmentBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
public class BotInitializer {

    private static final Logger log = LoggerFactory.getLogger(BotInitializer.class);

    private final AppointmentBot bot;

    public BotInitializer(AppointmentBot bot) {
        this.bot = bot;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(bot);
            log.info("Telegram bot registered successfully");
        } catch (TelegramApiException e) {
            log.error("Failed to register Telegram bot", e);
        }
    }
}

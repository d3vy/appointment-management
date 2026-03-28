package com.telegrambot.appointment.management.infrastructure.config;

import com.telegrambot.appointment.management.adapter.telegram.AppointmentBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(name = "telegram.enabled", havingValue = "true", matchIfMissing = true)
public class BotInitializer {

    private static final Logger log = LoggerFactory.getLogger(BotInitializer.class);

    private final AppointmentBot bot;
    private final BotCommandsRegistry commandsRegistry;
    private final TelegramBotLifecycle telegramBotLifecycle;
    private final AtomicBoolean startupCompleted = new AtomicBoolean(false);

    public BotInitializer(AppointmentBot bot,
                          BotCommandsRegistry commandsRegistry,
                          TelegramBotLifecycle telegramBotLifecycle) {
        this.bot = bot;
        this.commandsRegistry = commandsRegistry;
        this.telegramBotLifecycle = telegramBotLifecycle;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!startupCompleted.compareAndSet(false, true)) {
            return;
        }
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(bot);
            log.info("Telegram bot registered successfully");
            commandsRegistry.registerDefaultCommands(bot);
            telegramBotLifecycle.markRegistered();
        } catch (TelegramApiException e) {
            log.error("Failed to register Telegram bot", e);
            telegramBotLifecycle.markFailed();
        }
    }
}

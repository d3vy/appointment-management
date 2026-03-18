package com.telegrambot.appointment.management.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Component
public class BotCommandsRegistry {

    private static final Logger log = LoggerFactory.getLogger(BotCommandsRegistry.class);


    private static final List<BotCommand> DEFAULT_COMMANDS = List.of(
            new BotCommand("/menu", "Главное меню"),
            new BotCommand("/appointments", "Мои записи"),
            new BotCommand("/help", "Список команд")
    );

    public void registerDefaultCommands(AbsSender sender) {
        try {
            sender.execute(SetMyCommands.builder()
                    .commands(DEFAULT_COMMANDS)
                    .scope(new BotCommandScopeDefault())
                    .build());
            log.info("Bot commands registered successfully");
        } catch (TelegramApiException e) {
            log.error("Failed to register bot commands", e);
        }
    }
}
package com.telegrambot.appointment.management.adapter.telegram.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
public class HelpHandler {

    public SendMessage prepareHelpForUnregistered(Long chatId) {
        String text = """
                📋 Доступные команды:
                
                /start — начать работу с ботом
                /help — список команд
                
                Для записи к мастеру необходимо зарегистрироваться.
                Нажмите /start чтобы начать.
                """;
        return new SendMessage(chatId.toString(), text);
    }

    public SendMessage prepareHelpForClient(Long chatId) {
        String text = """
                📋 Доступные команды:
                
                /menu — выбрать услугу и записаться
                /appointments — мои записи
                /help — список команд
                """;
        return new SendMessage(chatId.toString(), text);
    }

    public SendMessage prepareHelpForManager(Long chatId) {
        String text = """
                📋 Команды менеджера:
                
                /menu — главное меню
                /specialists — управление специалистами
                /schedule — расписание
                /help — список команд
                """;
        return new SendMessage(chatId.toString(), text);
    }

    public SendMessage prepareHelpForSpecialist(Long chatId) {
        String text = """
                📋 Команды специалиста:
                
                /schedule — моё расписание
                /appointments — мои записи на сегодня
                /help — список команд
                """;
        return new SendMessage(chatId.toString(), text);
    }
}

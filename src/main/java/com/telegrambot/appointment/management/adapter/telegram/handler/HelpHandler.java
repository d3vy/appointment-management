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
                
                /menu — главное меню (запись и записи)
                /make_appointment — сразу начать запись
                /appointments — мои записи
                /help — список команд
                """;
        return new SendMessage(chatId.toString(), text);
    }

    public SendMessage prepareHelpForManager(Long chatId) {
        String text = """
                📋 Команды менеджера:
                
                /menu — специалисты, расписание, услуги, привязка услуг
                /help — список команд
                """;
        return new SendMessage(chatId.toString(), text);
    }

    public SendMessage prepareHelpForSpecialist(Long chatId) {
        String text = """
                📋 Команды специалиста:
                
                /menu — расписание и записи
                /help — список команд
                """;
        return new SendMessage(chatId.toString(), text);
    }
}

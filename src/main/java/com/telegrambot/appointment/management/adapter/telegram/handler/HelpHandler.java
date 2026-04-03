package com.telegrambot.appointment.management.adapter.telegram.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

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
        return withInlineButton(chatId, text, "Начать регистрацию", "REGISTER");
    }

    public SendMessage prepareHelpForClient(Long chatId) {
        String text = """
                📋 Доступные команды:
                
                /menu — главное меню (запись и записи)
                /make_appointment — сразу начать запись
                /appointments — мои записи
                /help — список команд
                """;
        return withInlineButton(chatId, text, "◀️ В меню", "CLIENT_MAIN_MENU");
    }

    public SendMessage prepareHelpForManager(Long chatId) {
        String text = """
                📋 Команды менеджера:
                
                /menu — специалисты, расписание, услуги, привязка услуг
                /help — список команд
                """;
        return withInlineButton(chatId, text, "◀️ В меню", "MANAGER_MAIN_MENU");
    }

    public SendMessage prepareHelpForSpecialist(Long chatId) {
        String text = """
                📋 Команды специалиста:
                
                /menu — расписание, записи и услуги
                /help — список команд
                """;
        return withInlineButton(chatId, text, "◀️ В меню", "SPECIALIST_MAIN_MENU");
    }

    private static SendMessage withInlineButton(Long chatId, String text, String buttonLabel, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(buttonLabel);
        button.setCallbackData(callbackData);
        SendMessage message = new SendMessage(chatId.toString(), text);
        message.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(button))));
        return message;
    }
}
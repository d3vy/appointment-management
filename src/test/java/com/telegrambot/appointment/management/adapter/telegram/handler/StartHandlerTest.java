package com.telegrambot.appointment.management.adapter.telegram.handler;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartHandlerTest {

    private final StartHandler startHandler = new StartHandler();

    @Test
    void prepareStartMessageBuildsWelcomeTextAndRegisterButton() {
        Message message = buildMessage(12345L, "Ilia");

        SendMessage result = startHandler.prepareStartMessage(message);

        assertEquals("12345", result.getChatId());
        assertTrue(result.getText().contains("Здравствуйте, Ilia"));

        InlineKeyboardMarkup keyboard = assertInstanceOf(InlineKeyboardMarkup.class, result.getReplyMarkup());
        List<List<InlineKeyboardButton>> rows = keyboard.getKeyboard();
        assertEquals(1, rows.size());
        assertEquals(1, rows.getFirst().size());
        assertEquals("Регистрация", rows.getFirst().getFirst().getText());
        assertEquals("REGISTER", rows.getFirst().getFirst().getCallbackData());
    }

    private Message buildMessage(Long chatId, String firstName) {
        User user = new User();
        user.setId(1L);
        user.setFirstName(firstName);

        Chat chat = new Chat();
        chat.setId(chatId);

        Message message = new Message();
        message.setFrom(user);
        message.setChat(chat);
        message.setText("/start");
        return message;
    }
}

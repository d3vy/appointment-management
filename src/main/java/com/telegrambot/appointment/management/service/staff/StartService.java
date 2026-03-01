package com.telegrambot.appointment.management.service.staff;

import com.vdurmont.emoji.EmojiParser;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Service
public class StartService {


    public SendMessage prepareStartMessage(Message message) {
        String text = EmojiParser.parseToUnicode(
                "Здравствуйте, " + message.getFrom().getFirstName() + " 👋\n" +
                        "Выберите услугу и запишитесь к мастеру в пару кликов\n" +
                        "(Но сначала небольшая регистрация)"
        );

        InlineKeyboardButton registerButton = new InlineKeyboardButton();
        registerButton.setText("Регстрация");
        registerButton.setCallbackData("REGISTER");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(
                List.of(List.of(registerButton))
        );

        SendMessage msg = new SendMessage(message.getChatId().toString(), text);
        msg.setReplyMarkup(keyboard);

        return msg;
    }

}
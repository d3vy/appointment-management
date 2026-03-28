package com.telegrambot.appointment.management.adapter.telegram.handler;

import com.vdurmont.emoji.EmojiParser;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component
public class MenuHandler {

    public SendMessage prepareClientMenu(Message message, boolean notificationsEnabled) {
        String text = EmojiParser.parseToUnicode("Меню клиента 📋");

        InlineKeyboardButton scheduleAppointment = button("📅 Записаться",    "APPOINTMENTS_SCHEDULE");
        InlineKeyboardButton myAppointments      = button("📋 Мои записи",    "APPOINTMENTS_MY");
        InlineKeyboardButton notifications       = button(
                notificationsEnabled ? "🔔 Уведомления: вкл" : "🔕 Уведомления: выкл",
                "TOGGLE_NOTIFICATIONS"
        );

        SendMessage msg = new SendMessage(message.getChatId().toString(), text);
        msg.setReplyMarkup(new InlineKeyboardMarkup(List.of(
                List.of(scheduleAppointment),
                List.of(myAppointments),
                List.of(notifications)
        )));
        return msg;
    }

    public SendMessage prepareManagerMenu(Message message) {
        String text = EmojiParser.parseToUnicode("Меню менеджера 🗂");

        InlineKeyboardButton addSpecialist = button("➕ Добавить специалиста", "MANAGER_ADD_SPECIALIST");
        InlineKeyboardButton specialists = button("👤 Специалисты", "MANAGER_SPECIALISTS");
        InlineKeyboardButton schedule    = button("📆 Расписание", "MANAGER_SCHEDULE");
        InlineKeyboardButton addService    = button("💈 Добавить услугу", "MANAGER_ADD_SERVICE");
        InlineKeyboardButton linkService  = button("🔗 Привязать услугу", "MANAGER_LINK_SERVICE");

        SendMessage msg = new SendMessage(message.getChatId().toString(), text);
        msg.setReplyMarkup(new InlineKeyboardMarkup(List.of(
                List.of(addSpecialist),
                List.of(specialists),
                List.of(schedule),
                List.of(addService),
                List.of(linkService)
        )));
        return msg;
    }

    public SendMessage prepareSpecialistMenu(Message message) {
        String text = EmojiParser.parseToUnicode("Меню специалиста 🛠");

        InlineKeyboardButton schedule     = button("📆 Моё расписание", "SPECIALIST_SCHEDULE");
        InlineKeyboardButton appointments = button("📋 Мои записи",     "SPECIALIST_APPOINTMENTS");
        InlineKeyboardButton services     = button("💈 Мои услуги",      "SPECIALIST_SERVICES");

        SendMessage msg = new SendMessage(message.getChatId().toString(), text);
        msg.setReplyMarkup(new InlineKeyboardMarkup(List.of(
                List.of(schedule),
                List.of(appointments),
                List.of(services)
        )));
        return msg;
    }

    private InlineKeyboardButton button(String text, String callbackData) {
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText(text);
        btn.setCallbackData(callbackData);
        return btn;
    }
}

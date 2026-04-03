package com.telegrambot.appointment.management.adapter.telegram.role;

import com.telegrambot.appointment.management.adapter.telegram.TelegramReply;
import com.telegrambot.appointment.management.adapter.telegram.handler.HelpHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.MenuHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.StartHandler;
import com.telegrambot.appointment.management.domain.model.user.UserRole;
import com.telegrambot.appointment.management.domain.service.SpecialistService;
import com.telegrambot.appointment.management.infrastructure.service.TelegramMessageAnchorService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component
public class SpecialistRoleHandler implements TelegramRoleHandler {

    private final StartHandler startHandler;
    private final MenuHandler menuHandler;
    private final HelpHandler helpHandler;
    private final SpecialistService specialistService;
    private final TelegramMessageAnchorService anchorService;

    public SpecialistRoleHandler(StartHandler startHandler,
                                 MenuHandler menuHandler,
                                 HelpHandler helpHandler,
                                 SpecialistService specialistService,
                                 TelegramMessageAnchorService anchorService) {
        this.startHandler = startHandler;
        this.menuHandler = menuHandler;
        this.helpHandler = helpHandler;
        this.specialistService = specialistService;
        this.anchorService = anchorService;
    }

    @Override
    public UserRole role() {
        return UserRole.SPECIALIST;
    }

    @Override
    public void handleMessage(Message message, TelegramReply reply) {
        Long telegramId = message.getFrom().getId();
        Long chatId = message.getChatId();
        String text = message.getText();

        switch (text) {
            case "/start" -> {
                anchorService.forget(telegramId);
                reply.send(startHandler.prepareStartMessage(message));
            }
            case "/menu" -> {
                anchorService.forget(telegramId);
                reply.send(menuHandler.prepareSpecialistMenu(chatId));
            }
            case "/help" -> sendWithOptionalEdit(telegramId, helpHandler.prepareHelpForSpecialist(chatId), reply);
            default -> sendWithOptionalEdit(telegramId, specialistUnknownCommand(chatId), reply);
        }
    }

    @Override
    public void handleCallback(CallbackQuery callback, TelegramReply reply) {
        String data = callback.getData();
        Long telegramId = callback.getFrom().getId();
        Message message = (Message) callback.getMessage();
        Long chatId = message.getChatId();
        Integer messageId = message.getMessageId();

        anchorService.remember(telegramId, messageId);

        switch (data) {
            case "ADD_SPECIALIST_TO_WHITELIST" -> reply.sendOrEdit(specialistWhitelistNotice(chatId), messageId);
            case "SPECIALIST_MAIN_MENU" ->
                    reply.sendOrEdit(menuHandler.prepareSpecialistMenu(chatId), messageId);
            case "SPECIALIST_SCHEDULE" ->
                    reply.sendOrEdit(specialistService.buildScheduleMessage(telegramId, chatId), messageId);
            case "SPECIALIST_APPOINTMENTS" ->
                    reply.sendOrEdit(specialistService.buildAppointmentsMessage(telegramId, chatId), messageId);
            case "SPECIALIST_SERVICES" ->
                    reply.sendOrEdit(specialistService.buildMyServicesMessage(telegramId, chatId), messageId);
        }
    }

    private static SendMessage specialistUnknownCommand(Long chatId) {
        InlineKeyboardButton menu = new InlineKeyboardButton();
        menu.setText("◀️ В меню");
        menu.setCallbackData("SPECIALIST_MAIN_MENU");
        SendMessage outgoing = new SendMessage(chatId.toString(),
                "Неизвестная команда. Используйте /help для списка команд.");
        outgoing.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(menu))));
        return outgoing;
    }

    private static SendMessage specialistWhitelistNotice(Long chatId) {
        InlineKeyboardButton menu = new InlineKeyboardButton();
        menu.setText("◀️ В меню");
        menu.setCallbackData("SPECIALIST_MAIN_MENU");
        SendMessage outgoing = new SendMessage(chatId.toString(),
                "Добавлять специалистов в whitelist может только менеджер. Попросите менеджера добавить вас через меню «Добавить специалиста».");
        outgoing.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(menu))));
        return outgoing;
    }

    private void sendWithOptionalEdit(Long telegramId, SendMessage outgoing, TelegramReply reply) {
        anchorService.currentMessageId(telegramId).ifPresentOrElse(
                messageId -> reply.sendOrEdit(outgoing, messageId),
                () -> reply.send(outgoing));
    }
}

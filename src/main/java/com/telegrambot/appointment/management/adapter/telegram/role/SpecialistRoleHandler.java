package com.telegrambot.appointment.management.adapter.telegram.role;

import com.telegrambot.appointment.management.adapter.telegram.TelegramReply;
import com.telegrambot.appointment.management.adapter.telegram.handler.HelpHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.MenuHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.StartHandler;
import com.telegrambot.appointment.management.domain.model.user.UserRole;
import com.telegrambot.appointment.management.domain.service.SpecialistService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
public class SpecialistRoleHandler implements TelegramRoleHandler {

    private final StartHandler startHandler;
    private final MenuHandler menuHandler;
    private final HelpHandler helpHandler;
    private final SpecialistService specialistService;

    public SpecialistRoleHandler(StartHandler startHandler,
                                 MenuHandler menuHandler,
                                 HelpHandler helpHandler,
                                 SpecialistService specialistService) {
        this.startHandler = startHandler;
        this.menuHandler = menuHandler;
        this.helpHandler = helpHandler;
        this.specialistService = specialistService;
    }

    @Override
    public UserRole role() {
        return UserRole.SPECIALIST;
    }

    @Override
    public void handleMessage(Message message, TelegramReply reply) {
        Long chatId = message.getChatId();
        String text = message.getText();

        switch (text) {
            case "/start" -> reply.send(startHandler.prepareStartMessage(message));
            case "/menu" -> reply.send(menuHandler.prepareSpecialistMenu(message));
            case "/help" -> reply.send(helpHandler.prepareHelpForSpecialist(chatId));
            default -> reply.send(new SendMessage(chatId.toString(),
                    "Неизвестная команда. Используйте /help для списка команд."));
        }
    }

    @Override
    public void handleCallback(CallbackQuery callback, TelegramReply reply) {
        String data = callback.getData();
        Long telegramId = callback.getFrom().getId();
        Message message = (Message) callback.getMessage();
        Long chatId = message.getChatId();
        Integer messageId = message.getMessageId();

        switch (data) {
            case "ADD_SPECIALIST_TO_WHITELIST" -> reply.sendOrEdit(new SendMessage(chatId.toString(),
                    "Добавлять специалистов в whitelist может только менеджер. Попросите менеджера добавить вас через меню «Добавить специалиста»."), messageId);
            case "SPECIALIST_SCHEDULE" ->
                    reply.sendOrEdit(specialistService.buildScheduleMessage(telegramId, chatId), messageId);
            case "SPECIALIST_APPOINTMENTS" ->
                    reply.sendOrEdit(specialistService.buildAppointmentsMessage(telegramId, chatId), messageId);
            case "SPECIALIST_SERVICES" ->
                    reply.sendOrEdit(specialistService.buildMyServicesMessage(telegramId, chatId), messageId);
        }
    }
}

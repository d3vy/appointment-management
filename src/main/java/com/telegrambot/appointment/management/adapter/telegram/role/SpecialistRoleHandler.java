package com.telegrambot.appointment.management.adapter.telegram.role;

import com.telegrambot.appointment.management.adapter.telegram.handler.HelpHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.MenuHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.StartHandler;
import com.telegrambot.appointment.management.domain.model.user.UserRole;
import com.telegrambot.appointment.management.domain.service.SpecialistService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.function.Consumer;

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
    public void handleMessage(Message message, Consumer<SendMessage> sender) {
        Long chatId = message.getChatId();
        String text = message.getText();

        switch (text) {
            case "/start" -> sender.accept(startHandler.prepareStartMessage(message));
            case "/menu" -> sender.accept(menuHandler.prepareSpecialistMenu(message));
            case "/help" -> sender.accept(helpHandler.prepareHelpForSpecialist(chatId));
            default -> sender.accept(new SendMessage(chatId.toString(),
                    "Неизвестная команда. Используйте /help для списка команд."));
        }
    }

    @Override
    public void handleCallback(CallbackQuery callback, Consumer<SendMessage> sender) {
        String data = callback.getData();
        Long telegramId = callback.getFrom().getId();
        Message message = (Message) callback.getMessage();
        Long chatId = message.getChatId();

        switch (data) {
            case "ADD_SPECIALIST_TO_WHITELIST" -> sender.accept(new SendMessage(chatId.toString(),
                    "Добавлять специалистов в whitelist может только менеджер. Попросите менеджера добавить вас через меню «Добавить специалиста»."));
            case "SPECIALIST_SCHEDULE" ->
                    sender.accept(specialistService.buildScheduleMessage(telegramId, chatId));
            case "SPECIALIST_APPOINTMENTS" ->
                    sender.accept(specialistService.buildAppointmentsMessage(telegramId, chatId));
        }
    }
}

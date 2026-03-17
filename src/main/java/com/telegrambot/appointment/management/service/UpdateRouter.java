package com.telegrambot.appointment.management.service;

import com.telegrambot.appointment.management.model.UserRole;
import com.telegrambot.appointment.management.service.staff.MenuService;
import com.telegrambot.appointment.management.service.staff.RegistrationService;
import com.telegrambot.appointment.management.service.staff.StartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.function.Consumer;

/**
 * Роутинг входящих Telegram-обновлений по роли пользователя.
 * Выделен из старого God-класса MainService.
 *
 * Добавить новую роль или команду — добавить case здесь,
 * AppointmentBot трогать не нужно.
 */
@Service
public class UpdateRouter {

    private static final Logger log = LoggerFactory.getLogger(UpdateRouter.class);

    private final UserRoleService userRoleService;
    private final StartService startService;
    private final RegistrationService registrationService;
    private final MenuService menuService;

    public UpdateRouter(UserRoleService userRoleService,
                        StartService startService,
                        RegistrationService registrationService,
                        MenuService menuService) {
        this.userRoleService = userRoleService;
        this.startService = startService;
        this.registrationService = registrationService;
        this.menuService = menuService;
    }

    public void route(Update update, Consumer<SendMessage> sender) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update.getMessage(), sender);
        } else if (update.hasCallbackQuery()) {
            handleCallback(update, sender);
        }
    }

    // ---- Text messages ----

    private void handleMessage(Message message, Consumer<SendMessage> sender) {
        Long telegramId = message.getFrom().getId();
        Long chatId = message.getChatId();
        String text = message.getText();
        String username = message.getFrom().getUserName(); // может быть null

        UserRole role = userRoleService.defineUserRoleByTelegramId(telegramId);
        log.debug("Message: telegramId={}, role={}, text={}", telegramId, role, text);

        switch (role) {
            case NOT_REGISTERED -> {
                if ("/start".equals(text)) {
                    sender.accept(startService.prepareStartMessage(message));
                } else if (registrationService.isRegistering(telegramId)) {
                    SendMessage msg = registrationService.handleMessage(telegramId, chatId, text);
                    if (msg != null) sender.accept(msg);
                } else {
                    sender.accept(new SendMessage(chatId.toString(), "Сначала зарегистрируйтесь"));
                }
            }
            case CLIENT -> {
                if ("/start".equals(text)) {
                    sender.accept(startService.prepareStartMessage(message));
                } else if ("/menu".equals(text)) {
                    sender.accept(menuService.prepareMenuMessage(message));
                }
                // TODO: добавить команды клиента
            }
            case SPECIALIST -> {
                // TODO: команды специалиста
                log.debug("Specialist handler not implemented: telegramId={}", telegramId);
            }
            case MANAGER -> {
                // TODO: команды менеджера
                log.debug("Manager handler not implemented: telegramId={}", telegramId);
            }
        }
    }

    // ---- Callback queries ----

    private void handleCallback(Update update, Consumer<SendMessage> sender) {
        var callback = update.getCallbackQuery();
        String data = callback.getData();
        Long telegramId = callback.getFrom().getId();
        Message message = (Message) callback.getMessage();
        Long chatId = message.getChatId();
        String username = callback.getFrom().getUserName(); // может быть null

        UserRole role = userRoleService.defineUserRoleByTelegramId(telegramId);
        log.debug("Callback: telegramId={}, role={}, data={}", telegramId, role, data);

        switch (role) {
            case NOT_REGISTERED -> {
                switch (data) {
                    case "REGISTER" -> {
                        boolean whitelisted = userRoleService.isManagerWhitelisted(username);
                        log.info("REGISTER: telegramId={}, username={}, whitelisted={}", telegramId, username, whitelisted);
                        SendMessage msg = whitelisted
                                ? registrationService.startManagerRegistration(telegramId, chatId, username)
                                : registrationService.startClientRegistration(telegramId, chatId, username);
                        sender.accept(msg);
                    }
                    case "BACK_TO_FIRSTNAME", "BACK_TO_LASTNAME",
                         "BACK_TO_MANAGER_FIRSTNAME", "BACK_TO_MANAGER_LASTNAME" -> {
                        SendMessage msg = registrationService.handleBackCallback(telegramId, chatId, data);
                        if (msg != null) sender.accept(msg);
                    }
                }
            }
            case CLIENT -> {
                // TODO: callback'и клиента
            }
            case SPECIALIST -> {
                // TODO: callback'и специалиста
            }
            case MANAGER -> {
                // TODO: callback'и менеджера
            }
        }
    }
}

package com.telegrambot.appointment.management.service;

import com.telegrambot.appointment.management.model.UserRole;
import com.telegrambot.appointment.management.service.staff.HelpService;
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


@Service
public class UpdateRouter {

    private static final Logger log = LoggerFactory.getLogger(UpdateRouter.class);

    private final UserRoleService userRoleService;
    private final StartService startService;
    private final RegistrationService registrationService;
    private final MenuService menuService;
    private final HelpService helpService;

    public UpdateRouter(UserRoleService userRoleService,
                        StartService startService,
                        RegistrationService registrationService,
                        MenuService menuService,
                        HelpService helpService) {
        this.userRoleService = userRoleService;
        this.startService = startService;
        this.registrationService = registrationService;
        this.menuService = menuService;
        this.helpService = helpService;
    }

    public void route(Update update, Consumer<SendMessage> sender) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update.getMessage(), sender);
        } else if (update.hasCallbackQuery()) {
            handleCallback(update, sender);
        }
    }


    private void handleMessage(Message message, Consumer<SendMessage> sender) {
        Long telegramId = message.getFrom().getId();
        Long chatId = message.getChatId();
        String text = message.getText();
        String username = message.getFrom().getUserName();

        UserRole role = userRoleService.defineUserRoleByTelegramId(telegramId);
        log.debug("Message: telegramId={}, role={}, text={}", telegramId, role, text);

        switch (role) {
            case NOT_REGISTERED -> {
                switch (text) {
                    case "/start" -> sender.accept(startService.prepareStartMessage(message));
                    case "/help" -> sender.accept(helpService.prepareHelpForUnregistered(chatId));
                    default -> {
                        if (registrationService.isRegistering(telegramId)) {
                            SendMessage msg = registrationService.handleMessage(telegramId, chatId, text);
                            if (msg != null) sender.accept(msg);
                        } else {
                            sender.accept(new SendMessage(chatId.toString(),
                                    "Сначала зарегистрируйтесь. Нажмите /start"));
                        }
                    }
                }
            }
            case CLIENT -> {
                switch (text) {
                    case "/start" -> sender.accept(startService.prepareStartMessage(message));
                    case "/menu" -> sender.accept(menuService.prepareClientMenu(message));
                    case "/help" -> sender.accept(helpService.prepareHelpForClient(chatId));
                    // TODO: /appointments
                    default -> sender.accept(new SendMessage(chatId.toString(),
                            "Неизвестная команда. Используйте /help для списка команд."));
                }
            }
            case SPECIALIST -> {
                switch (text) {
                    case "/start" -> sender.accept(startService.prepareStartMessage(message));
                    case "/menu" -> sender.accept(menuService.prepareSpecialistMenu(message));
                    case "/help" -> sender.accept(helpService.prepareHelpForSpecialist(chatId));
                    // TODO: /schedule, /appointments
                    default -> sender.accept(new SendMessage(chatId.toString(),
                            "Неизвестная команда. Используйте /help для списка команд."));
                }
            }
            case MANAGER -> {
                switch (text) {
                    case "/start" -> sender.accept(startService.prepareStartMessage(message));
                    case "/menu" -> sender.accept(menuService.prepareManagerMenu(message));
                    case "/help" -> sender.accept(helpService.prepareHelpForManager(chatId));
                    // TODO: /specialists, /schedule
                    default -> sender.accept(new SendMessage(chatId.toString(),
                            "Неизвестная команда. Используйте /help для списка команд."));
                }
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
                switch (data) {
                    case "APPOINTMENTS_SCHEDULE" -> {
                        // TODO: запустить флоу записи к специалисту
                        sender.accept(new SendMessage(chatId.toString(), "🚧 В разработке"));
                    }
                    case "APPOINTMENTS_MY" -> {
                        // TODO: показать список записей клиента
                        sender.accept(new SendMessage(chatId.toString(), "🚧 В разработке"));
                    }
                }
            }
            case SPECIALIST -> {
                switch (data) {
                    case "SPECIALIST_SCHEDULE" -> {
                        // TODO: показать расписание специалиста
                        sender.accept(new SendMessage(chatId.toString(), "🚧 В разработке"));
                    }
                    case "SPECIALIST_APPOINTMENTS" -> {
                        // TODO: показать записи специалиста
                        sender.accept(new SendMessage(chatId.toString(), "🚧 В разработке"));
                    }
                }
            }
            case MANAGER -> {
                switch (data) {
                    case "MANAGER_SPECIALISTS" -> {
                        // TODO: управление специалистами
                        sender.accept(new SendMessage(chatId.toString(), "🚧 В разработке"));
                    }
                    case "MANAGER_SCHEDULE" -> {
                        // TODO: расписание всех специалистов
                        sender.accept(new SendMessage(chatId.toString(), "🚧 В разработке"));
                    }
                }
            }
        }
    }
}

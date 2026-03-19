package com.telegrambot.appointment.management.adapter.telegram.router;

import com.telegrambot.appointment.management.adapter.telegram.handler.HelpHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.MenuHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.RegistrationHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.StartHandler;
import com.telegrambot.appointment.management.domain.model.user.UserRole;
import com.telegrambot.appointment.management.domain.service.AppointmentBookingService;
import com.telegrambot.appointment.management.domain.service.UserRoleService;
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
    private final StartHandler startHandler;
    private final RegistrationHandler registrationHandler;
    private final MenuHandler menuHandler;
    private final HelpHandler helpHandler;
    private final AppointmentBookingService bookingService;

    public UpdateRouter(UserRoleService userRoleService,
                        StartHandler startHandler,
                        RegistrationHandler registrationHandler,
                        MenuHandler menuHandler,
                        HelpHandler helpHandler,
                        AppointmentBookingService bookingService) {
        this.userRoleService = userRoleService;
        this.startHandler = startHandler;
        this.registrationHandler = registrationHandler;
        this.menuHandler = menuHandler;
        this.helpHandler = helpHandler;
        this.bookingService = bookingService;
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
                    case "/start" -> sender.accept(startHandler.prepareStartMessage(message));
                    case "/help" -> sender.accept(helpHandler.prepareHelpForUnregistered(chatId));
                    default -> {
                        if (registrationHandler.isRegistering(telegramId)) {
                            SendMessage msg = registrationHandler.handleMessage(telegramId, chatId, text);
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
                    case "/start" -> sender.accept(startHandler.prepareStartMessage(message));
                    case "/menu" -> sender.accept(menuHandler.prepareClientMenu(message));
                    case "/make_appointment" -> sender.accept(bookingService.startBooking(telegramId, chatId));
                    case "/help" -> sender.accept(helpHandler.prepareHelpForClient(chatId));
                    default -> sender.accept(new SendMessage(chatId.toString(),
                            "Неизвестная команда. Используйте /help для списка команд."));
                }
            }
            case SPECIALIST -> {
                switch (text) {
                    case "/start" -> sender.accept(startHandler.prepareStartMessage(message));
                    case "/menu" -> sender.accept(menuHandler.prepareSpecialistMenu(message));
                    case "/help" -> sender.accept(helpHandler.prepareHelpForSpecialist(chatId));
                    default -> sender.accept(new SendMessage(chatId.toString(),
                            "Неизвестная команда. Используйте /help для списка команд."));
                }
            }
            case MANAGER -> {
                switch (text) {
                    case "/start" -> sender.accept(startHandler.prepareStartMessage(message));
                    case "/menu" -> sender.accept(menuHandler.prepareManagerMenu(message));
                    case "/help" -> sender.accept(helpHandler.prepareHelpForManager(chatId));
                    default -> sender.accept(new SendMessage(chatId.toString(),
                            "Неизвестная команда. Используйте /help для списка команд."));
                }
            }
        }
    }

    private void handleCallback(Update update, Consumer<SendMessage> sender) {
        var callback = update.getCallbackQuery();
        String data = callback.getData();
        Long telegramId = callback.getFrom().getId();
        Message message = (Message) callback.getMessage();
        Long chatId = message.getChatId();
        String username = callback.getFrom().getUserName();

        UserRole role = userRoleService.defineUserRoleByTelegramId(telegramId);
        log.debug("Callback: telegramId={}, role={}, data={}", telegramId, role, data);

        switch (role) {
            case NOT_REGISTERED -> {
                switch (data) {
                    case "REGISTER" -> {
                        boolean whitelisted = userRoleService.isManagerWhitelisted(username);
                        log.info("REGISTER: telegramId={}, username={}, whitelisted={}", telegramId, username, whitelisted);
                        SendMessage msg = whitelisted
                                ? registrationHandler.startManagerRegistration(telegramId, chatId, username)
                                : registrationHandler.startClientRegistration(telegramId, chatId, username);
                        sender.accept(msg);
                    }
                    case "BACK_TO_FIRSTNAME", "BACK_TO_LASTNAME",
                         "BACK_TO_MANAGER_FIRSTNAME", "BACK_TO_MANAGER_LASTNAME" -> {
                        SendMessage msg = registrationHandler.handleBackCallback(telegramId, chatId, data);
                        if (msg != null) sender.accept(msg);
                    }
                }
            }
            case CLIENT -> {
                if (data.startsWith("BOOK_") || bookingService.isBooking(telegramId)) {
                    sender.accept(bookingService.handleCallback(telegramId, chatId, data));
                } else {
                    switch (data) {
                        case "APPOINTMENTS_SCHEDULE" -> sender.accept(bookingService.startBooking(telegramId, chatId));
                        case "APPOINTMENTS_MY" -> sender.accept(new SendMessage(chatId.toString(), "🚧 В разработке"));
                    }
                }
            }
            case SPECIALIST -> {
                switch (data) {
                    case "SPECIALIST_SCHEDULE" -> sender.accept(new SendMessage(chatId.toString(), "🚧 В разработке"));
                    case "SPECIALIST_APPOINTMENTS" ->
                            sender.accept(new SendMessage(chatId.toString(), "🚧 В разработке"));
                }
            }
            case MANAGER -> {
                switch (data) {
                    case "MANAGER_SPECIALISTS" -> sender.accept(new SendMessage(chatId.toString(), "🚧 В разработке"));
                    case "MANAGER_SCHEDULE" -> sender.accept(new SendMessage(chatId.toString(), "🚧 В разработке"));
                }
            }
        }
    }
}

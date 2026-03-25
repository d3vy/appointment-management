package com.telegrambot.appointment.management.adapter.telegram.router;

import com.telegrambot.appointment.management.adapter.telegram.handler.HelpHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.MenuHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.RegistrationHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.StartHandler;
import com.telegrambot.appointment.management.domain.model.user.UserRole;
import com.telegrambot.appointment.management.domain.port.TelegramCallbackAcknowledger;
import com.telegrambot.appointment.management.domain.service.AppointmentBookingService;
import com.telegrambot.appointment.management.domain.service.ClientService;
import com.telegrambot.appointment.management.domain.service.ManagerScheduleService;
import com.telegrambot.appointment.management.domain.service.ManagerService;
import com.telegrambot.appointment.management.domain.service.SpecialistService;
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
    private static final String LINK_SPEC_PREFIX = "M_LNK_SP_";
    private static final String LINK_SVC_PREFIX = "M_LNK_SV_";

    private final UserRoleService userRoleService;
    private final StartHandler startHandler;
    private final RegistrationHandler registrationHandler;
    private final MenuHandler menuHandler;
    private final HelpHandler helpHandler;
    private final AppointmentBookingService bookingService;
    private final ManagerService managerService;
    private final SpecialistService specialistService;
    private final ClientService clientService;
    private final ManagerScheduleService managerScheduleService;
    private final TelegramCallbackAcknowledger callbackAcknowledger;

    public UpdateRouter(UserRoleService userRoleService,
                        StartHandler startHandler,
                        RegistrationHandler registrationHandler,
                        MenuHandler menuHandler,
                        HelpHandler helpHandler,
                        AppointmentBookingService bookingService,
                        ManagerService managerService,
                        SpecialistService specialistService,
                        ClientService clientService,
                        ManagerScheduleService managerScheduleService,
                        TelegramCallbackAcknowledger callbackAcknowledger) {
        this.userRoleService = userRoleService;
        this.startHandler = startHandler;
        this.registrationHandler = registrationHandler;
        this.menuHandler = menuHandler;
        this.helpHandler = helpHandler;
        this.bookingService = bookingService;
        this.managerService = managerService;
        this.specialistService = specialistService;
        this.clientService = clientService;
        this.managerScheduleService = managerScheduleService;
        this.callbackAcknowledger = callbackAcknowledger;
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
                    case "/menu" -> sender.accept(menuHandler.prepareClientMenu(message,
                            clientService.isNotificationsEnabled(telegramId)));
                    case "/make_appointment" -> sender.accept(bookingService.startBooking(telegramId, chatId));
                    case "/appointments" ->
                            sender.accept(bookingService.buildClientAppointmentsMessage(telegramId, chatId));
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
                if (managerScheduleService.hasActiveContext(telegramId)) {
                    SendMessage msg = managerScheduleService.handleTextInput(telegramId, chatId, text);
                    if (msg != null) {
                        sender.accept(msg);
                        return;
                    }
                } else if (managerService.hasPendingAction(telegramId)) {
                    SendMessage msg = managerService.handlePendingAction(telegramId, chatId, text);
                    if (msg != null) sender.accept(msg);
                    return;
                }
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
        callbackAcknowledger.acknowledge(callback.getId());
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
                        boolean managerWhitelisted = userRoleService.isManagerWhitelisted(username);
                        boolean specialistWhitelisted = userRoleService.isSpecialistWhitelisted(username);
                        log.info("REGISTER: telegramId={}, username={}, manager={}, specialist={}",
                                telegramId, username, managerWhitelisted, specialistWhitelisted);

                        SendMessage msg;
                        if (managerWhitelisted) {
                            msg = registrationHandler.startManagerRegistration(telegramId, chatId, username);
                        } else if (specialistWhitelisted) {
                            msg = registrationHandler.startSpecialistRegistration(telegramId, chatId, username);
                        } else {
                            msg = registrationHandler.startClientRegistration(telegramId, chatId, username);
                        }

                        sender.accept(msg);
                    }
                    case "BACK_TO_FIRSTNAME", "BACK_TO_LASTNAME",
                         "BACK_TO_MANAGER_FIRSTNAME", "BACK_TO_MANAGER_LASTNAME",
                         "BACK_TO_SPECIALIST_FIRSTNAME", "BACK_TO_SPECIALIST_LASTNAME" -> {
                        SendMessage msg = registrationHandler.handleBackCallback(telegramId, chatId, data);
                        if (msg != null) sender.accept(msg);
                    }
                }
            }
            case CLIENT -> {
                if (data.startsWith("CANCEL_APPT_")) {
                    Integer appointmentId = Integer.parseInt(data.replace("CANCEL_APPT_", ""));
                    sender.accept(bookingService.cancelAppointment(telegramId, appointmentId, chatId));
                } else if (data.startsWith("BOOK_") || bookingService.isBooking(telegramId)) {
                    sender.accept(bookingService.handleCallback(telegramId, chatId, data));
                } else {
                    switch (data) {
                        case "APPOINTMENTS_SCHEDULE" -> sender.accept(bookingService.startBooking(telegramId, chatId));
                        case "APPOINTMENTS_MY" ->
                                sender.accept(bookingService.buildClientAppointmentsMessage(telegramId, chatId));
                        case "TOGGLE_NOTIFICATIONS" -> {
                            boolean enabled = clientService.toggleNotifications(telegramId);
                            sender.accept(menuHandler.prepareClientMenu(message, enabled));
                        }
                    }
                }

            }
            case SPECIALIST -> {
                switch (data) {
                    case "ADD_SPECIALIST_TO_WHITELIST" -> sender.accept(new SendMessage(chatId.toString(),
                            "Добавлять специалистов в whitelist может только менеджер. Попросите менеджера добавить вас через меню «Добавить специалиста»."));
                    case "SPECIALIST_SCHEDULE" ->
                            sender.accept(specialistService.buildScheduleMessage(telegramId, chatId));
                    case "SPECIALIST_APPOINTMENTS" ->
                            sender.accept(specialistService.buildAppointmentsMessage(telegramId, chatId));
                }
            }
            case MANAGER -> {
                if (data.startsWith("SCHED_")) {
                    if (data.startsWith("SCHED_DEL_DAY_")) {
                        int scheduleId = Integer.parseInt(data.replace("SCHED_DEL_DAY_", ""));
                        sender.accept(managerScheduleService.deleteDayWithCheck(telegramId, chatId, scheduleId));
                    } else if (data.startsWith("SCHED_DEL_CONFIRM_")) {
                        int scheduleId = Integer.parseInt(data.replace("SCHED_DEL_CONFIRM_", ""));
                        sender.accept(managerScheduleService.confirmDeleteDay(chatId, scheduleId));
                    } else if (data.startsWith("SCHED_DAY_")) {
                        int scheduleId = Integer.parseInt(data.replace("SCHED_DAY_", ""));
                        sender.accept(managerScheduleService.buildDayDetailMessage(chatId, scheduleId));
                    } else if (data.startsWith("SCHED_BACK_TO_DAY_")) {
                        int specialistId = Integer.parseInt(data.replace("SCHED_BACK_TO_DAY_", ""));
                        sender.accept(managerScheduleService.buildSpecialistScheduleMessage(chatId, specialistId));
                    } else if (data.startsWith("SCHED_BACK_TO_SPECIALISTS")) {
                        sender.accept(managerScheduleService.startScheduleFlow(telegramId, chatId));
                    } else if (data.startsWith("SCHED_ADD_")) {
                        int specialistId = Integer.parseInt(data.replace("SCHED_ADD_", ""));
                        sender.accept(managerScheduleService.startAddDaysFlow(telegramId, chatId, specialistId));
                    } else {
                        sender.accept(managerScheduleService.handleCallback(telegramId, chatId, data));
                    }
                    return;
                }
                if (data.startsWith(LINK_SPEC_PREFIX)) {
                    int specialistId = Integer.parseInt(data.substring(LINK_SPEC_PREFIX.length()));
                    sender.accept(managerService.handleLinkPickSpecialist(chatId, specialistId));
                    return;
                }
                if (data.startsWith(LINK_SVC_PREFIX)) {
                    String payload = data.substring(LINK_SVC_PREFIX.length());
                    int underscore = payload.indexOf('_');
                    int specialistId = Integer.parseInt(payload.substring(0, underscore));
                    int serviceId = Integer.parseInt(payload.substring(underscore + 1));
                    sender.accept(managerService.confirmLinkService(chatId, specialistId, serviceId));
                    return;
                }
                switch (data) {
                    case "MANAGER_ADD_SPECIALIST" ->
                            sender.accept(managerService.startAddSpecialistToWhitelist(telegramId, chatId));
                    case "MANAGER_ADD_SERVICE" -> sender.accept(managerService.startAddService(telegramId, chatId));
                    case "MANAGER_LINK_SERVICE" -> sender.accept(managerService.startLinkServiceFlow(chatId));
                    case "MANAGER_SPECIALISTS" -> sender.accept(managerService.buildSpecialistListMessage(chatId));
                    case "MANAGER_SCHEDULE" -> sender.accept(managerScheduleService.startScheduleFlow(telegramId, chatId));
                }
            }
        }
    }
}

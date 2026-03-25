package com.telegrambot.appointment.management.adapter.telegram.role;

import com.telegrambot.appointment.management.adapter.telegram.handler.HelpHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.MenuHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.StartHandler;
import com.telegrambot.appointment.management.domain.model.user.UserRole;
import com.telegrambot.appointment.management.domain.service.AppointmentBookingService;
import com.telegrambot.appointment.management.domain.service.ClientService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.function.Consumer;

@Component
public class ClientRoleHandler implements TelegramRoleHandler {

    private final StartHandler startHandler;
    private final MenuHandler menuHandler;
    private final HelpHandler helpHandler;
    private final AppointmentBookingService bookingService;
    private final ClientService clientService;

    public ClientRoleHandler(StartHandler startHandler,
                             MenuHandler menuHandler,
                             HelpHandler helpHandler,
                             AppointmentBookingService bookingService,
                             ClientService clientService) {
        this.startHandler = startHandler;
        this.menuHandler = menuHandler;
        this.helpHandler = helpHandler;
        this.bookingService = bookingService;
        this.clientService = clientService;
    }

    @Override
    public UserRole role() {
        return UserRole.CLIENT;
    }

    @Override
    public void handleMessage(Message message, Consumer<SendMessage> sender) {
        Long telegramId = message.getFrom().getId();
        Long chatId = message.getChatId();
        String text = message.getText();

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

    @Override
    public void handleCallback(CallbackQuery callback, Consumer<SendMessage> sender) {
        String data = callback.getData();
        Long telegramId = callback.getFrom().getId();
        Message message = (Message) callback.getMessage();
        Long chatId = message.getChatId();

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
}

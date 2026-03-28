package com.telegrambot.appointment.management.adapter.telegram.role;

import com.telegrambot.appointment.management.adapter.telegram.TelegramReply;
import com.telegrambot.appointment.management.adapter.telegram.handler.HelpHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.MenuHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.StartHandler;
import com.telegrambot.appointment.management.domain.model.user.UserRole;
import com.telegrambot.appointment.management.domain.service.AppointmentBookingService;
import com.telegrambot.appointment.management.domain.service.ClientService;
import com.telegrambot.appointment.management.infrastructure.service.TelegramMessageAnchorService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
public class ClientRoleHandler implements TelegramRoleHandler {

    private final StartHandler startHandler;
    private final MenuHandler menuHandler;
    private final HelpHandler helpHandler;
    private final AppointmentBookingService bookingService;
    private final ClientService clientService;
    private final TelegramMessageAnchorService anchorService;

    public ClientRoleHandler(StartHandler startHandler,
                             MenuHandler menuHandler,
                             HelpHandler helpHandler,
                             AppointmentBookingService bookingService,
                             ClientService clientService,
                             TelegramMessageAnchorService anchorService) {
        this.startHandler = startHandler;
        this.menuHandler = menuHandler;
        this.helpHandler = helpHandler;
        this.bookingService = bookingService;
        this.clientService = clientService;
        this.anchorService = anchorService;
    }

    @Override
    public UserRole role() {
        return UserRole.CLIENT;
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
            case "/menu" -> sendWithOptionalEdit(telegramId,
                    menuHandler.prepareClientMenu(chatId, clientService.isNotificationsEnabled(telegramId)), reply);
            case "/make_appointment" -> sendWithOptionalEdit(telegramId, bookingService.startBooking(telegramId, chatId), reply);
            case "/appointments" -> sendWithOptionalEdit(telegramId,
                    bookingService.buildClientAppointmentsMessage(telegramId, chatId), reply);
            case "/help" -> sendWithOptionalEdit(telegramId, helpHandler.prepareHelpForClient(chatId), reply);
            default -> sendWithOptionalEdit(telegramId, new SendMessage(chatId.toString(),
                    "Неизвестная команда. Используйте /help для списка команд."), reply);
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

        if ("CLIENT_MAIN_MENU".equals(data)) {
            boolean enabled = clientService.isNotificationsEnabled(telegramId);
            reply.sendOrEdit(menuHandler.prepareClientMenu(chatId, enabled), messageId);
            return;
        }
        if (data.startsWith("CANCEL_APPT_")) {
            Integer appointmentId = Integer.parseInt(data.replace("CANCEL_APPT_", ""));
            reply.sendOrEdit(bookingService.cancelAppointment(telegramId, appointmentId, chatId), messageId);
            return;
        }
        if (data.startsWith("BOOK_") || bookingService.isBooking(telegramId)) {
            reply.sendOrEdit(bookingService.handleCallback(telegramId, chatId, data), messageId);
            return;
        }
        switch (data) {
            case "APPOINTMENTS_SCHEDULE" ->
                    reply.sendOrEdit(bookingService.startBooking(telegramId, chatId), messageId);
            case "APPOINTMENTS_MY" ->
                    reply.sendOrEdit(bookingService.buildClientAppointmentsMessage(telegramId, chatId), messageId);
            case "TOGGLE_NOTIFICATIONS" -> {
                boolean enabled = clientService.toggleNotifications(telegramId);
                reply.sendOrEdit(menuHandler.prepareClientMenu(chatId, enabled), messageId);
            }
        }
    }

    private void sendWithOptionalEdit(Long telegramId, SendMessage outgoing, TelegramReply reply) {
        anchorService.currentMessageId(telegramId).ifPresentOrElse(
                messageId -> reply.sendOrEdit(outgoing, messageId),
                () -> reply.send(outgoing));
    }
}

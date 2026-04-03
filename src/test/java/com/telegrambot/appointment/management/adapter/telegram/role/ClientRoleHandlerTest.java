package com.telegrambot.appointment.management.adapter.telegram.role;

import com.telegrambot.appointment.management.adapter.telegram.TelegramReply;
import com.telegrambot.appointment.management.adapter.telegram.handler.HelpHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.MenuHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.StartHandler;
import com.telegrambot.appointment.management.domain.model.user.UserRole;
import com.telegrambot.appointment.management.domain.service.AppointmentBookingService;
import com.telegrambot.appointment.management.domain.service.ClientService;
import com.telegrambot.appointment.management.infrastructure.service.TelegramMessageAnchorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientRoleHandlerTest {

    @Mock
    private StartHandler startHandler;
    @Mock
    private MenuHandler menuHandler;
    @Mock
    private HelpHandler helpHandler;
    @Mock
    private AppointmentBookingService bookingService;
    @Mock
    private ClientService clientService;
    @Mock
    private TelegramMessageAnchorService anchorService;

    @Test
    void roleIsClient() {
        ClientRoleHandler handler = new ClientRoleHandler(
                startHandler, menuHandler, helpHandler, bookingService, clientService, anchorService);
        assertEquals(UserRole.CLIENT, handler.role());
    }

    @Test
    void startCommandDelegatesToStartHandler() {
        ClientRoleHandler handler = new ClientRoleHandler(
                startHandler, menuHandler, helpHandler, bookingService, clientService, anchorService);
        Message message = buildTextMessage(1L, 10L, "/start");
        SendMessage prepared = new SendMessage("10", "welcome");
        when(startHandler.prepareStartMessage(message)).thenReturn(prepared);

        List<BotApiMethod<?>> executed = new ArrayList<>();
        TelegramReply reply = new TelegramReply(executed::add);
        handler.handleMessage(message, reply);

        assertEquals(1, executed.size());
        assertEquals(prepared, executed.get(0));
    }

    @Test
    void makeAppointmentDelegatesToBookingService() {
        ClientRoleHandler handler = new ClientRoleHandler(
                startHandler, menuHandler, helpHandler, bookingService, clientService, anchorService);
        Message message = buildTextMessage(2L, 20L, "/make_appointment");
        SendMessage bookingMsg = new SendMessage("20", "booking");
        when(bookingService.startBooking(2L, 20L)).thenReturn(bookingMsg);
        when(anchorService.currentMessageId(any())).thenReturn(Optional.empty());

        List<BotApiMethod<?>> executed = new ArrayList<>();
        TelegramReply reply = new TelegramReply(executed::add);
        handler.handleMessage(message, reply);

        assertEquals(1, executed.size());
        assertEquals(bookingMsg, executed.get(0));
        verify(bookingService).startBooking(2L, 20L);
    }

    @Test
    void appointmentsScheduleCallbackUsesStartBookingEvenWhenBookingContextExists() {
        ClientRoleHandler handler = new ClientRoleHandler(
                startHandler, menuHandler, helpHandler, bookingService, clientService, anchorService);
        CallbackQuery callback = buildCallback(5L, 50L, 7, "APPOINTMENTS_SCHEDULE");
        SendMessage bookingMsg = new SendMessage("50", "choose path");
        when(bookingService.startBooking(5L, 50L)).thenReturn(bookingMsg);

        List<BotApiMethod<?>> executed = new ArrayList<>();
        TelegramReply reply = new TelegramReply(executed::add);
        handler.handleCallback(callback, reply);

        verify(bookingService).startBooking(5L, 50L);
        verify(bookingService, never()).handleCallback(anyLong(), anyLong(), any());
        assertEquals(1, executed.size());
        assertInstanceOf(EditMessageText.class, executed.get(0));
        EditMessageText edit = (EditMessageText) executed.get(0);
        assertEquals(7, edit.getMessageId());
        assertEquals(bookingMsg.getText(), edit.getText());
    }

    @Test
    void menuCommandClearsBookingContext() {
        ClientRoleHandler handler = new ClientRoleHandler(
                startHandler, menuHandler, helpHandler, bookingService, clientService, anchorService);
        Message message = buildTextMessage(3L, 30L, "/menu");
        SendMessage menuMsg = new SendMessage("30", "menu");
        when(clientService.isNotificationsEnabled(3L)).thenReturn(false);
        when(menuHandler.prepareClientMenu(30L, false)).thenReturn(menuMsg);

        List<BotApiMethod<?>> executed = new ArrayList<>();
        TelegramReply reply = new TelegramReply(executed::add);
        handler.handleMessage(message, reply);

        verify(bookingService).clearBookingContextIfPresent(3L);
        verify(anchorService).forget(3L);
        verify(anchorService, never()).currentMessageId(eq(3L));
        assertEquals(1, executed.size());
        assertEquals(menuMsg, executed.get(0));
    }

    @Test
    void mainMenuCallbackClearsBookingContext() {
        ClientRoleHandler handler = new ClientRoleHandler(
                startHandler, menuHandler, helpHandler, bookingService, clientService, anchorService);
        CallbackQuery callback = buildCallback(8L, 80L, 3, "CLIENT_MAIN_MENU");
        SendMessage menuMsg = new SendMessage("80", "client menu");
        when(clientService.isNotificationsEnabled(8L)).thenReturn(true);
        when(menuHandler.prepareClientMenu(80L, true)).thenReturn(menuMsg);

        List<BotApiMethod<?>> executed = new ArrayList<>();
        TelegramReply reply = new TelegramReply(executed::add);
        handler.handleCallback(callback, reply);

        verify(bookingService).clearBookingContextIfPresent(8L);
        verify(bookingService, never()).handleCallback(anyLong(), anyLong(), any());
    }

    private static CallbackQuery buildCallback(Long telegramId, Long chatId, int messageId, String data) {
        User user = new User();
        user.setId(telegramId);
        Chat chat = new Chat();
        chat.setId(chatId);
        Message message = new Message();
        message.setMessageId(messageId);
        message.setChat(chat);
        CallbackQuery callback = new CallbackQuery();
        callback.setFrom(user);
        callback.setMessage(message);
        callback.setData(data);
        return callback;
    }

    private static Message buildTextMessage(Long telegramId, Long chatId, String text) {
        User user = new User();
        user.setId(telegramId);
        Chat chat = new Chat();
        chat.setId(chatId);
        Message message = new Message();
        message.setFrom(user);
        message.setChat(chat);
        message.setText(text);
        return message;
    }
}

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
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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

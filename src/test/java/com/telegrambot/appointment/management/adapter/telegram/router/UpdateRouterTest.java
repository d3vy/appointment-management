package com.telegrambot.appointment.management.adapter.telegram.router;

import com.telegrambot.appointment.management.adapter.telegram.role.TelegramRoleHandler;
import com.telegrambot.appointment.management.domain.model.user.UserRole;
import com.telegrambot.appointment.management.domain.port.TelegramCallbackAcknowledger;
import com.telegrambot.appointment.management.domain.service.UserRoleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.EnumMap;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateRouterTest {

    @Mock
    private UserRoleService userRoleService;
    @Mock
    private TelegramCallbackAcknowledger callbackAcknowledger;
    @Mock
    private TelegramRoleHandler notRegisteredHandler;
    @Mock
    private TelegramRoleHandler clientHandler;
    @Mock
    private TelegramRoleHandler specialistHandler;
    @Mock
    private TelegramRoleHandler managerHandler;
    @Mock
    private Consumer<SendMessage> sender;

    @Test
    void routeForUnregisteredStartCommandDelegatesToNotRegisteredHandler() {
        UpdateRouter updateRouter = buildUpdateRouter();
        Update update = buildMessageUpdate(1001L, 7001L, "ilia_username", "Ilia", "/start");
        when(userRoleService.defineUserRoleByTelegramId(1001L)).thenReturn(UserRole.NOT_REGISTERED);

        updateRouter.route(update, sender);

        verify(notRegisteredHandler).handleMessage(any(Message.class), eq(sender));
    }

    @Test
    void routeForClientUnknownCommandDelegatesToClientHandler() {
        UpdateRouter updateRouter = buildUpdateRouter();
        Update update = buildMessageUpdate(2002L, 8002L, "client_username", "Client", "/unknown");
        when(userRoleService.defineUserRoleByTelegramId(2002L)).thenReturn(UserRole.CLIENT);

        updateRouter.route(update, sender);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(clientHandler).handleMessage(messageCaptor.capture(), eq(sender));
        assertEquals("8002", messageCaptor.getValue().getChatId().toString());
        assertEquals("/unknown", messageCaptor.getValue().getText());
    }

    @Test
    void routeForCallbackAcksAndDelegatesToHandler() {
        UpdateRouter updateRouter = buildUpdateRouter();
        when(userRoleService.defineUserRoleByTelegramId(3003L)).thenReturn(UserRole.CLIENT);

        var update = new Update();
        var cq = new org.telegram.telegrambots.meta.api.objects.CallbackQuery();
        cq.setId("cq1");
        cq.setData("APPOINTMENTS_MY");
        var user = new User();
        user.setId(3003L);
        cq.setFrom(user);
        var chat = new Chat();
        chat.setId(9009L);
        var msg = new Message();
        msg.setChat(chat);
        msg.setMessageId(1);
        cq.setMessage(msg);
        update.setCallbackQuery(cq);

        updateRouter.route(update, sender);

        verify(callbackAcknowledger).acknowledge("cq1");
        verify(clientHandler).handleCallback(eq(cq), eq(sender));
    }

    private UpdateRouter buildUpdateRouter() {
        EnumMap<UserRole, TelegramRoleHandler> map = new EnumMap<>(UserRole.class);
        map.put(UserRole.NOT_REGISTERED, notRegisteredHandler);
        map.put(UserRole.CLIENT, clientHandler);
        map.put(UserRole.SPECIALIST, specialistHandler);
        map.put(UserRole.MANAGER, managerHandler);
        return new UpdateRouter(userRoleService, callbackAcknowledger, map);
    }

    private Update buildMessageUpdate(Long telegramId, Long chatId, String username, String firstName, String text) {
        User user = new User();
        user.setId(telegramId);
        user.setUserName(username);
        user.setFirstName(firstName);

        Chat chat = new Chat();
        chat.setId(chatId);

        Message message = new Message();
        message.setFrom(user);
        message.setChat(chat);
        message.setText(text);

        Update update = new Update();
        update.setMessage(message);
        return update;
    }
}

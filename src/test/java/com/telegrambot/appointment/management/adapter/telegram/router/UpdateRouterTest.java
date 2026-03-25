package com.telegrambot.appointment.management.adapter.telegram.router;

import com.telegrambot.appointment.management.adapter.telegram.handler.HelpHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.MenuHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.RegistrationHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.StartHandler;
import com.telegrambot.appointment.management.domain.model.user.UserRole;
import com.telegrambot.appointment.management.domain.service.AppointmentBookingService;
import com.telegrambot.appointment.management.domain.service.ClientService;
import com.telegrambot.appointment.management.domain.service.ManagerScheduleService;
import com.telegrambot.appointment.management.domain.service.ManagerService;
import com.telegrambot.appointment.management.domain.service.SpecialistService;
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

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateRouterTest {

    @Mock
    private UserRoleService userRoleService;
    @Mock
    private StartHandler startHandler;
    @Mock
    private RegistrationHandler registrationHandler;
    @Mock
    private MenuHandler menuHandler;
    @Mock
    private HelpHandler helpHandler;
    @Mock
    private AppointmentBookingService bookingService;
    @Mock
    private ManagerService managerService;
    @Mock
    private SpecialistService specialistService;
    @Mock
    private ClientService clientService;
    @Mock
    private ManagerScheduleService managerScheduleService;
    @Mock
    private Consumer<SendMessage> sender;

    @Test
    void routeForUnregisteredStartCommandUsesStartHandlerResult() {
        UpdateRouter updateRouter = buildUpdateRouter();
        Update update = buildMessageUpdate(1001L, 7001L, "ilia_username", "Ilia", "/start");
        SendMessage startMessage = new SendMessage("7001", "welcome");
        when(userRoleService.defineUserRoleByTelegramId(1001L)).thenReturn(UserRole.NOT_REGISTERED);
        when(startHandler.prepareStartMessage(any(Message.class))).thenReturn(startMessage);

        updateRouter.route(update, sender);

        verify(startHandler).prepareStartMessage(any(Message.class));
        verify(sender).accept(startMessage);
    }

    @Test
    void routeForClientUnknownCommandReturnsDefaultHint() {
        UpdateRouter updateRouter = buildUpdateRouter();
        Update update = buildMessageUpdate(2002L, 8002L, "client_username", "Client", "/unknown");
        when(userRoleService.defineUserRoleByTelegramId(2002L)).thenReturn(UserRole.CLIENT);

        updateRouter.route(update, sender);

        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(sender).accept(messageCaptor.capture());
        assertEquals("8002", messageCaptor.getValue().getChatId());
        assertTrue(messageCaptor.getValue().getText().contains("Неизвестная команда"));
        assertTrue(messageCaptor.getValue().getText().contains("/help"));
    }

    private UpdateRouter buildUpdateRouter() {
        return new UpdateRouter(
                userRoleService,
                startHandler,
                registrationHandler,
                menuHandler,
                helpHandler,
                bookingService,
                managerService,
                specialistService,
                clientService,
                managerScheduleService
        );
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

package com.telegrambot.appointment.management.adapter.telegram;

import com.telegrambot.appointment.management.adapter.telegram.TelegramReply;
import com.telegrambot.appointment.management.adapter.telegram.router.UpdateRouter;
import com.telegrambot.appointment.management.infrastructure.config.BotConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentBotTest {

    @Mock
    private BotConfig botConfig;

    @Mock
    private UpdateRouter updateRouter;

    @Test
    void getBotUsernameReturnsValueFromConfig() {
        when(botConfig.getBotName()).thenReturn("appointment_test_bot");
        AppointmentBot appointmentBot = new AppointmentBot(botConfig, updateRouter);

        assertEquals("appointment_test_bot", appointmentBot.getBotUsername());
    }

    @Test
    void getBotTokenReturnsValueFromConfig() {
        when(botConfig.getToken()).thenReturn("test-token");
        AppointmentBot appointmentBot = new AppointmentBot(botConfig, updateRouter);

        assertEquals("test-token", appointmentBot.getBotToken());
    }

    @Test
    void onUpdateReceivedDelegatesToRouter() {
        AppointmentBot appointmentBot = new AppointmentBot(botConfig, updateRouter);
        Update update = new Update();

        appointmentBot.onUpdateReceived(update);

        verify(updateRouter).route(any(Update.class), any(TelegramReply.class));
    }

    @Test
    void onUpdateReceivedHandlesRouterException() {
        AppointmentBot appointmentBot = new AppointmentBot(botConfig, updateRouter);
        Update update = new Update();
        doThrow(new RuntimeException("router failed")).when(updateRouter).route(any(Update.class), any(TelegramReply.class));

        assertDoesNotThrow(() -> appointmentBot.onUpdateReceived(update));
        verify(updateRouter).route(any(Update.class), any(TelegramReply.class));
    }
}

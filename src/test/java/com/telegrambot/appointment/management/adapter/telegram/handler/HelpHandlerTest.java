package com.telegrambot.appointment.management.adapter.telegram.handler;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelpHandlerTest {

    private final HelpHandler helpHandler = new HelpHandler();

    @Test
    void prepareHelpForUnregisteredContainsStartCommandHint() {
        SendMessage result = helpHandler.prepareHelpForUnregistered(111L);

        assertEquals("111", result.getChatId());
        assertTrue(result.getText().contains("/start"));
        assertTrue(result.getText().contains("/help"));
    }

    @Test
    void prepareHelpForClientContainsMainClientCommands() {
        SendMessage result = helpHandler.prepareHelpForClient(222L);

        assertEquals("222", result.getChatId());
        assertTrue(result.getText().contains("/menu"));
        assertTrue(result.getText().contains("/make_appointment"));
        assertTrue(result.getText().contains("/appointments"));
        assertTrue(result.getText().contains("/help"));
    }

    @Test
    void prepareHelpForManagerContainsManagerCommands() {
        SendMessage result = helpHandler.prepareHelpForManager(333L);

        assertEquals("333", result.getChatId());
        assertTrue(result.getText().contains("/menu"));
        assertTrue(result.getText().contains("/help"));
    }

    @Test
    void prepareHelpForSpecialistContainsSpecialistCommands() {
        SendMessage result = helpHandler.prepareHelpForSpecialist(444L);

        assertEquals("444", result.getChatId());
        assertTrue(result.getText().contains("/menu"));
        assertTrue(result.getText().contains("/help"));
    }
}


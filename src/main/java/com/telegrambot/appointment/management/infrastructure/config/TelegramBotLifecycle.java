package com.telegrambot.appointment.management.infrastructure.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class TelegramBotLifecycle {

    public enum RegistrationState {
        NOT_ATTEMPTED,
        REGISTERED,
        FAILED
    }

    private final AtomicReference<RegistrationState> registrationState =
            new AtomicReference<>(RegistrationState.NOT_ATTEMPTED);

    public RegistrationState getRegistrationState() {
        return registrationState.get();
    }

    public void markRegistered() {
        registrationState.set(RegistrationState.REGISTERED);
    }

    public void markFailed() {
        registrationState.set(RegistrationState.FAILED);
    }
}

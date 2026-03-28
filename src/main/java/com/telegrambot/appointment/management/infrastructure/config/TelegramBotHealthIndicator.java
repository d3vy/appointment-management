package com.telegrambot.appointment.management.infrastructure.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class TelegramBotHealthIndicator implements HealthIndicator {

    private final Environment environment;
    private final TelegramBotLifecycle telegramBotLifecycle;

    public TelegramBotHealthIndicator(Environment environment, TelegramBotLifecycle telegramBotLifecycle) {
        this.environment = environment;
        this.telegramBotLifecycle = telegramBotLifecycle;
    }

    @Override
    public Health health() {
        boolean telegramEnabled = environment.getProperty("telegram.enabled", Boolean.class, true);
        if (!telegramEnabled) {
            return Health.up().withDetail("telegram", "disabled").build();
        }

        return switch (telegramBotLifecycle.getRegistrationState()) {
            case NOT_ATTEMPTED -> Health.up().withDetail("telegram", "starting").build();
            case REGISTERED -> Health.up().withDetail("telegram", "registered").build();
            case FAILED -> Health.down().withDetail("telegram", "bot registration failed").build();
        };
    }
}

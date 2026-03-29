package com.telegrambot.appointment.management.infrastructure.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionEnvironmentValidator {

    private final String redisPassword;
    private final String datasourcePassword;
    private final String botName;
    private final String botToken;

    public ProductionEnvironmentValidator(
            @Value("${spring.data.redis.password:}") String redisPassword,
            @Value("${spring.datasource.password:}") String datasourcePassword,
            @Value("${bot.name:}") String botName,
            @Value("${bot.token:}") String botToken) {
        this.redisPassword = redisPassword;
        this.datasourcePassword = datasourcePassword;
        this.botName = botName;
        this.botToken = botToken;
    }

    @PostConstruct
    void requireStrongSecrets() {
        if (redisPassword == null || redisPassword.isBlank()) {
            throw new IllegalStateException(
                    "Production requires non-empty spring.data.redis.password (set REDIS_PASSWORD).");
        }
        if (datasourcePassword == null || datasourcePassword.isBlank()) {
            throw new IllegalStateException(
                    "Production requires non-empty spring.datasource.password (set DB_PASSWORD).");
        }
        if (botName == null || botName.isBlank()) {
            throw new IllegalStateException(
                    "Production requires non-empty bot.name (set BOT_NAME).");
        }
        if (botToken == null || botToken.isBlank()) {
            throw new IllegalStateException(
                    "Production requires non-empty bot.token (set BOT_TOKEN).");
        }
    }
}

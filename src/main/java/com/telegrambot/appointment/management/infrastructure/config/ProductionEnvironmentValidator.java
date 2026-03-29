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

    public ProductionEnvironmentValidator(
            @Value("${spring.data.redis.password:}") String redisPassword,
            @Value("${spring.datasource.password:}") String datasourcePassword) {
        this.redisPassword = redisPassword;
        this.datasourcePassword = datasourcePassword;
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
    }
}

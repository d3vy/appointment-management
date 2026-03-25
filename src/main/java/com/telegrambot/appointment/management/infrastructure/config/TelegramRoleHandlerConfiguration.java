package com.telegrambot.appointment.management.infrastructure.config;

import com.telegrambot.appointment.management.adapter.telegram.role.TelegramRoleHandler;
import com.telegrambot.appointment.management.domain.model.user.UserRole;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.List;

@Configuration
public class TelegramRoleHandlerConfiguration {

    @Bean
    public EnumMap<UserRole, TelegramRoleHandler> telegramRoleHandlers(List<TelegramRoleHandler> handlers) {
        EnumMap<UserRole, TelegramRoleHandler> map = new EnumMap<>(UserRole.class);
        for (TelegramRoleHandler handler : handlers) {
            UserRole role = handler.role();
            if (map.containsKey(role)) {
                throw new IllegalStateException(
                        "Duplicate TelegramRoleHandler bean for role " + role + ": "
                                + map.get(role).getClass().getName() + " and " + handler.getClass().getName());
            }
            map.put(role, handler);
        }
        for (UserRole role : UserRole.values()) {
            if (!map.containsKey(role)) {
                throw new IllegalStateException("Missing TelegramRoleHandler for role " + role);
            }
        }
        return map;
    }
}

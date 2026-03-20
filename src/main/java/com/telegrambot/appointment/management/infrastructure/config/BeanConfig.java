package com.telegrambot.appointment.management.infrastructure.config;

import com.telegrambot.appointment.management.adapter.telegram.AppointmentBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.function.Consumer;

@Configuration
public class BeanConfig {
    @Bean
    public Consumer<SendMessage> messageSender(AppointmentBot bot) {
        return bot::sendMessage;
    }
}

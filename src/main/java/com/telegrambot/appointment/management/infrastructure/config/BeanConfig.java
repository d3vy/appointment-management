package com.telegrambot.appointment.management.infrastructure.config;

import com.telegrambot.appointment.management.adapter.telegram.AppointmentBot;
import com.telegrambot.appointment.management.domain.port.TelegramTextMessageSender;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Configuration
public class BeanConfig {
    @Bean
    public TelegramTextMessageSender messageSender(@Lazy AppointmentBot bot) {
        return new TelegramTextMessageSender() {
            @Override
            public void sendText(Long chatId, String text) {
                bot.sendMessage(new SendMessage(chatId.toString(), text));
            }
        };
    }
}

package com.telegrambot.appointment.management.infrastructure.config;

import com.telegrambot.appointment.management.adapter.telegram.AppointmentBot;
import com.telegrambot.appointment.management.domain.port.TelegramCallbackAcknowledger;
import com.telegrambot.appointment.management.domain.port.TelegramTextMessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Configuration
public class BeanConfig {

    private static final Logger log = LoggerFactory.getLogger(BeanConfig.class);

    @Bean
    public TelegramTextMessageSender messageSender(@Lazy AppointmentBot bot) {
        return new TelegramTextMessageSender() {
            @Override
            public void sendText(Long chatId, String text) {
                bot.sendMessage(new SendMessage(chatId.toString(), text));
            }
        };
    }

    @Bean
    public TelegramCallbackAcknowledger callbackAcknowledger(@Lazy AppointmentBot bot) {
        return callbackQueryId -> {
            if (callbackQueryId == null || callbackQueryId.isBlank()) {
                return;
            }
            try {
                bot.execute(AnswerCallbackQuery.builder().callbackQueryId(callbackQueryId).build());
            } catch (TelegramApiException e) {
                log.debug("Callback ack failed: {}", e.getMessage());
            }
        };
    }
}

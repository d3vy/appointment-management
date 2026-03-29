package com.telegrambot.appointment.management.domain.exception;

public class TelegramDeliveryException extends RuntimeException {

    public TelegramDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}

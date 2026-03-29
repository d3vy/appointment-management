package com.telegrambot.appointment.management.infrastructure.telegram;

import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;

public final class TelegramDisplayHtml {

    private TelegramDisplayHtml() {}

    public static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        return HtmlUtils.htmlEscape(raw, StandardCharsets.UTF_8.name());
    }
}

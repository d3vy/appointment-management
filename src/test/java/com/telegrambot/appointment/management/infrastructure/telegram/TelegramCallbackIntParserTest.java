package com.telegrambot.appointment.management.infrastructure.telegram;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelegramCallbackIntParserTest {

    @Test
    void parsePositiveIntSuffix_acceptsDigits() {
        assertEquals(Optional.of(42), TelegramCallbackIntParser.parsePositiveIntSuffix("BOOK_DAY_42", "BOOK_DAY_"));
    }

    @Test
    void parsePositiveIntSuffix_rejectsNonNumeric() {
        assertTrue(TelegramCallbackIntParser.parsePositiveIntSuffix("BOOK_DAY_x", "BOOK_DAY_").isEmpty());
    }

    @Test
    void parsePositiveIntSuffix_rejectsZero() {
        assertTrue(TelegramCallbackIntParser.parsePositiveIntSuffix("BOOK_DAY_0", "BOOK_DAY_").isEmpty());
    }

    @Test
    void parseTwoPositiveIdsUnderscore_splitsPayload() {
        var pair = TelegramCallbackIntParser.parseTwoPositiveIdsUnderscore("3_9");
        assertTrue(pair.isPresent());
        assertEquals(3, pair.get().first());
        assertEquals(9, pair.get().second());
    }

    @Test
    void parseTwoPositiveIdsUnderscore_rejectsMalformed() {
        assertTrue(TelegramCallbackIntParser.parseTwoPositiveIdsUnderscore("3").isEmpty());
        assertTrue(TelegramCallbackIntParser.parseTwoPositiveIdsUnderscore("_3").isEmpty());
    }
}

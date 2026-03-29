package com.telegrambot.appointment.management.infrastructure.telegram;

import java.util.Optional;

public final class TelegramCallbackIntParser {

    private static final int MAX_SUFFIX_LEN = 12;

    private TelegramCallbackIntParser() {}

    public static Optional<Integer> parsePositiveIntSuffix(String data, String prefix) {
        if (data == null || prefix == null || !data.startsWith(prefix)) {
            return Optional.empty();
        }
        String suffix = data.substring(prefix.length());
        if (suffix.isEmpty() || suffix.length() > MAX_SUFFIX_LEN) {
            return Optional.empty();
        }
        for (int i = 0; i < suffix.length(); i++) {
            if (!Character.isDigit(suffix.charAt(i))) {
                return Optional.empty();
            }
        }
        try {
            int value = Integer.parseInt(suffix);
            return value > 0 ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static Optional<IntPair> parseTwoPositiveIdsUnderscore(String payload) {
        if (payload == null) {
            return Optional.empty();
        }
        int underscore = payload.indexOf('_');
        if (underscore <= 0 || underscore >= payload.length() - 1) {
            return Optional.empty();
        }
        String first = payload.substring(0, underscore);
        String second = payload.substring(underscore + 1);
        Optional<Integer> a = parseDigitsOnlyPositive(first);
        Optional<Integer> b = parseDigitsOnlyPositive(second);
        if (a.isEmpty() || b.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new IntPair(a.get(), b.get()));
    }

    private static Optional<Integer> parseDigitsOnlyPositive(String s) {
        if (s.isEmpty() || s.length() > MAX_SUFFIX_LEN) {
            return Optional.empty();
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return Optional.empty();
            }
        }
        try {
            int value = Integer.parseInt(s);
            return value > 0 ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public record IntPair(int first, int second) {}
}

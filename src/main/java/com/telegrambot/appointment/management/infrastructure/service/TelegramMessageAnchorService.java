package com.telegrambot.appointment.management.infrastructure.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class TelegramMessageAnchorService {

    private static final String KEY_PREFIX = "appointment:telegram:message-anchor:";
    private static final Duration TTL = Duration.ofHours(48);

    private final StringRedisTemplate stringRedisTemplate;

    public TelegramMessageAnchorService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void remember(Long telegramUserId, Integer messageId) {
        if (telegramUserId == null || messageId == null) {
            return;
        }
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + telegramUserId, messageId.toString(), TTL);
    }

    public Optional<Integer> currentMessageId(Long telegramUserId) {
        if (telegramUserId == null) {
            return Optional.empty();
        }
        String raw = stringRedisTemplate.opsForValue().get(KEY_PREFIX + telegramUserId);
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(raw));
    }

    public void forget(Long telegramUserId) {
        if (telegramUserId == null) {
            return;
        }
        stringRedisTemplate.delete(KEY_PREFIX + telegramUserId);
    }
}

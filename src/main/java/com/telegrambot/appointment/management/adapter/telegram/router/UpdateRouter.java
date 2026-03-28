package com.telegrambot.appointment.management.adapter.telegram.router;

import com.telegrambot.appointment.management.adapter.telegram.TelegramReply;
import com.telegrambot.appointment.management.adapter.telegram.role.TelegramRoleHandler;
import com.telegrambot.appointment.management.domain.model.user.UserRole;
import com.telegrambot.appointment.management.domain.port.TelegramCallbackAcknowledger;
import com.telegrambot.appointment.management.domain.service.UserRoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.EnumMap;

@Service
public class UpdateRouter {

    private static final Logger log = LoggerFactory.getLogger(UpdateRouter.class);

    private final UserRoleService userRoleService;
    private final TelegramCallbackAcknowledger callbackAcknowledger;
    private final EnumMap<UserRole, TelegramRoleHandler> roleHandlers;

    public UpdateRouter(UserRoleService userRoleService,
                        TelegramCallbackAcknowledger callbackAcknowledger,
                        EnumMap<UserRole, TelegramRoleHandler> roleHandlers) {
        this.userRoleService = userRoleService;
        this.callbackAcknowledger = callbackAcknowledger;
        this.roleHandlers = roleHandlers;
    }

    public void route(Update update, TelegramReply reply) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update.getMessage(), reply);
        } else if (update.hasCallbackQuery()) {
            handleCallback(update, reply);
        }
    }

    private void handleMessage(Message message, TelegramReply reply) {
        Long telegramId = message.getFrom().getId();
        String text = message.getText();

        UserRole role = userRoleService.defineUserRoleByTelegramId(telegramId);
        log.debug("Message: telegramId={}, role={}, text={}", telegramId, role, text);

        TelegramRoleHandler handler = roleHandlers.get(role);
        if (handler == null) {
            log.error("No TelegramRoleHandler for role {}", role);
            return;
        }
        handler.handleMessage(message, reply);
    }

    private void handleCallback(Update update, TelegramReply reply) {
        var callback = update.getCallbackQuery();
        callbackAcknowledger.acknowledge(callback.getId());

        Long telegramId = callback.getFrom().getId();
        String data = callback.getData();

        UserRole role = userRoleService.defineUserRoleByTelegramId(telegramId);
        log.debug("Callback: telegramId={}, role={}, data={}", telegramId, role, data);

        TelegramRoleHandler handler = roleHandlers.get(role);
        if (handler == null) {
            log.error("No TelegramRoleHandler for role {}", role);
            return;
        }
        handler.handleCallback(callback, reply);
    }
}

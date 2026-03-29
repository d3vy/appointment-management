package com.telegrambot.appointment.management.adapter.telegram.role;

import com.telegrambot.appointment.management.adapter.telegram.TelegramReply;
import com.telegrambot.appointment.management.adapter.telegram.handler.HelpHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.MenuHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.StartHandler;
import com.telegrambot.appointment.management.domain.model.user.UserRole;
import com.telegrambot.appointment.management.domain.service.ManagerScheduleService;
import com.telegrambot.appointment.management.domain.service.ManagerService;
import com.telegrambot.appointment.management.infrastructure.service.TelegramMessageAnchorService;
import com.telegrambot.appointment.management.infrastructure.telegram.TelegramCallbackIntParser;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component
public class ManagerRoleHandler implements TelegramRoleHandler {

    private static final String LINK_SPEC_PREFIX = "M_LNK_SP_";
    private static final String LINK_SVC_PREFIX = "M_LNK_SV_";

    private final StartHandler startHandler;
    private final MenuHandler menuHandler;
    private final HelpHandler helpHandler;
    private final ManagerService managerService;
    private final ManagerScheduleService managerScheduleService;
    private final TelegramMessageAnchorService anchorService;

    public ManagerRoleHandler(StartHandler startHandler,
                              MenuHandler menuHandler,
                              HelpHandler helpHandler,
                              ManagerService managerService,
                              ManagerScheduleService managerScheduleService,
                              TelegramMessageAnchorService anchorService) {
        this.startHandler = startHandler;
        this.menuHandler = menuHandler;
        this.helpHandler = helpHandler;
        this.managerService = managerService;
        this.managerScheduleService = managerScheduleService;
        this.anchorService = anchorService;
    }

    @Override
    public UserRole role() {
        return UserRole.MANAGER;
    }

    @Override
    public void handleMessage(Message message, TelegramReply reply) {
        Long telegramId = message.getFrom().getId();
        Long chatId = message.getChatId();
        String text = message.getText();

        if (managerScheduleService.hasActiveContext(telegramId)) {
            SendMessage msg = managerScheduleService.handleTextInput(telegramId, chatId, text);
            if (msg != null) {
                sendWizardReply(message, telegramId, msg, reply);
            }
            return;
        }
        if (managerService.hasPendingAction(telegramId)) {
            SendMessage msg = managerService.handlePendingAction(telegramId, chatId, text);
            if (msg != null) {
                sendWizardReply(message, telegramId, msg, reply);
            }
            return;
        }
        switch (text) {
            case "/start" -> {
                anchorService.forget(telegramId);
                reply.send(startHandler.prepareStartMessage(message));
            }
            case "/menu" -> sendWithOptionalEdit(telegramId, menuHandler.prepareManagerMenu(chatId), reply);
            case "/help" -> sendWithOptionalEdit(telegramId, helpHandler.prepareHelpForManager(chatId), reply);
            default -> sendWithOptionalEdit(telegramId, managerUnknownCommand(chatId), reply);
        }
    }

    @Override
    public void handleCallback(CallbackQuery callback, TelegramReply reply) {
        String data = callback.getData();
        Long telegramId = callback.getFrom().getId();
        Message message = (Message) callback.getMessage();
        Long chatId = message.getChatId();
        Integer messageId = message.getMessageId();

        anchorService.remember(telegramId, messageId);

        if ("MANAGER_MAIN_MENU".equals(data)) {
            managerService.clearPendingActionIfPresent(telegramId);
            managerScheduleService.clearScheduleContextIfPresent(telegramId);
            reply.sendOrEdit(menuHandler.prepareManagerMenu(chatId), messageId);
            return;
        }
        if ("M_LNK_BACK_LIST".equals(data)) {
            reply.sendOrEdit(managerService.startLinkServiceFlow(chatId), messageId);
            return;
        }
        if ("MANAGER_PENDING_BACK".equals(data)) {
            managerService.navigatePendingBack(telegramId, chatId).ifPresentOrElse(
                    msg -> reply.sendOrEdit(msg, messageId),
                    () -> reply.sendOrEdit(menuHandler.prepareManagerMenu(chatId), messageId));
            return;
        }

        if (data.startsWith("SCHED_")) {
            if (data.startsWith("SCHED_DEL_DAY_")) {
                var scheduleId = TelegramCallbackIntParser.parsePositiveIntSuffix(data, "SCHED_DEL_DAY_");
                if (scheduleId.isEmpty()) {
                    reply.sendOrEdit(invalidManagerCallback(chatId), messageId);
                    return;
                }
                reply.sendOrEdit(
                        managerScheduleService.deleteDayWithCheck(telegramId, chatId, scheduleId.get()), messageId);
            } else if (data.startsWith("SCHED_DEL_CONFIRM_")) {
                var scheduleId = TelegramCallbackIntParser.parsePositiveIntSuffix(data, "SCHED_DEL_CONFIRM_");
                if (scheduleId.isEmpty()) {
                    reply.sendOrEdit(invalidManagerCallback(chatId), messageId);
                    return;
                }
                reply.sendOrEdit(
                        managerScheduleService.confirmDeleteDay(telegramId, chatId, scheduleId.get()), messageId);
            } else if (data.startsWith("SCHED_DAY_")) {
                var scheduleId = TelegramCallbackIntParser.parsePositiveIntSuffix(data, "SCHED_DAY_");
                if (scheduleId.isEmpty()) {
                    reply.sendOrEdit(invalidManagerCallback(chatId), messageId);
                    return;
                }
                reply.sendOrEdit(
                        managerScheduleService.buildDayDetailMessage(telegramId, chatId, scheduleId.get()),
                        messageId);
            } else if (data.startsWith("SCHED_BACK_TO_DAY_")) {
                var specialistId = TelegramCallbackIntParser.parsePositiveIntSuffix(data, "SCHED_BACK_TO_DAY_");
                if (specialistId.isEmpty()) {
                    reply.sendOrEdit(invalidManagerCallback(chatId), messageId);
                    return;
                }
                reply.sendOrEdit(
                        managerScheduleService.buildSpecialistScheduleMessage(chatId, specialistId.get()),
                        messageId);
            } else if (data.startsWith("SCHED_BACK_TO_SPECIALISTS")) {
                reply.sendOrEdit(managerScheduleService.startScheduleFlow(telegramId, chatId), messageId);
            } else if (data.startsWith("SCHED_ADD_")) {
                var specialistId = TelegramCallbackIntParser.parsePositiveIntSuffix(data, "SCHED_ADD_");
                if (specialistId.isEmpty()) {
                    reply.sendOrEdit(invalidManagerCallback(chatId), messageId);
                    return;
                }
                reply.sendOrEdit(
                        managerScheduleService.startAddDaysFlow(telegramId, chatId, specialistId.get()), messageId);
            } else {
                reply.sendOrEdit(managerScheduleService.handleCallback(telegramId, chatId, data), messageId);
            }
            return;
        }
        if (data.startsWith(LINK_SPEC_PREFIX)) {
            var specialistId = TelegramCallbackIntParser.parsePositiveIntSuffix(data, LINK_SPEC_PREFIX);
            if (specialistId.isEmpty()) {
                reply.sendOrEdit(invalidManagerCallback(chatId), messageId);
                return;
            }
            reply.sendOrEdit(managerService.handleLinkPickSpecialist(chatId, specialistId.get()), messageId);
            return;
        }
        if (data.startsWith(LINK_SVC_PREFIX)) {
            var pair = TelegramCallbackIntParser.parseTwoPositiveIdsUnderscore(data.substring(LINK_SVC_PREFIX.length()));
            if (pair.isEmpty()) {
                reply.sendOrEdit(invalidManagerCallback(chatId), messageId);
                return;
            }
            var ids = pair.get();
            reply.sendOrEdit(managerService.confirmLinkService(chatId, ids.first(), ids.second()), messageId);
            return;
        }
        switch (data) {
            case "MANAGER_ADD_SPECIALIST" ->
                    reply.sendOrEdit(managerService.startAddSpecialistToWhitelist(telegramId, chatId), messageId);
            case "MANAGER_ADD_SERVICE" -> reply.sendOrEdit(managerService.startAddService(telegramId, chatId), messageId);
            case "MANAGER_LINK_SERVICE" -> reply.sendOrEdit(managerService.startLinkServiceFlow(chatId), messageId);
            case "MANAGER_SPECIALISTS" -> reply.sendOrEdit(managerService.buildSpecialistListMessage(chatId), messageId);
            case "MANAGER_SCHEDULE" -> reply.sendOrEdit(managerScheduleService.startScheduleFlow(telegramId, chatId), messageId);
        }
    }

    private static SendMessage managerUnknownCommand(Long chatId) {
        InlineKeyboardButton menu = new InlineKeyboardButton();
        menu.setText("◀️ В меню");
        menu.setCallbackData("MANAGER_MAIN_MENU");
        SendMessage outgoing = new SendMessage(chatId.toString(),
                "Неизвестная команда. Используйте /help для списка команд.");
        outgoing.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(menu))));
        return outgoing;
    }

    private static SendMessage invalidManagerCallback(Long chatId) {
        InlineKeyboardButton menu = new InlineKeyboardButton();
        menu.setText("◀️ В меню");
        menu.setCallbackData("MANAGER_MAIN_MENU");
        SendMessage outgoing = new SendMessage(chatId.toString(),
                "Действие недействительно. Откройте /menu.");
        outgoing.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(menu))));
        return outgoing;
    }

    private void sendWithOptionalEdit(Long telegramId, SendMessage outgoing, TelegramReply reply) {
        anchorService.currentMessageId(telegramId).ifPresentOrElse(
                messageId -> reply.sendOrEdit(outgoing, messageId),
                () -> reply.send(outgoing));
    }

    private void sendWizardReply(Message userMessage, Long telegramId, SendMessage outgoing, TelegramReply reply) {
        sendWithOptionalEdit(telegramId, outgoing, reply);
        reply.deleteMessage(userMessage.getChatId().toString(), userMessage.getMessageId());
    }
}

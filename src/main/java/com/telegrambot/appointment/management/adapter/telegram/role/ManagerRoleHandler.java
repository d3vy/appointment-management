package com.telegrambot.appointment.management.adapter.telegram.role;

import com.telegrambot.appointment.management.adapter.telegram.TelegramReply;
import com.telegrambot.appointment.management.adapter.telegram.handler.HelpHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.MenuHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.StartHandler;
import com.telegrambot.appointment.management.domain.model.user.UserRole;
import com.telegrambot.appointment.management.domain.service.ManagerScheduleService;
import com.telegrambot.appointment.management.domain.service.ManagerService;
import com.telegrambot.appointment.management.infrastructure.service.TelegramMessageAnchorService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

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
                sendWithOptionalEdit(telegramId, msg, reply);
            }
            return;
        }
        if (managerService.hasPendingAction(telegramId)) {
            SendMessage msg = managerService.handlePendingAction(telegramId, chatId, text);
            if (msg != null) {
                sendWithOptionalEdit(telegramId, msg, reply);
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
            default -> sendWithOptionalEdit(telegramId, new SendMessage(chatId.toString(),
                    "Неизвестная команда. Используйте /help для списка команд."), reply);
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
            reply.sendOrEdit(menuHandler.prepareManagerMenu(chatId), messageId);
            return;
        }
        if ("M_LNK_BACK_LIST".equals(data)) {
            reply.sendOrEdit(managerService.startLinkServiceFlow(chatId), messageId);
            return;
        }

        if (data.startsWith("SCHED_")) {
            if (data.startsWith("SCHED_DEL_DAY_")) {
                int scheduleId = Integer.parseInt(data.replace("SCHED_DEL_DAY_", ""));
                reply.sendOrEdit(managerScheduleService.deleteDayWithCheck(telegramId, chatId, scheduleId), messageId);
            } else if (data.startsWith("SCHED_DEL_CONFIRM_")) {
                int scheduleId = Integer.parseInt(data.replace("SCHED_DEL_CONFIRM_", ""));
                reply.sendOrEdit(managerScheduleService.confirmDeleteDay(chatId, scheduleId), messageId);
            } else if (data.startsWith("SCHED_DAY_")) {
                int scheduleId = Integer.parseInt(data.replace("SCHED_DAY_", ""));
                reply.sendOrEdit(managerScheduleService.buildDayDetailMessage(chatId, scheduleId), messageId);
            } else if (data.startsWith("SCHED_BACK_TO_DAY_")) {
                int specialistId = Integer.parseInt(data.replace("SCHED_BACK_TO_DAY_", ""));
                reply.sendOrEdit(managerScheduleService.buildSpecialistScheduleMessage(chatId, specialistId), messageId);
            } else if (data.startsWith("SCHED_BACK_TO_SPECIALISTS")) {
                reply.sendOrEdit(managerScheduleService.startScheduleFlow(telegramId, chatId), messageId);
            } else if (data.startsWith("SCHED_ADD_")) {
                int specialistId = Integer.parseInt(data.replace("SCHED_ADD_", ""));
                reply.sendOrEdit(managerScheduleService.startAddDaysFlow(telegramId, chatId, specialistId), messageId);
            } else {
                reply.sendOrEdit(managerScheduleService.handleCallback(telegramId, chatId, data), messageId);
            }
            return;
        }
        if (data.startsWith(LINK_SPEC_PREFIX)) {
            int specialistId = Integer.parseInt(data.substring(LINK_SPEC_PREFIX.length()));
            reply.sendOrEdit(managerService.handleLinkPickSpecialist(chatId, specialistId), messageId);
            return;
        }
        if (data.startsWith(LINK_SVC_PREFIX)) {
            String payload = data.substring(LINK_SVC_PREFIX.length());
            int underscore = payload.indexOf('_');
            int specialistId = Integer.parseInt(payload.substring(0, underscore));
            int serviceId = Integer.parseInt(payload.substring(underscore + 1));
            reply.sendOrEdit(managerService.confirmLinkService(chatId, specialistId, serviceId), messageId);
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

    private void sendWithOptionalEdit(Long telegramId, SendMessage outgoing, TelegramReply reply) {
        anchorService.currentMessageId(telegramId).ifPresentOrElse(
                messageId -> reply.sendOrEdit(outgoing, messageId),
                () -> reply.send(outgoing));
    }
}

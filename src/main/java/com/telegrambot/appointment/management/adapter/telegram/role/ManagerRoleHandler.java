package com.telegrambot.appointment.management.adapter.telegram.role;

import com.telegrambot.appointment.management.adapter.telegram.handler.HelpHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.MenuHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.StartHandler;
import com.telegrambot.appointment.management.domain.model.user.UserRole;
import com.telegrambot.appointment.management.domain.service.ManagerScheduleService;
import com.telegrambot.appointment.management.domain.service.ManagerService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.function.Consumer;

@Component
public class ManagerRoleHandler implements TelegramRoleHandler {

    private static final String LINK_SPEC_PREFIX = "M_LNK_SP_";
    private static final String LINK_SVC_PREFIX = "M_LNK_SV_";

    private final StartHandler startHandler;
    private final MenuHandler menuHandler;
    private final HelpHandler helpHandler;
    private final ManagerService managerService;
    private final ManagerScheduleService managerScheduleService;

    public ManagerRoleHandler(StartHandler startHandler,
                              MenuHandler menuHandler,
                              HelpHandler helpHandler,
                              ManagerService managerService,
                              ManagerScheduleService managerScheduleService) {
        this.startHandler = startHandler;
        this.menuHandler = menuHandler;
        this.helpHandler = helpHandler;
        this.managerService = managerService;
        this.managerScheduleService = managerScheduleService;
    }

    @Override
    public UserRole role() {
        return UserRole.MANAGER;
    }

    @Override
    public void handleMessage(Message message, Consumer<SendMessage> sender) {
        Long telegramId = message.getFrom().getId();
        Long chatId = message.getChatId();
        String text = message.getText();

        if (managerScheduleService.hasActiveContext(telegramId)) {
            SendMessage msg = managerScheduleService.handleTextInput(telegramId, chatId, text);
            if (msg != null) {
                sender.accept(msg);
            }
            return;
        }
        if (managerService.hasPendingAction(telegramId)) {
            SendMessage msg = managerService.handlePendingAction(telegramId, chatId, text);
            if (msg != null) {
                sender.accept(msg);
            }
            return;
        }
        switch (text) {
            case "/start" -> sender.accept(startHandler.prepareStartMessage(message));
            case "/menu" -> sender.accept(menuHandler.prepareManagerMenu(message));
            case "/help" -> sender.accept(helpHandler.prepareHelpForManager(chatId));
            default -> sender.accept(new SendMessage(chatId.toString(),
                    "Неизвестная команда. Используйте /help для списка команд."));
        }
    }

    @Override
    public void handleCallback(CallbackQuery callback, Consumer<SendMessage> sender) {
        String data = callback.getData();
        Long telegramId = callback.getFrom().getId();
        Message message = (Message) callback.getMessage();
        Long chatId = message.getChatId();

        if (data.startsWith("SCHED_")) {
            if (data.startsWith("SCHED_DEL_DAY_")) {
                int scheduleId = Integer.parseInt(data.replace("SCHED_DEL_DAY_", ""));
                sender.accept(managerScheduleService.deleteDayWithCheck(telegramId, chatId, scheduleId));
            } else if (data.startsWith("SCHED_DEL_CONFIRM_")) {
                int scheduleId = Integer.parseInt(data.replace("SCHED_DEL_CONFIRM_", ""));
                sender.accept(managerScheduleService.confirmDeleteDay(chatId, scheduleId));
            } else if (data.startsWith("SCHED_DAY_")) {
                int scheduleId = Integer.parseInt(data.replace("SCHED_DAY_", ""));
                sender.accept(managerScheduleService.buildDayDetailMessage(chatId, scheduleId));
            } else if (data.startsWith("SCHED_BACK_TO_DAY_")) {
                int specialistId = Integer.parseInt(data.replace("SCHED_BACK_TO_DAY_", ""));
                sender.accept(managerScheduleService.buildSpecialistScheduleMessage(chatId, specialistId));
            } else if (data.startsWith("SCHED_BACK_TO_SPECIALISTS")) {
                sender.accept(managerScheduleService.startScheduleFlow(telegramId, chatId));
            } else if (data.startsWith("SCHED_ADD_")) {
                int specialistId = Integer.parseInt(data.replace("SCHED_ADD_", ""));
                sender.accept(managerScheduleService.startAddDaysFlow(telegramId, chatId, specialistId));
            } else {
                sender.accept(managerScheduleService.handleCallback(telegramId, chatId, data));
            }
            return;
        }
        if (data.startsWith(LINK_SPEC_PREFIX)) {
            int specialistId = Integer.parseInt(data.substring(LINK_SPEC_PREFIX.length()));
            sender.accept(managerService.handleLinkPickSpecialist(chatId, specialistId));
            return;
        }
        if (data.startsWith(LINK_SVC_PREFIX)) {
            String payload = data.substring(LINK_SVC_PREFIX.length());
            int underscore = payload.indexOf('_');
            int specialistId = Integer.parseInt(payload.substring(0, underscore));
            int serviceId = Integer.parseInt(payload.substring(underscore + 1));
            sender.accept(managerService.confirmLinkService(chatId, specialistId, serviceId));
            return;
        }
        switch (data) {
            case "MANAGER_ADD_SPECIALIST" ->
                    sender.accept(managerService.startAddSpecialistToWhitelist(telegramId, chatId));
            case "MANAGER_ADD_SERVICE" -> sender.accept(managerService.startAddService(telegramId, chatId));
            case "MANAGER_LINK_SERVICE" -> sender.accept(managerService.startLinkServiceFlow(chatId));
            case "MANAGER_SPECIALISTS" -> sender.accept(managerService.buildSpecialistListMessage(chatId));
            case "MANAGER_SCHEDULE" -> sender.accept(managerScheduleService.startScheduleFlow(telegramId, chatId));
        }
    }
}

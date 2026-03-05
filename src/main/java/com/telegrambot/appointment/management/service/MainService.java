package com.telegrambot.appointment.management.service;

import com.telegrambot.appointment.management.config.BotConfig;
import com.telegrambot.appointment.management.model.UserRole;
import com.telegrambot.appointment.management.service.staff.MenuService;
import com.telegrambot.appointment.management.service.staff.RegistrationService;
import com.telegrambot.appointment.management.service.staff.StartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
public class MainService extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(MainService.class);
    private final BotConfig botConfig;
    private final StartService startService;
    private final RegistrationService registrationService;
    private final MenuService menuService;
    private final UserRoleService userRoleService;
    private final ManagerService managerService;

    public MainService(
            BotConfig botConfig,
            StartService startService,
            RegistrationService registrationService,
            MenuService menuService,
            UserRoleService userRoleService,
            ManagerService managerService
    ) {
        this.botConfig = botConfig;
        this.startService = startService;
        this.registrationService = registrationService;
        this.menuService = menuService;
        this.userRoleService = userRoleService;
        this.managerService = managerService;
    }

    @Override
    public String getBotUsername() {
        return this.botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return this.botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            Long telegramId = message.getFrom().getId();
            String username = message.getFrom().getUserName();
            Long chatId = message.getChatId();
            String messageText = message.getText();
            UserRole role = this.userRoleService.defineUserRoleByTelegramId(telegramId);


            if (role == UserRole.NOT_REGISTERED) {
                switch (messageText) {
                    case "/start" -> sendMessage(this.startService.prepareStartMessage(message));
                    default -> {
                        if (this.registrationService.isRegistering(telegramId)) {
                            SendMessage msg = this.registrationService.handleMessage(telegramId, chatId, messageText);
                            if (msg != null) sendMessage(msg);
                        } else {
                            send(chatId, "Сначала зарегистрируйтесь");
                        }
                    }
                }
            } else if (role == UserRole.CLIENT) {
                switch (messageText) {
                    case "/start" -> {
                        SendMessage messageToSend = this.startService.prepareStartMessage(message);
                        sendMessage(messageToSend);
                    }
                }
            } else if (role == UserRole.SPECIALIST) {

            } else if (role == UserRole.MANAGER) {

            }


        } else if (update.hasCallbackQuery()) {
            var callback = update.getCallbackQuery();
            String data = callback.getData();
            Long telegramId = callback.getFrom().getId();
            Message message = (Message) callback.getMessage();
            String username = callback.getFrom().getUserName();
            Long chatId = message.getChatId();
            UserRole role = this.userRoleService.defineUserRoleByTelegramId(telegramId);

            if (role == UserRole.NOT_REGISTERED) {
                switch (data) {
                    case "REGISTER" -> {
                        SendMessage msg;
                        log.info("REGISTER callback: telegramId={}, username={}, whitelisted={}",
                                telegramId, username, userRoleService.isManagerWhitelisted(username));
                        if (userRoleService.isManagerWhitelisted(username)) {
                            msg = registrationService.startManagerRegistration(telegramId, chatId, username);
                        } else {
                            msg = registrationService.startClientRegistration(telegramId, chatId, username);
                        }
                        sendMessage(msg);
                    }
                    case "BACK_TO_FIRSTNAME", "BACK_TO_LASTNAME",
                         "BACK_TO_MANAGER_FIRSTNAME", "BACK_TO_MANAGER_LASTNAME" -> {
                        SendMessage msg = registrationService.handleBackCallback(telegramId, chatId, data);
                        if (msg != null) sendMessage(msg);
                    }
                }
            } else if (role == UserRole.CLIENT) {

            } else if (role == UserRole.SPECIALIST) {

            } else if (role == UserRole.MANAGER) {

            }


        }
    }


    public void send(Long chatId, String text) {
        try {
            execute(new SendMessage(chatId.toString(), text));
        } catch (TelegramApiException e) {
            log.error("Cannot send the message", e);
        }
    }

    public void sendMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Cannot send the message", e);
        }
    }
}

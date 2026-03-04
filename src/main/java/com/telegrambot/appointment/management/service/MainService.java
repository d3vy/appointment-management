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

    public MainService(
            BotConfig botConfig,
            StartService startService,
            RegistrationService registrationService,
            MenuService menuService,
            UserRoleService userRoleService
    ) {
        this.botConfig = botConfig;
        this.startService = startService;
        this.registrationService = registrationService;
        this.menuService = menuService;
        this.userRoleService = userRoleService;
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
                    default -> {
                        if (this.registrationService.isRegistering(telegramId)) {
                            this.registrationService.handleMessage(telegramId, chatId, messageText);
                        } else {
                            send(chatId, "Сначала зарегистрируйтесь");
                        }
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
            Long chatId = message.getChatId();
            UserRole role = this.userRoleService.defineUserRoleByTelegramId(telegramId);

            if (role == UserRole.NOT_REGISTERED) {
                switch (data) {
                    case "REGISTER" -> {
                        SendMessage msg = this.registrationService.startRegistration(telegramId, chatId);
                        sendMessage(msg);
                    }
                    case "ROLE_CLIENT", "ROLE_SPECIALIST", "ROLE_MANAGER" -> {
                        SendMessage msg = this.registrationService.handleRoleCallback(telegramId, chatId, data);
                        if (msg != null) sendMessage(msg);
                    }
                    case "BACK_TO_ROLE", "BACK_TO_FIRSTNAME", "BACK_TO_LASTNAME" -> {
                        SendMessage msg = this.registrationService.handleBackCallback(telegramId, chatId, data);
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

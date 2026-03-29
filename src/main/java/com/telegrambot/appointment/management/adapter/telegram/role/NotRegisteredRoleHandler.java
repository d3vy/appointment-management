package com.telegrambot.appointment.management.adapter.telegram.role;

import com.telegrambot.appointment.management.adapter.telegram.TelegramReply;
import com.telegrambot.appointment.management.adapter.telegram.handler.HelpHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.RegistrationHandler;
import com.telegrambot.appointment.management.adapter.telegram.handler.StartHandler;
import com.telegrambot.appointment.management.domain.model.user.UserRole;
import com.telegrambot.appointment.management.domain.service.UserRoleService;
import com.telegrambot.appointment.management.infrastructure.service.TelegramMessageAnchorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component
public class NotRegisteredRoleHandler implements TelegramRoleHandler {

    private static final Logger log = LoggerFactory.getLogger(NotRegisteredRoleHandler.class);

    private final StartHandler startHandler;
    private final HelpHandler helpHandler;
    private final RegistrationHandler registrationHandler;
    private final UserRoleService userRoleService;
    private final TelegramMessageAnchorService anchorService;

    public NotRegisteredRoleHandler(StartHandler startHandler,
                                    HelpHandler helpHandler,
                                    RegistrationHandler registrationHandler,
                                    UserRoleService userRoleService,
                                    TelegramMessageAnchorService anchorService) {
        this.startHandler = startHandler;
        this.helpHandler = helpHandler;
        this.registrationHandler = registrationHandler;
        this.userRoleService = userRoleService;
        this.anchorService = anchorService;
    }

    @Override
    public UserRole role() {
        return UserRole.NOT_REGISTERED;
    }

    @Override
    public void handleContact(Message message, TelegramReply reply) {
        Long telegramId = message.getFrom().getId();
        Long chatId = message.getChatId();
        if (!registrationHandler.isRegistering(telegramId)) {
            return;
        }
        SendMessage msg = registrationHandler.handleContact(telegramId, chatId, message.getContact());
        if (msg != null) {
            sendWizardReply(message, telegramId, msg, reply);
        }
    }

    @Override
    public void handleMessage(Message message, TelegramReply reply) {
        Long telegramId = message.getFrom().getId();
        Long chatId = message.getChatId();
        String text = message.getText();

        switch (text) {
            case "/start" -> {
                anchorService.forget(telegramId);
                reply.send(startHandler.prepareStartMessage(message));
            }
            case "/help" -> sendWithOptionalEdit(telegramId, helpHandler.prepareHelpForUnregistered(chatId), reply);
            default -> {
                if (registrationHandler.isRegistering(telegramId)) {
                    SendMessage msg = registrationHandler.handleMessage(telegramId, chatId, text);
                    if (msg != null) {
                        sendWizardReply(message, telegramId, msg, reply);
                    }
                } else {
                    sendWizardReply(message, telegramId, notRegisteredNeedStart(chatId), reply);
                }
            }
        }
    }

    @Override
    public void handleCallback(CallbackQuery callback, TelegramReply reply) {
        String data = callback.getData();
        Long telegramId = callback.getFrom().getId();
        Message message = (Message) callback.getMessage();
        Long chatId = message.getChatId();
        Integer messageId = message.getMessageId();
        String username = callback.getFrom().getUserName();

        anchorService.remember(telegramId, messageId);

        switch (data) {
            case "REGISTER" -> {
                boolean managerWhitelisted = userRoleService.isManagerWhitelisted(username);
                boolean specialistWhitelisted = userRoleService.isSpecialistWhitelisted(username);
                log.debug("REGISTER: telegramId={}, username=present, manager={}, specialist={}",
                        telegramId, managerWhitelisted, specialistWhitelisted);

                SendMessage msg;
                if (managerWhitelisted) {
                    msg = registrationHandler.startManagerRegistration(telegramId, chatId, username);
                } else if (specialistWhitelisted) {
                    msg = registrationHandler.startSpecialistRegistration(telegramId, chatId, username);
                } else {
                    msg = registrationHandler.startClientRegistration(telegramId, chatId, username);
                }
                reply.sendOrEdit(msg, messageId);
            }
            case "BACK_TO_LASTNAME", "BACK_TO_MANAGER_LASTNAME", "BACK_TO_SPECIALIST_LASTNAME",
                 "BACK_TO_FIRSTNAME", "BACK_TO_MANAGER_FIRSTNAME", "BACK_TO_SPECIALIST_FIRSTNAME" -> {
                SendMessage msg = registrationHandler.handleBackCallback(telegramId, chatId, data);
                if (msg != null) {
                    reply.sendOrEdit(msg, messageId);
                }
            }
        }
    }

    private static SendMessage notRegisteredNeedStart(Long chatId) {
        InlineKeyboardButton register = new InlineKeyboardButton();
        register.setText("Начать регистрацию");
        register.setCallbackData("REGISTER");
        SendMessage outgoing = new SendMessage(chatId.toString(),
                "Сначала зарегистрируйтесь. Нажмите /start или кнопку ниже.");
        outgoing.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(register))));
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

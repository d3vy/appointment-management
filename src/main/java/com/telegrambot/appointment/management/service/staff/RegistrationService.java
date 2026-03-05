package com.telegrambot.appointment.management.service.staff;

import com.telegrambot.appointment.management.model.UserRole;
import com.telegrambot.appointment.management.model.registration.RegistrationContext;
import com.telegrambot.appointment.management.model.registration.RegistrationStep;
import com.telegrambot.appointment.management.model.user.Client;
import com.telegrambot.appointment.management.model.user.Manager;
import com.telegrambot.appointment.management.model.user.Specialist;
import com.telegrambot.appointment.management.model.user.User;
import com.telegrambot.appointment.management.repository.ClientRepository;
import com.telegrambot.appointment.management.repository.ManagerRepository;
import com.telegrambot.appointment.management.repository.RegistrationContextRepository;
import com.telegrambot.appointment.management.repository.SpecialistRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Service
@Transactional
public class RegistrationService {

    private static final String PHONE_REGEX = "^(\\+7|8)[0-9]{10}$";

    private final ClientRepository clientRepository;
    private final ManagerRepository managerRepository;
    private final RegistrationContextRepository contextRepository;

    public RegistrationService(
            ClientRepository clientRepository,
            ManagerRepository managerRepository,
            RegistrationContextRepository contextRepository
    ) {
        this.clientRepository = clientRepository;
        this.managerRepository = managerRepository;
        this.contextRepository = contextRepository;
    }


    public SendMessage startClientRegistration(Long telegramId, Long chatId, String username) {
        RegistrationContext context = buildContext(telegramId, username, UserRole.CLIENT);
        context.setStep(RegistrationStep.ENTER_FIRSTNAME);
        contextRepository.save(context);
        return prepareAskFirstnameMessage(chatId);
    }

    public SendMessage startManagerRegistration(Long telegramId, Long chatId, String username) {
        RegistrationContext context = buildContext(telegramId, username, UserRole.MANAGER);
        context.setStep(RegistrationStep.MANAGER_ENTER_FIRSTNAME);
        contextRepository.save(context);

        return new SendMessage(chatId.toString(),
                "👔 Вы определены как менеджер.\nДавайте заполним ваш профиль.\n\nВведите ваше имя:");
    }

    private RegistrationContext buildContext(Long telegramId, String username, UserRole role) {
        RegistrationContext context = contextRepository.findById(telegramId)
                .orElse(new RegistrationContext());
        context.setTelegramId(telegramId);
        context.setUsername(username);
        context.setPendingRole(role);
        return context;
    }


    public SendMessage handleMessage(Long telegramId, Long chatId, String messageText) {
        RegistrationContext context = contextRepository.findById(telegramId).orElse(null);
        if (context == null) return null;

        return switch (context.getStep()) {

            case ENTER_FIRSTNAME -> {
                context.setFirstname(messageText.trim());
                context.setStep(RegistrationStep.ENTER_LASTNAME);
                contextRepository.save(context);
                yield prepareAskLastnameMessage(chatId);
            }
            case ENTER_LASTNAME -> {
                context.setLastname(messageText.trim());
                context.setStep(RegistrationStep.ENTER_NUMBER);
                contextRepository.save(context);
                yield prepareAskNumberMessage(chatId);
            }
            case ENTER_NUMBER -> {
                if (!isValidPhone(messageText)) {
                    yield new SendMessage(chatId.toString(),
                            "❌ Неверный формат. Введите номер в формате +79991234567 или 89991234567");
                }
                context.setNumber(messageText.trim());
                saveClient(telegramId, context);
                contextRepository.delete(context);
                yield new SendMessage(chatId.toString(), "✅ Вы успешно зарегистрированы как клиент!");
            }

            case MANAGER_ENTER_FIRSTNAME -> {
                context.setFirstname(messageText.trim());
                context.setStep(RegistrationStep.MANAGER_ENTER_LASTNAME);
                contextRepository.save(context);
                yield prepareAskLastnameMessageWithBack(chatId, "BACK_TO_MANAGER_FIRSTNAME");
            }
            case MANAGER_ENTER_LASTNAME -> {
                context.setLastname(messageText.trim());
                context.setStep(RegistrationStep.MANAGER_ENTER_NUMBER);
                contextRepository.save(context);
                yield prepareAskNumberMessageWithBack(chatId, "BACK_TO_MANAGER_LASTNAME");
            }
            case MANAGER_ENTER_NUMBER -> {
                if (!isValidPhone(messageText)) {
                    yield new SendMessage(chatId.toString(),
                            "❌ Неверный формат. Введите номер в формате +79991234567 или 89991234567");
                }
                context.setNumber(messageText.trim());
                saveManager(telegramId, context);
                contextRepository.delete(context);
                yield new SendMessage(chatId.toString(), "✅ Профиль менеджера создан!");
            }

            default -> null;
        };
    }


    public SendMessage handleBackCallback(Long telegramId, Long chatId, String data) {
        RegistrationContext context = contextRepository.findById(telegramId).orElse(null);
        if (context == null) return null;

        return switch (data) {
            case "BACK_TO_FIRSTNAME" -> {
                context.setLastname(null); // сбрасываем данные шага
                context.setStep(RegistrationStep.ENTER_FIRSTNAME);
                contextRepository.save(context);
                yield prepareAskFirstnameMessage(chatId);
            }
            case "BACK_TO_LASTNAME" -> {
                context.setNumber(null);
                context.setStep(RegistrationStep.ENTER_LASTNAME);
                contextRepository.save(context);
                yield prepareAskLastnameMessage(chatId);
            }
            case "BACK_TO_MANAGER_FIRSTNAME" -> {
                context.setLastname(null);
                context.setStep(RegistrationStep.MANAGER_ENTER_FIRSTNAME);
                contextRepository.save(context);
                yield new SendMessage(chatId.toString(), "Введите ваше имя:");
            }
            case "BACK_TO_MANAGER_LASTNAME" -> {
                context.setNumber(null);
                context.setStep(RegistrationStep.MANAGER_ENTER_LASTNAME);
                contextRepository.save(context);
                yield prepareAskLastnameMessageWithBack(chatId, "BACK_TO_MANAGER_FIRSTNAME");
            }
            default -> null;
        };
    }


    public boolean isRegistering(Long telegramId) {
        return contextRepository.findById(telegramId).isPresent();
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.replaceAll("\\s", "").matches(PHONE_REGEX);
    }


    private void saveClient(Long telegramId, RegistrationContext context) {
        Client client = new Client();
        client.setTelegramId(telegramId);
        client.setFirstname(context.getFirstname());
        client.setLastname(context.getLastname());
        client.setPhoneNumber(context.getNumber());
        client.setUsername(context.getUsername());
        clientRepository.save(client);
    }

    private void saveManager(Long telegramId, RegistrationContext context) {
        Manager manager = new Manager();
        manager.setTelegramId(telegramId);
        manager.setFirstname(context.getFirstname());
        manager.setLastname(context.getLastname());
        manager.setPhoneNumber(context.getNumber());
        manager.setUsername(context.getUsername());
        managerRepository.save(manager);
    }


    private SendMessage prepareAskFirstnameMessage(Long chatId) {
        return new SendMessage(chatId.toString(), "Введите ваше имя:");
    }

    private SendMessage prepareAskLastnameMessage(Long chatId) {
        return prepareAskLastnameMessageWithBack(chatId, "BACK_TO_FIRSTNAME");
    }

    private SendMessage prepareAskLastnameMessageWithBack(Long chatId, String backCallback) {
        SendMessage message = new SendMessage(chatId.toString(), "Введите вашу фамилию:");
        message.setReplyMarkup(buildBackKeyboard(backCallback));
        return message;
    }

    private SendMessage prepareAskNumberMessage(Long chatId) {
        return prepareAskNumberMessageWithBack(chatId, "BACK_TO_LASTNAME");
    }

    private SendMessage prepareAskNumberMessageWithBack(Long chatId, String backCallback) {
        SendMessage message = new SendMessage(chatId.toString(),
                "Введите ваш номер телефона:\n(формат: +79991234567)");
        message.setReplyMarkup(buildBackKeyboard(backCallback));
        return message;
    }

    private InlineKeyboardMarkup buildBackKeyboard(String callbackData) {
        InlineKeyboardButton back = new InlineKeyboardButton();
        back.setText("👈 Вернуться");
        back.setCallbackData(callbackData);
        return new InlineKeyboardMarkup(List.of(List.of(back)));
    }
}

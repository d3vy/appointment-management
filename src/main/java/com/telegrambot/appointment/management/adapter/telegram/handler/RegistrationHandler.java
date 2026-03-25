package com.telegrambot.appointment.management.adapter.telegram.handler;

import com.telegrambot.appointment.management.domain.model.registration.RegistrationContext;
import com.telegrambot.appointment.management.domain.model.registration.RegistrationStep;
import com.telegrambot.appointment.management.domain.model.user.client.Client;
import com.telegrambot.appointment.management.domain.model.user.manager.Manager;
import com.telegrambot.appointment.management.domain.model.user.UserRole;
import com.telegrambot.appointment.management.domain.model.user.specialist.Specialist;
import com.telegrambot.appointment.management.domain.service.UserRoleService;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.client.ClientRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.manager.ManagerRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.context.RegistrationContextRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.manager.ManagerWhitelistRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.specialist.SpecialistRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.specialist.SpecialistWhitelistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Component
public class RegistrationHandler {

    private static final Logger log = LoggerFactory.getLogger(RegistrationHandler.class);
    private static final String PHONE_REGEX = "^(\\+7|8)[0-9]{10}$";

    private final ClientRepository clientRepository;
    private final ManagerRepository managerRepository;
    private final RegistrationContextRepository contextRepository;
    private final UserRoleService userRoleService;
    private final SpecialistRepository specialistRepository;
    private final SpecialistWhitelistRepository specialistWhitelistRepository;
    private final ManagerWhitelistRepository managerWhitelistRepository;

    public RegistrationHandler(ClientRepository clientRepository,
                               ManagerRepository managerRepository,
                               RegistrationContextRepository contextRepository,
                               UserRoleService userRoleService,
                               SpecialistRepository specialistRepository,
                               SpecialistWhitelistRepository specialistWhitelistRepository,
                               ManagerWhitelistRepository managerWhitelistRepository) {
        this.clientRepository = clientRepository;
        this.managerRepository = managerRepository;
        this.contextRepository = contextRepository;
        this.userRoleService = userRoleService;
        this.specialistRepository = specialistRepository;
        this.specialistWhitelistRepository = specialistWhitelistRepository;
        this.managerWhitelistRepository = managerWhitelistRepository;
    }

    public SendMessage startClientRegistration(Long telegramId, Long chatId, String username) {
        RegistrationContext context = buildContext(telegramId, resolveUsername(telegramId, username), UserRole.CLIENT);
        context.setStep(RegistrationStep.ENTER_FIRSTNAME);
        contextRepository.save(context);
        log.info("Client registration started: telegramId={}", telegramId);
        return new SendMessage(chatId.toString(), "Введите ваше имя:");
    }

    public SendMessage startManagerRegistration(Long telegramId, Long chatId, String username) {
        RegistrationContext context = buildContext(telegramId, resolveUsername(telegramId, username), UserRole.MANAGER);
        context.setStep(RegistrationStep.MANAGER_ENTER_FIRSTNAME);
        contextRepository.save(context);
        log.info("Manager registration started: telegramId={}", telegramId);
        return new SendMessage(chatId.toString(),
                "👔 Вы определены как менеджер.\nДавайте заполним ваш профиль.\n\nВведите ваше имя:");
    }

    public SendMessage startSpecialistRegistration(Long telegramId, Long chatId, String username) {
        RegistrationContext context = buildContext(telegramId, resolveUsername(telegramId, username), UserRole.SPECIALIST);
        context.setStep(RegistrationStep.SPECIALIST_ENTER_FIRSTNAME);
        contextRepository.save(context);
        log.info("Specialist registration started: telegramId={}", telegramId);
        return new SendMessage(chatId.toString(),
                "🔧 Вы определены как специалист.\nДавайте заполним ваш профиль.\n\nВведите ваше имя:");
    }


    public SendMessage handleMessage(Long telegramId, Long chatId, String messageText) {
        RegistrationContext context = contextRepository.findById(telegramId).orElse(null);
        if (context == null) {
            log.warn("No registration context found: telegramId={}", telegramId);
            return null;
        }

        return switch (context.getStep()) {
            case ENTER_FIRSTNAME -> {
                context.setFirstname(messageText.trim());
                context.setStep(RegistrationStep.ENTER_LASTNAME);
                contextRepository.save(context);
                yield prepareAskLastnameMessage(chatId, "BACK_TO_FIRSTNAME");
            }
            case ENTER_LASTNAME -> {
                context.setLastname(messageText.trim());
                context.setStep(RegistrationStep.ENTER_NUMBER);
                contextRepository.save(context);
                yield prepareAskNumberMessage(chatId, "BACK_TO_LASTNAME");
            }
            case ENTER_NUMBER -> {
                if (!isValidPhone(messageText)) {
                    yield new SendMessage(chatId.toString(),
                            "❌ Неверный формат. Введите номер в формате +79991234567 или 89991234567");
                }
                context.setNumber(messageText.trim());
                saveClient(telegramId, context);
                contextRepository.deleteById(telegramId);
                userRoleService.assignRole(telegramId, UserRole.CLIENT);
                log.info("Client registered: telegramId={}", telegramId);
                yield new SendMessage(chatId.toString(), "✅ Вы успешно зарегистрированы как клиент!");
            }
            case MANAGER_ENTER_FIRSTNAME -> {
                context.setFirstname(messageText.trim());
                context.setStep(RegistrationStep.MANAGER_ENTER_LASTNAME);
                contextRepository.save(context);
                yield prepareAskLastnameMessage(chatId, "BACK_TO_MANAGER_FIRSTNAME");
            }
            case MANAGER_ENTER_LASTNAME -> {
                context.setLastname(messageText.trim());
                context.setStep(RegistrationStep.MANAGER_ENTER_NUMBER);
                contextRepository.save(context);
                yield prepareAskNumberMessage(chatId, "BACK_TO_MANAGER_LASTNAME");
            }
            case MANAGER_ENTER_NUMBER -> {
                if (!isValidPhone(messageText)) {
                    yield new SendMessage(chatId.toString(),
                            "❌ Неверный формат. Введите номер в формате +79991234567 или 89991234567");
                }
                context.setNumber(messageText.trim());
                saveManager(telegramId, context);
                contextRepository.deleteById(telegramId);
                userRoleService.assignRole(telegramId, UserRole.MANAGER);
                log.info("Manager registered: telegramId={}", telegramId);
                yield new SendMessage(chatId.toString(), "✅ Профиль менеджера создан!");
            }

            case SPECIALIST_ENTER_FIRSTNAME -> {
                context.setFirstname(messageText.trim());
                context.setStep(RegistrationStep.SPECIALIST_ENTER_LASTNAME);
                contextRepository.save(context);
                yield prepareAskLastnameMessage(chatId, "BACK_TO_SPECIALIST_FIRSTNAME");
            }
            case SPECIALIST_ENTER_LASTNAME -> {
                context.setLastname(messageText.trim());
                context.setStep(RegistrationStep.SPECIALIST_ENTER_NUMBER);
                contextRepository.save(context);
                yield prepareAskNumberMessage(chatId, "BACK_TO_SPECIALIST_LASTNAME");
            }
            case SPECIALIST_ENTER_NUMBER -> {
                if (!isValidPhone(messageText)) {
                    yield new SendMessage(chatId.toString(),
                            "❌ Неверный формат. Введите номер в формате +79991234567 или 89991234567");
                }
                context.setNumber(messageText.trim());
                saveSpecialist(telegramId, context);
                contextRepository.deleteById(telegramId);
                userRoleService.assignRole(telegramId, UserRole.SPECIALIST);
                log.info("Specialist registered: telegramId={}", telegramId);
                yield new SendMessage(chatId.toString(), "✅ Профиль специалиста создан!");
            }
            default -> {
                log.warn("Unexpected step={} for telegramId={}", context.getStep(), telegramId);
                yield null;
            }
        };
    }

    public SendMessage handleBackCallback(Long telegramId, Long chatId, String data) {
        RegistrationContext context = contextRepository.findById(telegramId).orElse(null);
        if (context == null) return null;

        return switch (data) {
            case "BACK_TO_FIRSTNAME" -> {
                context.setLastname(null);
                context.setStep(RegistrationStep.ENTER_FIRSTNAME);
                contextRepository.save(context);
                yield new SendMessage(chatId.toString(), "Введите ваше имя:");
            }
            case "BACK_TO_LASTNAME" -> {
                context.setNumber(null);
                context.setStep(RegistrationStep.ENTER_LASTNAME);
                contextRepository.save(context);
                yield prepareAskLastnameMessage(chatId, "BACK_TO_FIRSTNAME");
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
                yield prepareAskLastnameMessage(chatId, "BACK_TO_MANAGER_FIRSTNAME");
            }

            case "BACK_TO_SPECIALIST_FIRSTNAME" -> {
                context.setLastname(null);
                context.setStep(RegistrationStep.SPECIALIST_ENTER_FIRSTNAME);
                contextRepository.save(context);
                yield new SendMessage(chatId.toString(), "Введите ваше имя:");
            }
            case "BACK_TO_SPECIALIST_LASTNAME" -> {
                context.setNumber(null);
                context.setStep(RegistrationStep.SPECIALIST_ENTER_LASTNAME);
                contextRepository.save(context);
                yield prepareAskLastnameMessage(chatId, "BACK_TO_SPECIALIST_FIRSTNAME");
            }
            default -> null;
        };
    }

    public boolean isRegistering(Long telegramId) {
        return contextRepository.existsById(telegramId);
    }

    private String resolveUsername(Long telegramId, String username) {
        return (username != null && !username.isBlank()) ? username : telegramId.toString();
    }

    private RegistrationContext buildContext(Long telegramId, String username, UserRole role) {
        RegistrationContext context = contextRepository.findById(telegramId)
                .orElse(new RegistrationContext());
        context.setTelegramId(telegramId);
        context.setUsername(username);
        context.setPendingRole(role);
        return context;
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.replaceAll("\\s", "").matches(PHONE_REGEX);
    }

    @Transactional
    protected void saveClient(Long telegramId, RegistrationContext context) {
        Client client = new Client();
        client.setTelegramId(telegramId);
        client.setFirstname(context.getFirstname());
        client.setLastname(context.getLastname());
        client.setPhoneNumber(context.getNumber());
        client.setUsername(context.getUsername());
        clientRepository.save(client);
    }

    @Transactional
    protected void saveManager(Long telegramId, RegistrationContext context) {
        Manager manager = new Manager();
        manager.setTelegramId(telegramId);
        manager.setFirstname(context.getFirstname());
        manager.setLastname(context.getLastname());
        manager.setPhoneNumber(context.getNumber());
        manager.setUsername(context.getUsername());
        managerRepository.save(manager);
        managerWhitelistRepository.deleteById(context.getUsername());
        log.info("Manager registered and removed from whitelist: username={}", context.getUsername());
    }

    @Transactional
    protected void saveSpecialist(Long telegramId, RegistrationContext context) {
        Specialist specialist = new Specialist();
        specialist.setTelegramId(telegramId);
        specialist.setFirstname(context.getFirstname());
        specialist.setLastname(context.getLastname());
        specialist.setPhoneNumber(context.getNumber());
        specialist.setUsername(context.getUsername());
        specialistRepository.save(specialist);
        specialistWhitelistRepository.deleteById(context.getUsername());
        log.info("Specialist registered and removed from whitelist: username={}", context.getUsername());
    }

    private SendMessage prepareAskLastnameMessage(Long chatId, String backCallback) {
        SendMessage message = new SendMessage(chatId.toString(), "Введите вашу фамилию:");
        message.setReplyMarkup(buildBackKeyboard(backCallback));
        return message;
    }

    private SendMessage prepareAskNumberMessage(Long chatId, String backCallback) {
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

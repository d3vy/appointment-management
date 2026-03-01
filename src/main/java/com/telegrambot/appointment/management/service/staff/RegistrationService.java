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

    private final ClientRepository clientRepository;
    private final SpecialistRepository specialistRepository;
    private final ManagerRepository managerRepository;
    private final RegistrationContextRepository contextRepository;

    public RegistrationService(
            ClientRepository clientRepository,
            SpecialistRepository specialistRepository,
            ManagerRepository managerRepository,
            RegistrationContextRepository contextRepository
    ) {
        this.clientRepository = clientRepository;
        this.specialistRepository = specialistRepository;
        this.managerRepository = managerRepository;
        this.contextRepository = contextRepository;
    }

    public void createContext(Long telegramId) {
        RegistrationContext context = new RegistrationContext();
        context.setTelegramId(telegramId);
        context.setStep(RegistrationStep.CHOOSE_ROLE);
        this.contextRepository.save(context);
    }

    public SendMessage prepareAskRoleMessage(Long chatId) {
        String text = "Кто вы?";
        SendMessage message = new SendMessage(chatId.toString(), text);

        InlineKeyboardButton client = new InlineKeyboardButton();
        client.setText("✌️ Клиент");
        client.setCallbackData("ROLE_CLIENT");

        InlineKeyboardButton specialist = new InlineKeyboardButton();
        specialist.setText("💪 Специалист");
        specialist.setCallbackData("ROLE_SPECIALIST");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(
                List.of(List.of(client, specialist))
        );
        message.setReplyMarkup(keyboard);

        return message;
    }


    public void handleRoleCallback(Long telegramId, String data) {
        RegistrationContext context
                = this.contextRepository.findById(telegramId).orElse(null);
        if (context == null) return;
        if (context.getStep() != RegistrationStep.CHOOSE_ROLE) return;

        if ("ROLE_CLIENT".equals(data)) {
            context.setRole(UserRole.CLIENT);
        } else if ("ROLE_SPECIALIST".equals(data)) {
            context.setRole(UserRole.SPECIALIST);
        } else if ("ROLE_MANAGER".equals(data)) {
            context.setRole(UserRole.MANAGER);
        } else return;

        context.setStep(RegistrationStep.ENTER_FIRSTNAME);
    }

    public SendMessage prepareAskFirstnameMessage(Long chatId) {
        SendMessage message = new SendMessage(chatId.toString(), "Введите ваше имя");

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("👈 Вернуться");
        backButton.setCallbackData("BACK_TO_ROLE");

        InlineKeyboardMarkup keyboard =
                new InlineKeyboardMarkup(List.of(List.of(backButton)));

        message.setReplyMarkup(keyboard);

        return message;
    }

    public boolean isRegistering(Long telegramId) {
        return this.contextRepository.findById(telegramId).isPresent();
    }


    public void handleMessage(Long telegramId, Long chatId, String messageText) {
        RegistrationContext context
                = this.contextRepository.findById(telegramId).orElse(null);
        if (context == null) return;

        switch (context.getStep()) {
            case ENTER_FIRSTNAME -> {
                context.setFirstname(messageText);
                context.setStep(RegistrationStep.ENTER_NUMBER);
                this.contextRepository.save(context);
            }
            case ENTER_LASTNAME -> {
                context.setLastname(messageText);
                context.setStep(RegistrationStep.ENTER_NUMBER);
                this.contextRepository.save(context);
            }
            case ENTER_NUMBER -> {
                context.setNumber(messageText);
                saveUser(telegramId, context);
                context.setStep(RegistrationStep.REGISTERED);
            }
        }
    }

    private void saveUser(Long telegramId, RegistrationContext context) {
        User user;

        if (context.getRole() == UserRole.CLIENT) {
            user = new Client();
        } else if (context.getRole() == UserRole.SPECIALIST) {
            user = new Specialist();
        } else if (context.getRole() == UserRole.MANAGER) {
            user = new Manager();
        } else {
            throw new IllegalArgumentException("Unknown role");
        }

        user.setTelegramId(telegramId);
        user.setFirstname(context.getFirstname());
        user.setLastname(context.getLastname());
        user.setPhoneNumber(context.getNumber());

        if (user instanceof Client client) {
            this.clientRepository.save(client);
        } else if (user instanceof Specialist specialist) {
            this.specialistRepository.save(specialist);
        } else {
            Manager manager = (Manager) user;
            this.managerRepository.save(manager);
        }
    }
}

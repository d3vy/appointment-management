package com.telegrambot.appointment.management.domain.service;

import com.telegrambot.appointment.management.domain.model.appointment.Service;
import com.telegrambot.appointment.management.domain.model.user.manager.Manager;
import com.telegrambot.appointment.management.domain.model.user.manager.ManagerAction;
import com.telegrambot.appointment.management.domain.model.user.manager.ManagerPendingAction;
import com.telegrambot.appointment.management.domain.model.user.specialist.Specialist;
import com.telegrambot.appointment.management.domain.model.user.specialist.SpecialistWhitelist;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.appointment.ServiceRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.context.ManagerPendingActionRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.manager.ManagerRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.specialist.SpecialistRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.specialist.SpecialistWhitelistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class ManagerService {

    private static final Logger log = LoggerFactory.getLogger(ManagerService.class);
    private static final String LINK_SPEC_PREFIX = "M_LNK_SP_";
    private static final String LINK_SVC_PREFIX = "M_LNK_SV_";
    private static final String CALLBACK_MANAGER_MAIN_MENU = "MANAGER_MAIN_MENU";
    private static final String CALLBACK_LINK_BACK_LIST = "M_LNK_BACK_LIST";

    private final ManagerRepository managerRepository;
    private final ManagerPendingActionRepository pendingActionRepository;
    private final SpecialistWhitelistRepository specialistWhitelistRepository;
    private final SpecialistRepository specialistRepository;
    private final ServiceRepository serviceRepository;

    public ManagerService(ManagerRepository managerRepository,
                          ManagerPendingActionRepository pendingActionRepository,
                          SpecialistWhitelistRepository specialistWhitelistRepository,
                          SpecialistRepository specialistRepository,
                          ServiceRepository serviceRepository) {
        this.managerRepository = managerRepository;
        this.pendingActionRepository = pendingActionRepository;
        this.specialistWhitelistRepository = specialistWhitelistRepository;
        this.specialistRepository = specialistRepository;
        this.serviceRepository = serviceRepository;
    }

    @Transactional(readOnly = true)
    public List<String> getAllManagersUsername() {
        List<String> usernames = managerRepository.findAll().stream()
                .map(Manager::getUsername)
                .toList();
        log.debug("Fetched {} manager usernames", usernames.size());
        return usernames;
    }

    public SendMessage startAddSpecialistToWhitelist(Long telegramId, Long chatId) {
        ManagerPendingAction pendingAction = new ManagerPendingAction(telegramId, ManagerAction.AWAITING_SPECIALIST_USERNAME);
        pendingActionRepository.save(pendingAction);
        return new SendMessage(chatId.toString(), "Введите username специалиста (без @):");
    }

    public SendMessage startAddService(Long telegramId, Long chatId) {
        ManagerPendingAction pendingAction = new ManagerPendingAction(telegramId, ManagerAction.AWAITING_SERVICE_NAME);
        pendingActionRepository.save(pendingAction);
        return new SendMessage(chatId.toString(), "Введите название услуги:");
    }

    @Transactional(readOnly = true)
    public SendMessage startLinkServiceFlow(Long chatId) {
        List<Specialist> specialists = specialistRepository.findAllWithServices();
        if (specialists.isEmpty()) {
            return singleRowMessage(chatId,
                    "Нет зарегистрированных специалистов. Сначала добавьте специалиста в whitelist.",
                    inlineBtn("◀️ В меню", CALLBACK_MANAGER_MAIN_MENU));
        }
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Specialist s : specialists) {
            String label = s.getFirstname() + " " + s.getLastname() + " (@" + s.getUsername() + ")";
            rows.add(List.of(inlineBtn(label, LINK_SPEC_PREFIX + s.getId())));
        }
        rows.add(List.of(inlineBtn("◀️ В меню", CALLBACK_MANAGER_MAIN_MENU)));
        SendMessage message = new SendMessage(chatId.toString(), "Выберите специалиста для привязки услуги:");
        message.setReplyMarkup(new InlineKeyboardMarkup(rows));
        return message;
    }

    @Transactional
    public SendMessage handleLinkPickSpecialist(Long chatId, Integer specialistId) {
        Specialist specialist = specialistRepository.findById(specialistId).orElse(null);
        if (specialist == null) {
            return singleRowMessage(chatId, "Специалист не найден.",
                    inlineBtn("◀️ В меню", CALLBACK_MANAGER_MAIN_MENU));
        }
        Set<Integer> linkedIds = specialist.getServices().stream().map(Service::getId).collect(Collectors.toSet());
        List<Service> available = serviceRepository.findAll().stream()
                .filter(s -> !linkedIds.contains(s.getId()))
                .toList();
        if (available.isEmpty()) {
            return twoButtonRowMessage(chatId,
                    "У этого специалиста уже все услуги из каталога. Добавьте новую услугу через меню.",
                    inlineBtn("◀️ Назад", CALLBACK_LINK_BACK_LIST),
                    inlineBtn("◀️ В меню", CALLBACK_MANAGER_MAIN_MENU));
        }
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Service service : available) {
            String label = service.getName() + " — " + service.getPrice().setScale(0, RoundingMode.HALF_UP) + " ₽, "
                    + service.getDurationMinutes() + " мин";
            rows.add(List.of(inlineBtn(label, LINK_SVC_PREFIX + specialistId + "_" + service.getId())));
        }
        rows.add(List.of(
                inlineBtn("◀️ Назад", CALLBACK_LINK_BACK_LIST),
                inlineBtn("◀️ В меню", CALLBACK_MANAGER_MAIN_MENU)));
        SendMessage message = new SendMessage(chatId.toString(),
                "Выберите услугу для «" + specialist.getFirstname() + " " + specialist.getLastname() + "»:");
        message.setReplyMarkup(new InlineKeyboardMarkup(rows));
        return message;
    }

    @Transactional
    public SendMessage confirmLinkService(Long chatId, Integer specialistId, Integer serviceId) {
        Specialist specialist = specialistRepository.findById(specialistId).orElse(null);
        Service service = serviceRepository.findById(serviceId).orElse(null);
        if (specialist == null || service == null) {
            return singleRowMessage(chatId, "Специалист или услуга не найдены.",
                    inlineBtn("◀️ В меню", CALLBACK_MANAGER_MAIN_MENU));
        }
        if (specialist.getServices().stream().anyMatch(s -> s.getId().equals(serviceId))) {
            return singleRowMessage(chatId, "Эта услуга уже назначена специалисту.",
                    inlineBtn("◀️ В меню", CALLBACK_MANAGER_MAIN_MENU));
        }
        specialist.getServices().add(service);
        specialistRepository.save(specialist);
        log.info("Linked serviceId={} to specialistId={}", serviceId, specialistId);
        SendMessage done = new SendMessage(chatId.toString(),
                "✅ Услуга «" + service.getName() + "» назначена специалисту "
                        + specialist.getFirstname() + " " + specialist.getLastname() + ".");
        done.setReplyMarkup(new InlineKeyboardMarkup(List.of(
                List.of(inlineBtn("◀️ В меню", CALLBACK_MANAGER_MAIN_MENU)))));
        return done;
    }

    private static SendMessage singleRowMessage(Long chatId, String text, InlineKeyboardButton button) {
        SendMessage message = new SendMessage(chatId.toString(), text);
        message.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(button))));
        return message;
    }

    private static SendMessage twoButtonRowMessage(Long chatId, String text,
                                                   InlineKeyboardButton first, InlineKeyboardButton second) {
        SendMessage message = new SendMessage(chatId.toString(), text);
        message.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(first, second))));
        return message;
    }

    private static InlineKeyboardButton inlineBtn(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }

    @Transactional
    public SendMessage handlePendingAction(Long telegramId, Long chatId, String messageText) {
        ManagerPendingAction pendingAction = pendingActionRepository.findById(telegramId).orElse(null);
        if (pendingAction == null) {
            return null;
        }

        return switch (pendingAction.getAction()) {
            case AWAITING_SPECIALIST_USERNAME -> handleSpecialistWhitelistInput(telegramId, chatId, messageText);
            case AWAITING_SERVICE_NAME -> handleServiceNameStep(telegramId, chatId, messageText, pendingAction);
            case AWAITING_SERVICE_PRICE -> handleServicePriceStep(telegramId, chatId, messageText, pendingAction);
            case AWAITING_SERVICE_DURATION -> handleServiceDurationStep(telegramId, chatId, messageText, pendingAction);
        };
    }

    private SendMessage handleSpecialistWhitelistInput(Long telegramId, Long chatId, String messageText) {
        String username = messageText.trim().replace("@", "");
        if (username.isBlank()) {
            return new SendMessage(chatId.toString(), "❌ Username не может быть пустым. Попробуйте ещё раз:");
        }
        if (specialistWhitelistRepository.existsByUsernameIgnoreCase(username)) {
            pendingActionRepository.deleteById(telegramId);
            return new SendMessage(chatId.toString(),
                    String.format("⚠️ @%s уже в whitelist специалистов.", username));
        }
        SpecialistWhitelist entry = new SpecialistWhitelist();
        entry.setUsername(username);
        specialistWhitelistRepository.save(entry);
        pendingActionRepository.deleteById(telegramId);
        log.info("Specialist username '{}' added to specialist whitelist by telegramId={}", username, telegramId);
        return new SendMessage(chatId.toString(),
                String.format("✅ @%s добавлен в whitelist специалистов.", username));
    }

    private SendMessage handleServiceNameStep(Long telegramId, Long chatId, String messageText,
                                              ManagerPendingAction pendingAction) {
        String name = messageText.trim();
        if (name.isBlank()) {
            return new SendMessage(chatId.toString(), "❌ Название не может быть пустым. Введите снова:");
        }
        pendingAction.setDraftServiceName(name);
        pendingAction.setAction(ManagerAction.AWAITING_SERVICE_PRICE);
        pendingActionRepository.save(pendingAction);
        return new SendMessage(chatId.toString(), "Введите цену в рублях (целое число или десятичное, например 1500 или 1500.50):");
    }

    private SendMessage handleServicePriceStep(Long telegramId, Long chatId, String messageText,
                                               ManagerPendingAction pendingAction) {
        String normalized = messageText.trim().replace(",", ".").replace(" ", "");
        final BigDecimal price;
        try {
            price = new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            return new SendMessage(chatId.toString(), "❌ Некорректная цена. Пример: 2500 или 1999.99");
        }
        if (price.signum() <= 0) {
            return new SendMessage(chatId.toString(), "❌ Цена должна быть больше нуля.");
        }
        pendingAction.setDraftServicePrice(normalized);
        pendingAction.setAction(ManagerAction.AWAITING_SERVICE_DURATION);
        pendingActionRepository.save(pendingAction);
        return new SendMessage(chatId.toString(), "Введите длительность в минутах (целое число, кратно 30 — например 30, 60, 90):");
    }

    private SendMessage handleServiceDurationStep(Long telegramId, Long chatId, String messageText,
                                                  ManagerPendingAction pendingAction) {
        int minutes;
        try {
            minutes = Integer.parseInt(messageText.trim());
        } catch (NumberFormatException e) {
            return new SendMessage(chatId.toString(), "❌ Введите целое число минут.");
        }
        if (minutes <= 0 || minutes % 30 != 0) {
            return new SendMessage(chatId.toString(), "❌ Длительность должна быть положительной и кратна 30 минутам.");
        }
        String name = pendingAction.getDraftServiceName();
        BigDecimal price = new BigDecimal(pendingAction.getDraftServicePrice());
        Service entity = new Service();
        entity.setName(name);
        entity.setPrice(price);
        entity.setDurationMinutes(minutes);
        serviceRepository.save(entity);
        pendingActionRepository.deleteById(telegramId);
        log.info("Service created id={} name='{}' by telegramId={}", entity.getId(), name, telegramId);
        return new SendMessage(chatId.toString(),
                "✅ Услуга «" + name + "» добавлена. Привяжите её к специалисту через меню.");
    }

    public boolean hasPendingAction(Long telegramId) {
        return pendingActionRepository.existsById(telegramId);
    }

    @Transactional(readOnly = true)
    public SendMessage buildSpecialistListMessage(Long chatId) {
        List<Specialist> specialists = specialistRepository.findAllWithServices();
        if (specialists.isEmpty()) {
            return singleRowMessage(chatId, "👤 Специалистов пока нет.",
                    inlineBtn("◀️ В меню", CALLBACK_MANAGER_MAIN_MENU));
        }

        StringBuilder text = new StringBuilder("👤 *Специалисты* (" + specialists.size() + "):\n\n");
        for (int i = 0; i < specialists.size(); i++) {
            Specialist specialist = specialists.get(i);
            text.append(i + 1).append(". ")
                    .append(specialist.getFirstname()).append(" ").append(specialist.getLastname())
                    .append(" (@").append(specialist.getUsername()).append(")\n");

            Set<Service> services = specialist.getServices();
            if (services.isEmpty()) {
                text.append("   _услуги не назначены_\n");
            } else {
                String serviceNames = services.stream()
                        .map(Service::getName)
                        .collect(Collectors.joining(", "));
                text.append("   💈 ").append(serviceNames).append("\n");
            }
            text.append("\n");
        }

        SendMessage message = new SendMessage(chatId.toString(), text.toString().trim());
        message.setParseMode("Markdown");
        message.setReplyMarkup(new InlineKeyboardMarkup(List.of(
                List.of(inlineBtn("◀️ В меню", CALLBACK_MANAGER_MAIN_MENU)))));
        return message;
    }
}

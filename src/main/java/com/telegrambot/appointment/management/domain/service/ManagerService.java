package com.telegrambot.appointment.management.domain.service;

import com.telegrambot.appointment.management.domain.model.appointment.Service;
import com.telegrambot.appointment.management.domain.model.user.manager.Manager;
import com.telegrambot.appointment.management.domain.model.user.manager.ManagerAction;
import com.telegrambot.appointment.management.domain.model.user.manager.ManagerPendingAction;
import com.telegrambot.appointment.management.domain.model.user.manager.ManagerWhitelist;
import com.telegrambot.appointment.management.domain.model.user.specialist.Specialist;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.context.ManagerPendingActionRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.manager.ManagerRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.manager.ManagerWhitelistRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.specialist.SpecialistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class ManagerService {

    private static final Logger log = LoggerFactory.getLogger(ManagerService.class);

    private final ManagerRepository managerRepository;
    private final ManagerPendingActionRepository pendingActionRepository;
    private final ManagerWhitelistRepository whitelistRepository;
    private final SpecialistRepository specialistRepository;

    public ManagerService(ManagerRepository managerRepository,
                          ManagerPendingActionRepository pendingActionRepository,
                          ManagerWhitelistRepository whitelistRepository,
                          SpecialistRepository specialistRepository) {
        this.managerRepository = managerRepository;
        this.pendingActionRepository = pendingActionRepository;
        this.whitelistRepository = whitelistRepository;
        this.specialistRepository = specialistRepository;
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

    @Transactional
    public SendMessage handlePendingAction(Long telegramId, Long chatId, String messageText) {
        ManagerPendingAction pendingAction = pendingActionRepository.findById(telegramId).orElse(null);
        if (pendingAction == null) {
            return null;
        }

        return switch (pendingAction.getAction()) {
            case AWAITING_SPECIALIST_USERNAME -> {
                String username = messageText.trim().replace("@", "");
                if (username.isBlank()) {
                    yield new SendMessage(chatId.toString(), "❌ Username не может быть пустым. Попробуйте ещё раз:");
                }
                if (whitelistRepository.existsByUsernameIgnoreCase(username)) {
                    pendingActionRepository.deleteById(telegramId);
                    yield new SendMessage(chatId.toString(),
                            String.format("⚠️ @%s уже в whitelist.", username));
                }
                ManagerWhitelist entry = new ManagerWhitelist();
                entry.setUsername(username);
                whitelistRepository.save(entry);
                pendingActionRepository.deleteById(telegramId);
                log.info("Specialist username '{}' added to whitelist by managerId={}", username, telegramId);
                yield new SendMessage(chatId.toString(),
                        String.format("✅ @%s добавлен в whitelist специалистов.", username));
            }
        };
    }

    public boolean hasPendingAction(Long telegramId) {
        return pendingActionRepository.existsById(telegramId);
    }

    public SendMessage buildSpecialistListMessage(Long chatId) {
        List<Specialist> specialists = specialistRepository.findAllWithServices();
        if (specialists.isEmpty()) {
            return new SendMessage(chatId.toString(), "👤 Специалистов пока нет.");
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
        return message;
    }
}

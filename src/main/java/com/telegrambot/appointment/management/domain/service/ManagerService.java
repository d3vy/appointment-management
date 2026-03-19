package com.telegrambot.appointment.management.domain.service;

import com.telegrambot.appointment.management.domain.model.user.manager.Manager;
import com.telegrambot.appointment.management.domain.model.user.manager.ManagerAction;
import com.telegrambot.appointment.management.domain.model.user.manager.ManagerPendingAction;
import com.telegrambot.appointment.management.domain.model.user.manager.ManagerWhitelist;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.manager.ManagerPendingActionRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.manager.ManagerRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.manager.ManagerWhitelistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

@Service
public class ManagerService {

    private static final Logger log = LoggerFactory.getLogger(ManagerService.class);

    private final ManagerRepository managerRepository;
    private final ManagerPendingActionRepository pendingActionRepository;
    private final ManagerWhitelistRepository whitelistRepository;

    public ManagerService(ManagerRepository managerRepository,
                          ManagerPendingActionRepository pendingActionRepository,
                          ManagerWhitelistRepository whitelistRepository) {
        this.managerRepository = managerRepository;
        this.pendingActionRepository = pendingActionRepository;
        this.whitelistRepository = whitelistRepository;
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

    @Transactional
    public void removeFromWhitelist(String username) {
        whitelistRepository.deleteByUsernameIgnoreCase(username);
        log.info("Removed username '{}' from specialist whitelist after registration", username);
    }

}

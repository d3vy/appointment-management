package com.telegrambot.appointment.management.domain.service;

import com.telegrambot.appointment.management.domain.model.user.Manager;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.ManagerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ManagerService {

    private static final Logger log = LoggerFactory.getLogger(ManagerService.class);

    private final ManagerRepository managerRepository;

    public ManagerService(ManagerRepository managerRepository) {
        this.managerRepository = managerRepository;
    }

    @Transactional(readOnly = true)
    public List<String> getAllManagersUsername() {
        List<String> usernames = managerRepository.findAll().stream()
                .map(Manager::getUsername)
                .toList();
        log.debug("Fetched {} manager usernames", usernames.size());
        return usernames;
    }
}

package com.telegrambot.appointment.management.domain.service;

import com.telegrambot.appointment.management.domain.model.user.client.Client;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.client.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public boolean isNotificationsEnabled(Long telegramId) {
        return clientRepository.findByTelegramId(telegramId)
                .map(Client::isNotificationsEnabled)
                .orElse(true);
    }

    @Transactional
    public boolean toggleNotifications(Long telegramId) {
        Client client = clientRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new IllegalStateException("Client not found: " + telegramId));
        boolean newState = !client.isNotificationsEnabled();
        client.setNotificationsEnabled(newState);
        clientRepository.save(client);
        return newState;
    }
}
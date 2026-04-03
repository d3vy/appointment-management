package com.telegrambot.appointment.management.domain.service;

import com.telegrambot.appointment.management.domain.model.notification.TelegramNotificationOutboxEntry;
import com.telegrambot.appointment.management.domain.model.notification.TelegramNotificationOutboxEventType;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.notification.TelegramNotificationOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelegramNotificationOutboxService {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationOutboxService.class);

    private final TelegramNotificationOutboxRepository outboxRepository;

    public TelegramNotificationOutboxService(TelegramNotificationOutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public void enqueueSpecialistNewAppointment(Integer appointmentId) {
        enqueue(TelegramNotificationOutboxEventType.SPECIALIST_NEW_APPOINTMENT, appointmentId);
    }

    @Transactional
    public void enqueueSpecialistClientCancellation(Integer appointmentId) {
        enqueue(TelegramNotificationOutboxEventType.SPECIALIST_CLIENT_CANCELLATION, appointmentId);
    }

    @Transactional
    public void enqueueSpecialistManagerCancellation(Integer appointmentId) {
        enqueue(TelegramNotificationOutboxEventType.SPECIALIST_MANAGER_CANCELLATION, appointmentId);
    }

    @Transactional
    public void enqueueClientManagerCancellation(Integer appointmentId) {
        enqueue(TelegramNotificationOutboxEventType.CLIENT_MANAGER_CANCELLATION, appointmentId);
    }

    private void enqueue(TelegramNotificationOutboxEventType eventType, Integer appointmentId) {
        if (outboxRepository.existsByEventTypeAndAppointmentIdAndProcessedAtIsNull(eventType, appointmentId)) {
            return;
        }
        try {
            outboxRepository.save(new TelegramNotificationOutboxEntry(eventType, appointmentId));
        } catch (DataIntegrityViolationException e) {
            log.debug("Outbox enqueue race for eventType={}, appointmentId={}", eventType, appointmentId);
        }
    }
}

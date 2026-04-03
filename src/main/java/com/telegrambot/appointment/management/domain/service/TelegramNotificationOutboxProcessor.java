package com.telegrambot.appointment.management.domain.service;

import com.telegrambot.appointment.management.domain.model.appointment.Appointment;
import com.telegrambot.appointment.management.domain.model.notification.TelegramNotificationOutboxEntry;
import com.telegrambot.appointment.management.domain.model.notification.TelegramNotificationOutboxEventType;
import com.telegrambot.appointment.management.domain.port.TelegramTextMessageSender;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.appointment.AppointmentRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.notification.TelegramNotificationOutboxRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class TelegramNotificationOutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationOutboxProcessor.class);
    private static final int MAX_ATTEMPTS = 32;
    private static final int BATCH_SIZE = 32;

    private final TelegramNotificationOutboxRepository outboxRepository;
    private final AppointmentRepository appointmentRepository;
    private final SpecialistNotificationService specialistNotificationService;
    private final TelegramTextMessageSender messageSender;

    public TelegramNotificationOutboxProcessor(TelegramNotificationOutboxRepository outboxRepository,
                                               AppointmentRepository appointmentRepository,
                                               SpecialistNotificationService specialistNotificationService,
                                               TelegramTextMessageSender messageSender) {
        this.outboxRepository = outboxRepository;
        this.appointmentRepository = appointmentRepository;
        this.specialistNotificationService = specialistNotificationService;
        this.messageSender = messageSender;
    }

    @Scheduled(fixedDelayString = "${telegram.outbox.poll-interval-ms:2000}")
    @SchedulerLock(name = "processTelegramNotificationOutbox", lockAtLeastFor = "PT2S", lockAtMostFor = "PT9M")
    @Transactional
    public void processDueEntries() {
        Instant now = Instant.now();
        List<TelegramNotificationOutboxEntry> batch = outboxRepository
                .findByProcessedAtIsNullAndNextAttemptAtLessThanEqualOrderByIdAsc(now, PageRequest.of(0, BATCH_SIZE));
        for (TelegramNotificationOutboxEntry entry : batch) {
            try {
                dispatch(entry);
                entry.setProcessedAt(Instant.now());
                entry.setLastError(null);
            } catch (Exception e) {
                int nextAttempt = entry.getAttemptCount() + 1;
                entry.setAttemptCount(nextAttempt);
                entry.setLastError(truncateMessage(e.getMessage()));
                entry.setNextAttemptAt(now.plus(calculateBackoff(nextAttempt)));
                if (nextAttempt >= MAX_ATTEMPTS) {
                    log.error("Outbox entry exhausted after {} attempts: id={}, eventType={}, appointmentId={}",
                            nextAttempt, entry.getId(), entry.getEventType(), entry.getAppointmentId(), e);
                    entry.setProcessedAt(Instant.now());
                } else {
                    log.warn("Outbox dispatch failed attempt {} for id={}, eventType={}, appointmentId={}: {}",
                            nextAttempt, entry.getId(), entry.getEventType(), entry.getAppointmentId(),
                            e.getMessage());
                }
            }
            outboxRepository.save(entry);
        }
    }

    private void dispatch(TelegramNotificationOutboxEntry entry) {
        Appointment appointment = appointmentRepository.findByIdForReminderDispatch(entry.getAppointmentId())
                .orElse(null);
        if (appointment == null) {
            log.warn("Outbox skipping missing appointment id={} for eventType={}",
                    entry.getAppointmentId(), entry.getEventType());
            return;
        }
        switch (entry.getEventType()) {
            case SPECIALIST_NEW_APPOINTMENT -> specialistNotificationService.notifyAboutNewAppointment(appointment);
            case SPECIALIST_CLIENT_CANCELLATION ->
                    specialistNotificationService.notifyAboutClientCancellation(appointment);
            case SPECIALIST_MANAGER_CANCELLATION ->
                    specialistNotificationService.notifyAboutManagerCancellation(appointment);
            case CLIENT_MANAGER_CANCELLATION ->
                    sendClientManagerCancellation(appointment);
        }
    }

    private void sendClientManagerCancellation(Appointment appointment) {
        if (appointment.getClient() == null || appointment.getClient().getTelegramId() == null) {
            return;
        }
        messageSender.sendText(appointment.getClient().getTelegramId(),
                AppointmentManagerCancellationClientMessage.build(appointment));
    }

    private static Duration calculateBackoff(int attemptNumber) {
        long seconds = Math.min(300L, 1L << Math.min(attemptNumber, 8));
        return Duration.ofSeconds(Math.max(1L, seconds));
    }

    private static String truncateMessage(String message) {
        if (message == null) {
            return null;
        }
        if (message.length() <= 2000) {
            return message;
        }
        return message.substring(0, 1997) + "...";
    }
}

package com.telegrambot.appointment.management.infrastructure.persistence.repository.notification;

import com.telegrambot.appointment.management.domain.model.notification.TelegramNotificationOutboxEntry;
import com.telegrambot.appointment.management.domain.model.notification.TelegramNotificationOutboxEventType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TelegramNotificationOutboxRepository extends JpaRepository<TelegramNotificationOutboxEntry, Long> {

    boolean existsByEventTypeAndAppointmentIdAndProcessedAtIsNull(
            TelegramNotificationOutboxEventType eventType,
            Integer appointmentId);

    List<TelegramNotificationOutboxEntry> findByProcessedAtIsNullAndNextAttemptAtLessThanEqualOrderByIdAsc(
            Instant now,
            Pageable pageable);
}

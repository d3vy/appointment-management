package com.telegrambot.appointment.management.domain.model.notification;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "telegram_notification_outbox", schema = "public")
public class TelegramNotificationOutboxEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private TelegramNotificationOutboxEventType eventType;

    @Column(name = "appointment_id", nullable = false)
    private Integer appointmentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt = Instant.now();

    @Column(name = "last_error", length = 2000)
    private String lastError;

    public TelegramNotificationOutboxEntry() {}

    public TelegramNotificationOutboxEntry(TelegramNotificationOutboxEventType eventType, Integer appointmentId) {
        this.eventType = eventType;
        this.appointmentId = appointmentId;
        this.createdAt = Instant.now();
        this.nextAttemptAt = Instant.now();
    }

    public Long getId() { return id; }
    public TelegramNotificationOutboxEventType getEventType() { return eventType; }
    public Integer getAppointmentId() { return appointmentId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(Instant nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}

package com.telegrambot.appointment.management.domain.service;

import com.telegrambot.appointment.management.domain.model.appointment.Appointment;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.handler.AppointmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

@Service
public class AppointmentReminderService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentReminderService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static final int REMINDER_WINDOW_MINUTES = 10;

    private final AppointmentRepository appointmentRepository;
    private final Consumer<SendMessage> messageSender;

    public AppointmentReminderService(AppointmentRepository appointmentRepository,
                                      Consumer<SendMessage> messageSender) {
        this.appointmentRepository = appointmentRepository;
        this.messageSender = messageSender;
    }

    @Scheduled(cron = "0 */10 * * * *")
    @Async("taskScheduler")
    @Transactional
    public void sendDayReminders() {
        LocalDateTime from = LocalDateTime.now().plusHours(24).minusMinutes(REMINDER_WINDOW_MINUTES);
        LocalDateTime to   = LocalDateTime.now().plusHours(24).plusMinutes(REMINDER_WINDOW_MINUTES);

        List<Appointment> due = appointmentRepository.findDueForDayReminder(from, to);
        log.info("Day reminders: found {} appointments", due.size());

        for (Appointment appointment : due) {
            sendClientReminder(appointment, buildDayReminderText(appointment));
            appointment.setDayReminderSent(true);
        }
    }

    @Scheduled(cron = "0 */10 * * * *")
    @Async("taskScheduler")
    @Transactional
    public void sendHourReminders() {
        LocalDateTime from = LocalDateTime.now().plusHours(1).minusMinutes(REMINDER_WINDOW_MINUTES);
        LocalDateTime to   = LocalDateTime.now().plusHours(1).plusMinutes(REMINDER_WINDOW_MINUTES);

        List<Appointment> due = appointmentRepository.findDueForHourReminder(from, to);
        log.info("Hour reminders: found {} appointments", due.size());

        for (Appointment appointment : due) {
            sendClientReminder(appointment, buildHourReminderText(appointment));
            appointment.setHourReminderSent(true);
        }
    }

    private void sendClientReminder(Appointment appointment, String text) {
        Long chatId = appointment.getClient().getTelegramId();
        try {
            messageSender.accept(new SendMessage(chatId.toString(), text));
        } catch (Exception e) {
            log.error("Failed to send reminder to clientId={}, appointmentId={}",
                    appointment.getClient().getId(), appointment.getId(), e);
        }
    }

    private String buildDayReminderText(Appointment appointment) {
        return String.format("""
                🔔 Напоминание о записи
                
                Завтра у вас запись:
                👤 Специалист: %s %s
                💈 Услуга: %s
                📅 Дата: %s
                🕐 Время: %s
                """,
                appointment.getSpecialist().getFirstname(),
                appointment.getSpecialist().getLastname(),
                appointment.getService().getName(),
                appointment.getSlot().getSchedule().getDate().format(DATE_FMT),
                appointment.getSlot().getStartTime().format(TIME_FMT)
        );
    }

    private String buildHourReminderText(Appointment appointment) {
        return String.format("""
                ⏰ Через час у вас запись!
                
                👤 Специалист: %s %s
                💈 Услуга: %s
                🕐 Время: %s
                """,
                appointment.getSpecialist().getFirstname(),
                appointment.getSpecialist().getLastname(),
                appointment.getService().getName(),
                appointment.getSlot().getStartTime().format(TIME_FMT)
        );
    }
}
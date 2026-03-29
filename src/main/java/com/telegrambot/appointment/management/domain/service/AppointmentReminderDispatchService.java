package com.telegrambot.appointment.management.domain.service;

import com.telegrambot.appointment.management.domain.model.appointment.Appointment;
import com.telegrambot.appointment.management.domain.model.appointment.AppointmentStatus;
import com.telegrambot.appointment.management.domain.port.TelegramTextMessageSender;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.appointment.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

@Service
public class AppointmentReminderDispatchService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentRepository appointmentRepository;
    private final TelegramTextMessageSender messageSender;

    public AppointmentReminderDispatchService(AppointmentRepository appointmentRepository,
                                              TelegramTextMessageSender messageSender) {
        this.appointmentRepository = appointmentRepository;
        this.messageSender = messageSender;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatchDayReminder(Integer appointmentId) {
        Appointment appointment = appointmentRepository.findByIdForReminderDispatch(appointmentId).orElse(null);
        if (appointment == null
                || appointment.getStatus() != AppointmentStatus.CONFIRMED
                || appointment.isDayReminderSent()
                || !appointment.getClient().isNotificationsEnabled()) {
            return;
        }
        messageSender.sendText(appointment.getClient().getTelegramId(), buildDayReminderText(appointment));
        appointment.setDayReminderSent(true);
        appointmentRepository.save(appointment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatchHourReminder(Integer appointmentId) {
        Appointment appointment = appointmentRepository.findByIdForReminderDispatch(appointmentId).orElse(null);
        if (appointment == null
                || appointment.getStatus() != AppointmentStatus.CONFIRMED
                || appointment.isHourReminderSent()
                || !appointment.getClient().isNotificationsEnabled()) {
            return;
        }
        messageSender.sendText(appointment.getClient().getTelegramId(), buildHourReminderText(appointment));
        appointment.setHourReminderSent(true);
        appointmentRepository.save(appointment);
    }

    private static String buildDayReminderText(Appointment appointment) {
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

    private static String buildHourReminderText(Appointment appointment) {
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

package com.telegrambot.appointment.management.domain.service;

import com.telegrambot.appointment.management.domain.model.appointment.Appointment;
import com.telegrambot.appointment.management.domain.port.TelegramTextMessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
public class SpecialistNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SpecialistNotificationService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int SLOT_DURATION_MINUTES = 30;

    private final TelegramTextMessageSender messageSender;

    public SpecialistNotificationService(TelegramTextMessageSender messageSender) {
        this.messageSender = messageSender;
    }

    public void notifyAboutNewAppointment(Appointment appointment) {
        sendToSpecialist(appointment, String.format("""
                🔔 Новая запись

                💈 Услуга: %s
                👤 Клиент: %s
                📅 Дата: %s
                🕐 Время: %s–%s
                """,
                appointment.getService().getName(),
                formatClientName(appointment),
                appointment.getSlot().getSchedule().getDate().format(DATE_FMT),
                appointment.getSlot().getStartTime().format(TIME_FMT),
                calculateEndTime(appointment).format(TIME_FMT)
        ));
    }

    public void notifyAboutClientCancellation(Appointment appointment) {
        sendToSpecialist(appointment, String.format("""
                ❌ Запись отменена клиентом

                💈 Услуга: %s
                👤 Клиент: %s
                📅 Дата: %s
                🕐 Время: %s–%s
                """,
                appointment.getService().getName(),
                formatClientName(appointment),
                appointment.getSlot().getSchedule().getDate().format(DATE_FMT),
                appointment.getSlot().getStartTime().format(TIME_FMT),
                calculateEndTime(appointment).format(TIME_FMT)
        ));
    }

    public void notifyAboutManagerCancellation(Appointment appointment) {
        sendToSpecialist(appointment, String.format("""
                ❌ Запись отменена менеджером

                💈 Услуга: %s
                👤 Клиент: %s
                📅 Дата: %s
                🕐 Время: %s–%s
                """,
                appointment.getService().getName(),
                formatClientName(appointment),
                appointment.getSlot().getSchedule().getDate().format(DATE_FMT),
                appointment.getSlot().getStartTime().format(TIME_FMT),
                calculateEndTime(appointment).format(TIME_FMT)
        ));
    }

    private void sendToSpecialist(Appointment appointment, String text) {
        if (appointment == null
                || appointment.getSpecialist() == null
                || appointment.getSpecialist().getTelegramId() == null) {
            return;
        }
        try {
            messageSender.sendText(appointment.getSpecialist().getTelegramId(), text);
        } catch (Exception e) {
            log.error("Failed to notify specialist telegramId={} for appointmentId={}",
                    appointment.getSpecialist().getTelegramId(), appointment.getId(), e);
        }
    }

    private String formatClientName(Appointment appointment) {
        String firstname = appointment.getClient() == null || appointment.getClient().getFirstname() == null
                ? ""
                : appointment.getClient().getFirstname().trim();
        String lastname = appointment.getClient() == null || appointment.getClient().getLastname() == null
                ? ""
                : appointment.getClient().getLastname().trim();
        String fullName = (firstname + " " + lastname).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        return "Без имени";
    }

    private LocalTime calculateEndTime(Appointment appointment) {
        int slotsCount = appointment.getSlotsCount() > 0 ? appointment.getSlotsCount() : 1;
        return appointment.getSlot().getStartTime().plusMinutes((long) slotsCount * SLOT_DURATION_MINUTES);
    }
}

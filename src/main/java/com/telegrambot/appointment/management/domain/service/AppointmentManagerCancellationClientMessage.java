package com.telegrambot.appointment.management.domain.service;

import com.telegrambot.appointment.management.domain.model.appointment.Appointment;

import java.time.format.DateTimeFormatter;

public final class AppointmentManagerCancellationClientMessage {

    private static final DateTimeFormatter DATE_FULL_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private AppointmentManagerCancellationClientMessage() {}

    public static String build(Appointment appointment) {
        return String.format("""
                ❌ Ваша запись отменена менеджером.
                
                👤 Специалист: %s %s
                💈 Услуга: %s
                📅 Дата: %s
                🕐 Время: %s
                
                Приносим извинения за неудобства.
                """,
                appointment.getSpecialist().getFirstname(),
                appointment.getSpecialist().getLastname(),
                appointment.getService().getName(),
                appointment.getSlot().getSchedule().getDate().format(DATE_FULL_FMT),
                appointment.getSlot().getStartTime().format(TIME_FMT)
        );
    }
}

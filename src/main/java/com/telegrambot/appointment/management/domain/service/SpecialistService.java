package com.telegrambot.appointment.management.domain.service;

import com.telegrambot.appointment.management.domain.model.appointment.Appointment;
import com.telegrambot.appointment.management.domain.model.appointment.AppointmentStatus;
import com.telegrambot.appointment.management.domain.model.user.specialist.Specialist;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.handler.AppointmentRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.specialist.SpecialistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SpecialistService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final SpecialistRepository specialistRepository;
    private final AppointmentRepository appointmentRepository;

    public SpecialistService(SpecialistRepository specialistRepository,
                             AppointmentRepository appointmentRepository) {
        this.specialistRepository = specialistRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public SendMessage buildAppointmentsMessage(Long telegramId, Long chatId) {
        Specialist specialist = specialistRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new IllegalStateException("Specialist not found: " + telegramId));

        List<Appointment> appointments = appointmentRepository.findBySpecialistIdAndStatus(
                specialist.getId(), AppointmentStatus.CONFIRMED);

        if (appointments.isEmpty()) {
            return new SendMessage(chatId.toString(), "📋 У вас нет активных записей.");
        }

        StringBuilder text = new StringBuilder("📋 *Ваши записи* (")
                .append(appointments.size()).append("):\n\n");

        for (int i = 0; i < appointments.size(); i++) {
            Appointment appointment = appointments.get(i);
            text.append(i + 1).append(". ")
                    .append(appointment.getSlot().getSchedule().getDate().format(DATE_FMT))
                    .append(" в ").append(appointment.getSlot().getStartTime().format(TIME_FMT))
                    .append("\n   👤 ").append(appointment.getClient().getFirstname())
                    .append(" ").append(appointment.getClient().getLastname())
                    .append("\n   💈 ").append(appointment.getService().getName())
                    .append("\n\n");
        }

        SendMessage message = new SendMessage(chatId.toString(), text.toString().trim());
        message.setParseMode("Markdown");
        return message;
    }
}
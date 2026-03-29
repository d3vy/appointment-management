package com.telegrambot.appointment.management.domain.service;

import com.telegrambot.appointment.management.domain.model.appointment.Appointment;
import com.telegrambot.appointment.management.domain.model.appointment.AppointmentStatus;
import com.telegrambot.appointment.management.domain.model.appointment.Schedule;
import com.telegrambot.appointment.management.domain.model.appointment.ScheduleSlot;
import com.telegrambot.appointment.management.domain.model.user.specialist.Specialist;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.appointment.AppointmentRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.appointment.ScheduleRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.specialist.SpecialistRepository;
import com.telegrambot.appointment.management.infrastructure.telegram.TelegramDisplayHtml;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
public class SpecialistService {

    private static final String CALLBACK_MAIN_MENU = "SPECIALIST_MAIN_MENU";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int SCHEDULE_DAYS_AHEAD = 7;

    private final SpecialistRepository specialistRepository;
    private final AppointmentRepository appointmentRepository;
    private final ScheduleRepository  scheduleRepository;

    public SpecialistService(SpecialistRepository specialistRepository,
                             AppointmentRepository appointmentRepository,
                             ScheduleRepository scheduleRepository) {
        this.specialistRepository = specialistRepository;
        this.appointmentRepository = appointmentRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Transactional(readOnly = true)
    public SendMessage buildMyServicesMessage(Long telegramId, Long chatId) {
        Specialist specialist = specialistRepository.findByTelegramIdWithServices(telegramId)
                .orElseThrow(() -> new IllegalStateException("Specialist not found: " + telegramId));

        var services = specialist.getServices().stream()
                .sorted(Comparator.comparing(s -> s.getName(), String.CASE_INSENSITIVE_ORDER))
                .toList();

        if (services.isEmpty()) {
            return withMainMenuButton(chatId, "💈 У вас пока нет привязанных услуг.");
        }

        StringBuilder text = new StringBuilder("💈 Ваши услуги (").append(services.size()).append("):\n\n");
        for (int i = 0; i < services.size(); i++) {
            var service = services.get(i);
            text.append(i + 1).append(". ").append(service.getName())
                    .append("\n   💰 ").append(service.getPrice().toPlainString()).append(" ₽")
                    .append("   ⏱ ").append(service.getDurationMinutes()).append(" мин\n\n");
        }
        return withMainMenuButton(chatId, text.toString().trim());
    }

    @Transactional(readOnly = true)
    public SendMessage buildAppointmentsMessage(Long telegramId, Long chatId) {
        Specialist specialist = specialistRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new IllegalStateException("Specialist not found: " + telegramId));

        List<Appointment> appointments = appointmentRepository.findBySpecialistIdAndStatus(
                specialist.getId(), AppointmentStatus.CONFIRMED);

        if (appointments.isEmpty()) {
            return withMainMenuButton(chatId, "📋 У вас нет активных записей.");
        }

        StringBuilder text = new StringBuilder("📋 <b>Ваши записи</b> (")
                .append(appointments.size()).append("):\n\n");

        for (int i = 0; i < appointments.size(); i++) {
            Appointment appointment = appointments.get(i);
            text.append(i + 1).append(". ")
                    .append(appointment.getSlot().getSchedule().getDate().format(DATE_FMT))
                    .append(" в ").append(appointment.getSlot().getStartTime().format(TIME_FMT))
                    .append("\n   👤 ").append(TelegramDisplayHtml.escape(appointment.getClient().getFirstname()))
                    .append(" ").append(TelegramDisplayHtml.escape(appointment.getClient().getLastname()))
                    .append("\n   💈 ").append(TelegramDisplayHtml.escape(appointment.getService().getName()))
                    .append("\n\n");
        }

        SendMessage message = new SendMessage(chatId.toString(), text.toString().trim());
        message.setParseMode("HTML");
        message.setReplyMarkup(mainMenuMarkup());
        return message;
    }

    @Transactional(readOnly = true)
    public SendMessage buildScheduleMessage(Long telegramId, Long chatId) {
        Specialist specialist = specialistRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new IllegalStateException("Specialist not found: " + telegramId));

        LocalDate today = LocalDate.now();
        List<Schedule> schedules = scheduleRepository.findBySpecialistIdWithSlots(
                specialist.getId(), today, today.plusDays(SCHEDULE_DAYS_AHEAD));

        if (schedules.isEmpty()) {
            return withMainMenuButton(chatId, "📆 Расписание на ближайшие 7 дней пусто.");
        }

        StringBuilder text = new StringBuilder("📆 <b>Ваше расписание на 7 дней:</b>\n\n");
        for (Schedule schedule : schedules) {
            text.append("📅 ").append(schedule.getDate().format(DATE_FMT)).append("\n");

            List<ScheduleSlot> slots = schedule.getSlots().stream()
                    .sorted(Comparator.comparing(ScheduleSlot::getStartTime))
                    .toList();

            for (ScheduleSlot slot : slots) {
                String status = slot.isBooked() ? "🔴" : "🟢";
                text.append("  ").append(status).append(" ")
                        .append(slot.getStartTime().format(TIME_FMT))
                        .append(" – ")
                        .append(slot.getEndTime().format(TIME_FMT))
                        .append("\n");
            }
            text.append("\n");
        }
        text.append("🟢 — свободно   🔴 — занято");

        SendMessage message = new SendMessage(chatId.toString(), text.toString().trim());
        message.setParseMode("HTML");
        message.setReplyMarkup(mainMenuMarkup());
        return message;
    }

    private static SendMessage withMainMenuButton(Long chatId, String text) {
        SendMessage message = new SendMessage(chatId.toString(), text);
        message.setReplyMarkup(mainMenuMarkup());
        return message;
    }

    private static InlineKeyboardMarkup mainMenuMarkup() {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("◀️ В меню");
        button.setCallbackData(CALLBACK_MAIN_MENU);
        return new InlineKeyboardMarkup(List.of(List.of(button)));
    }
}
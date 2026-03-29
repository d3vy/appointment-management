package com.telegrambot.appointment.management.domain.service;

import com.telegrambot.appointment.management.domain.model.appointment.Appointment;
import com.telegrambot.appointment.management.domain.model.appointment.AppointmentStatus;
import com.telegrambot.appointment.management.domain.model.appointment.Schedule;
import com.telegrambot.appointment.management.domain.model.appointment.ScheduleSlot;
import com.telegrambot.appointment.management.domain.model.user.manager.Manager;
import com.telegrambot.appointment.management.domain.model.user.manager.ManagerScheduleContext;
import com.telegrambot.appointment.management.domain.model.user.manager.ManagerScheduleStep;
import com.telegrambot.appointment.management.domain.model.user.specialist.Specialist;
import com.telegrambot.appointment.management.domain.port.TelegramTextMessageSender;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.appointment.AppointmentRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.appointment.ScheduleRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.appointment.ScheduleSlotRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.manager.ManagerRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.context.ManagerScheduleContextRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.specialist.SpecialistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ManagerScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ManagerScheduleService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM (EEE)", new Locale("ru"));
    private static final DateTimeFormatter DATE_FULL_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int SLOT_DURATION_MINUTES = 30;
    private static final String CALLBACK_MANAGER_MAIN_MENU = "MANAGER_MAIN_MENU";
    private static final String SCHED_BACK_TO_DATES = "SCHED_BACK_TO_DATES";
    private static final String SCHED_BACK_TO_TIME = "SCHED_BACK_TO_TIME";

    private final ManagerScheduleContextRepository scheduleContextRepository;
    private final SpecialistRepository specialistRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleSlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final ManagerRepository managerRepository;
    private final TelegramTextMessageSender messageSender;

    public ManagerScheduleService(ManagerScheduleContextRepository scheduleContextRepository,
                                  SpecialistRepository specialistRepository,
                                  ScheduleRepository scheduleRepository,
                                  ScheduleSlotRepository slotRepository,
                                  AppointmentRepository appointmentRepository,
                                  ManagerRepository managerRepository,
                                  TelegramTextMessageSender messageSender) {
        this.scheduleContextRepository = scheduleContextRepository;
        this.specialistRepository = specialistRepository;
        this.scheduleRepository = scheduleRepository;
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
        this.managerRepository = managerRepository;
        this.messageSender = messageSender;
    }

    public SendMessage startScheduleFlow(Long telegramId, Long chatId) {
        ManagerScheduleContext context = new ManagerScheduleContext();
        context.setTelegramId(telegramId);
        context.setStep(ManagerScheduleStep.SELECT_SPECIALIST);
        scheduleContextRepository.save(context);
        return buildSpecialistListMessage(chatId);
    }

    public boolean hasActiveContext(Long telegramId) {
        return scheduleContextRepository.existsById(telegramId);
    }

    public void clearScheduleContextIfPresent(Long telegramId) {
        if (scheduleContextRepository.existsById(telegramId)) {
            scheduleContextRepository.deleteById(telegramId);
        }
    }

    private SendMessage messageWithMenu(Long chatId, String text) {
        SendMessage message = new SendMessage(chatId.toString(), text);
        message.setReplyMarkup(new InlineKeyboardMarkup(List.of(
                List.of(btn("◀️ В меню", CALLBACK_MANAGER_MAIN_MENU)))));
        return message;
    }

    @Transactional
    public SendMessage handleCallback(Long telegramId, Long chatId, String data) {
        ManagerScheduleContext context = scheduleContextRepository.findById(telegramId).orElse(null);
        if (context == null) {
            return messageWithMenu(chatId, "Сессия истекла. Начните заново через /menu");
        }

        if ("SCHED_CANCEL".equals(data)) {
            scheduleContextRepository.deleteById(telegramId);
            return messageWithMenu(chatId, "Отменено.");
        }

        return switch (context.getStep()) {
            case SELECT_SPECIALIST -> handleSpecialistSelection(context, chatId, data);
            case SELECT_DATES -> handleDateToggle(context, chatId, data);
            case ENTER_WORKDAY_TIME -> handleEnterWorkdayTimeStep(context, chatId, data);
            case CONFIRM -> handleConfirmStep(context, chatId, telegramId, data);
            default -> messageWithMenu(chatId, "Неизвестный шаг.");
        };
    }

    @Transactional
    public SendMessage handleTextInput(Long telegramId, Long chatId, String text) {
        ManagerScheduleContext context = scheduleContextRepository.findById(telegramId).orElse(null);
        if (context == null || context.getStep() != ManagerScheduleStep.ENTER_WORKDAY_TIME) {
            return null;
        }
        return handleWorkdayTimeInput(context, chatId, text);
    }

    private SendMessage handleSpecialistSelection(ManagerScheduleContext context, Long chatId, String data) {
        if (!data.startsWith("SCHED_SPEC_")) return unknownAction(chatId);
        int specialistId = Integer.parseInt(data.replace("SCHED_SPEC_", ""));
        context.setSelectedSpecialistId(specialistId);
        context.setStep(ManagerScheduleStep.SELECT_DATES);
        scheduleContextRepository.save(context);
        return buildDateSelectionMessage(chatId, context);
    }

    private SendMessage handleDateToggle(ManagerScheduleContext context, Long chatId, String data) {
        if ("SCHED_DATES_DONE".equals(data)) {
            if (context.getSelectedDates().isEmpty()) {
                return buildDateSelectionMessage(chatId, context,
                        "⚠️ Выберите хотя бы один день.\n\n");
            }
            context.setStep(ManagerScheduleStep.ENTER_WORKDAY_TIME);
            scheduleContextRepository.save(context);
            return workdayTimePrompt(chatId);
        }

        if (!data.startsWith("SCHED_DATE_")) return unknownAction(chatId);
        String dateStr = data.replace("SCHED_DATE_", "");
        LocalDate date = LocalDate.parse(dateStr);

        List<LocalDate> selected = new ArrayList<>(context.getSelectedDates());
        if (selected.contains(date)) {
            selected.remove(date);
        } else {
            selected.add(date);
        }
        context.setSelectedDates(selected);
        scheduleContextRepository.save(context);
        return buildDateSelectionMessage(chatId, context);
    }

    private SendMessage handleWorkdayTimeInput(ManagerScheduleContext context, Long chatId, String text) {
        WorkdayRange range = parseWorkdayRange(text.trim());
        if (range == null) {
            return workdayTimePrompt(chatId,
                    "❌ Неверный формат. Пример: 09:00-18:00\n\n"
                            + "Введите рабочее время в формате ЧЧ:ММ-ЧЧ:ММ\nПример: 09:00-18:00");
        }
        context.setWorkdayInput(text.trim());
        context.setStep(ManagerScheduleStep.CONFIRM);
        scheduleContextRepository.save(context);
        return buildConfirmationMessage(chatId, context, range);
    }

    private SendMessage handleEnterWorkdayTimeStep(ManagerScheduleContext context, Long chatId, String data) {
        if (SCHED_BACK_TO_DATES.equals(data)) {
            context.setStep(ManagerScheduleStep.SELECT_DATES);
            context.setWorkdayInput(null);
            scheduleContextRepository.save(context);
            return buildDateSelectionMessage(chatId, context);
        }
        return unknownAction(chatId);
    }

    private SendMessage handleConfirmStep(ManagerScheduleContext context, Long chatId, Long telegramId, String data) {
        if (SCHED_BACK_TO_TIME.equals(data)) {
            context.setStep(ManagerScheduleStep.ENTER_WORKDAY_TIME);
            scheduleContextRepository.save(context);
            String current = context.getWorkdayInput();
            String hintPrefix = (current != null && !current.isBlank())
                    ? "Текущее время: " + current + "\n\n"
                    : "";
            return workdayTimePrompt(chatId,
                    hintPrefix + "Введите рабочее время в формате ЧЧ:ММ-ЧЧ:ММ\nПример: 09:00-18:00");
        }
        if (!"SCHED_CONFIRM".equals(data)) {
            return unknownAction(chatId);
        }
        return executeScheduleCreation(context, chatId, telegramId);
    }

    private SendMessage executeScheduleCreation(ManagerScheduleContext context, Long chatId, Long telegramId) {
        WorkdayRange range = parseWorkdayRange(context.getWorkdayInput());
        if (range == null) {
            scheduleContextRepository.deleteById(telegramId);
            return messageWithMenu(chatId, "Ошибка данных. Начните заново.");
        }

        Manager manager = managerRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new IllegalStateException("Manager not found: " + telegramId));
        Specialist specialist = specialistRepository.findById(context.getSelectedSpecialistId())
                .orElseThrow();

        int createdCount = 0;
        int skippedCount = 0;

        for (LocalDate date : context.getSelectedDates()) {
            Optional<Schedule> existing = scheduleRepository.findBySpecialistIdAndDate(specialist.getId(), date);
            if (existing.isPresent()) {
                skippedCount++;
                continue;
            }
            Schedule schedule = new Schedule();
            schedule.setSpecialist(specialist);
            schedule.setDate(date);
            schedule.setCreatedBy(manager);
            scheduleRepository.save(schedule);
            generateSlots(schedule, range);
            createdCount++;
        }

        scheduleContextRepository.deleteById(telegramId);
        log.info("Schedule created: specialistId={}, days={}, skipped={}",
                specialist.getId(), createdCount, skippedCount);

        String result = String.format("✅ Создано расписаний: %d", createdCount);
        if (skippedCount > 0) {
            result += String.format("\n⚠️ Пропущено (уже существует): %d", skippedCount);
        }
        return messageWithMenu(chatId, result);
    }

    private void generateSlots(Schedule schedule, WorkdayRange range) {
        List<ScheduleSlot> slots = new ArrayList<>();
        LocalTime current = range.start();
        while (!current.plusMinutes(SLOT_DURATION_MINUTES).isAfter(range.end())) {
            slots.add(new ScheduleSlot(schedule, current, current.plusMinutes(SLOT_DURATION_MINUTES)));
            current = current.plusMinutes(SLOT_DURATION_MINUTES);
        }
        slotRepository.saveAll(slots);
    }

    @Transactional(readOnly = true)
    public SendMessage buildSpecialistScheduleMessage(Long chatId, Integer specialistId) {
        Specialist specialist = specialistRepository.findById(specialistId).orElseThrow();
        LocalDate today = LocalDate.now();
        List<Schedule> schedules = scheduleRepository.findBySpecialistIdWithSlots(
                specialist.getId(), today, today.plusDays(7));

        StringBuilder text = new StringBuilder("📆 *Расписание: ")
                .append(specialist.getFirstname()).append(" ").append(specialist.getLastname())
                .append("*\n\n");

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        if (schedules.isEmpty()) {
            text.append("Расписание не задано.");
        } else {
            for (Schedule schedule : schedules) {
                List<ScheduleSlot> slots = schedule.getSlots().stream()
                        .sorted(Comparator.comparing(ScheduleSlot::getStartTime))
                        .toList();

                long freeCount = slots.stream().filter(s -> !s.isBooked()).count();
                long bookedCount = slots.stream().filter(ScheduleSlot::isBooked).count();

                text.append("📅 ").append(schedule.getDate().format(DATE_FULL_FMT))
                        .append(" | 🟢 ").append(freeCount)
                        .append(" свободно | 🔴 ").append(bookedCount).append(" занято\n");

                rows.add(List.of(
                        btn("✏️ " + schedule.getDate().format(DATE_FULL_FMT), "SCHED_DAY_" + schedule.getId()),
                        btn("🗑", "SCHED_DEL_DAY_" + schedule.getId())
                ));
            }
        }

        rows.add(List.of(btn("➕ Добавить дни", "SCHED_ADD_" + specialistId)));
        rows.add(List.of(btn("◀️ Назад", "SCHED_BACK_TO_SPECIALISTS")));

        SendMessage message = new SendMessage(chatId.toString(), text.toString().trim());
        message.setParseMode("Markdown");
        message.setReplyMarkup(new InlineKeyboardMarkup(rows));
        return message;
    }

    @Transactional(readOnly = true)
    public SendMessage buildDayDetailMessage(Long chatId, Integer scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow();
        List<ScheduleSlot> slots = slotRepository.findAllByScheduleIdOrdered(scheduleId);

        StringBuilder text = new StringBuilder("📅 *")
                .append(schedule.getDate().format(DATE_FULL_FMT)).append("*\n")
                .append(schedule.getSpecialist().getFirstname()).append(" ")
                .append(schedule.getSpecialist().getLastname()).append("\n\n");

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (ScheduleSlot slot : slots) {
            String status = slot.isBooked() ? "🔴" : "🟢";
            String label = status + " " + slot.getStartTime().format(TIME_FMT)
                    + "–" + slot.getEndTime().format(TIME_FMT);
            rows.add(List.of(btn(label, "SCHED_SLOT_" + slot.getId())));
            text.append(label).append("\n");
        }

        rows.add(List.of(btn("◀️ Назад", "SCHED_BACK_TO_DAY_" + schedule.getSpecialist().getId())));

        SendMessage message = new SendMessage(chatId.toString(), text.toString().trim());
        message.setParseMode("Markdown");
        message.setReplyMarkup(new InlineKeyboardMarkup(rows));
        return message;
    }

    @Transactional
    public SendMessage deleteDayWithCheck(Long telegramId, Long chatId, Integer scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow();
        List<Appointment> activeAppointments = appointmentRepository
                .findConfirmedByScheduleId(scheduleId);

        if (activeAppointments.isEmpty()) {
            scheduleRepository.deleteById(scheduleId);
            return buildSpecialistScheduleMessage(chatId, schedule.getSpecialist().getId());
        }

        List<List<InlineKeyboardButton>> rows = List.of(
                List.of(
                        btn("✅ Да, удалить", "SCHED_DEL_CONFIRM_" + scheduleId),
                        btn("❌ Отмена", "SCHED_BACK_TO_DAY_" + schedule.getSpecialist().getId())
                )
        );

        SendMessage message = new SendMessage(chatId.toString(),
                "⚠️ На этот день есть " + activeAppointments.size() +
                        " активных записей.\nКлиенты получат уведомление об отмене.\nУдалить расписание?");
        message.setReplyMarkup(new InlineKeyboardMarkup(rows));
        return message;
    }

    @Transactional
    public SendMessage confirmDeleteDay(Long chatId, Integer scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow();
        List<Appointment> activeAppointments = appointmentRepository.findConfirmedByScheduleId(scheduleId);

        for (Appointment appointment : activeAppointments) {
            appointment.setStatus(AppointmentStatus.CANCELLED);
            notifyClientAboutCancellation(appointment);
        }

        appointmentRepository.saveAll(activeAppointments);
        Integer specialistId = schedule.getSpecialist().getId();
        scheduleRepository.deleteById(scheduleId);

        log.info("Schedule deleted: scheduleId={}, cancelledAppointments={}", scheduleId, activeAppointments.size());
        return buildSpecialistScheduleMessage(chatId, specialistId);
    }

    public SendMessage startAddDaysFlow(Long telegramId, Long chatId, Integer specialistId) {
        ManagerScheduleContext context = new ManagerScheduleContext();
        context.setTelegramId(telegramId);
        context.setSelectedSpecialistId(specialistId);
        context.setStep(ManagerScheduleStep.SELECT_DATES);
        scheduleContextRepository.save(context);
        return buildDateSelectionMessage(chatId, context);
    }

    private void notifyClientAboutCancellation(Appointment appointment) {
        String text = String.format("""
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
        try {
            messageSender.sendText(appointment.getClient().getTelegramId(), text);
        } catch (Exception e) {
            log.error("Failed to notify client telegramId={} about cancellation",
                    appointment.getClient().getTelegramId(), e);
        }
    }

    private SendMessage buildSpecialistListMessage(Long chatId) {
        List<Specialist> specialists = specialistRepository.findAll();
        if (specialists.isEmpty()) {
            return messageWithMenu(chatId, "Специалистов нет.");
        }
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Specialist sp : specialists) {
            rows.add(List.of(btn(
                    sp.getFirstname() + " " + sp.getLastname(),
                    "SCHED_SPEC_" + sp.getId()
            )));
        }
        rows.add(List.of(btn("❌ Отмена", "SCHED_CANCEL")));
        SendMessage message = new SendMessage(chatId.toString(), "Выберите специалиста:");
        message.setReplyMarkup(new InlineKeyboardMarkup(rows));
        return message;
    }

    private SendMessage buildDateSelectionMessage(Long chatId, ManagerScheduleContext context) {
        return buildDateSelectionMessage(chatId, context, "");
    }

    private SendMessage buildDateSelectionMessage(Long chatId, ManagerScheduleContext context, String textPrefix) {
        List<LocalDate> selected = context.getSelectedDates();
        LocalDate today = LocalDate.now();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            boolean isSelected = selected.contains(date);
            String label = (isSelected ? "✅ " : "") + date.format(DATE_FMT);
            rows.add(List.of(btn(label, "SCHED_DATE_" + date)));
        }

        if (!selected.isEmpty()) {
            rows.add(List.of(btn("➡️ Далее (" + selected.size() + " дн.)", "SCHED_DATES_DONE")));
        }
        rows.add(List.of(btn("❌ Отмена", "SCHED_CANCEL")));

        String text = textPrefix + "Выберите дни (можно несколько):";
        SendMessage message = new SendMessage(chatId.toString(), text);
        message.setReplyMarkup(new InlineKeyboardMarkup(rows));
        return message;
    }

    private SendMessage workdayTimePrompt(Long chatId) {
        return workdayTimePrompt(chatId,
                "Введите рабочее время в формате ЧЧ:ММ-ЧЧ:ММ\nПример: 09:00-18:00");
    }

    private SendMessage workdayTimePrompt(Long chatId, String text) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(btn("◀️ К дням", SCHED_BACK_TO_DATES)));
        rows.add(List.of(btn("❌ Отмена", "SCHED_CANCEL"), btn("◀️ В меню", CALLBACK_MANAGER_MAIN_MENU)));
        SendMessage message = new SendMessage(chatId.toString(), text);
        message.setReplyMarkup(new InlineKeyboardMarkup(rows));
        return message;
    }

    private SendMessage buildConfirmationMessage(Long chatId, ManagerScheduleContext context, WorkdayRange range) {
        Specialist specialist = specialistRepository.findById(context.getSelectedSpecialistId()).orElseThrow();
        int slotCount = (int) (Duration.between(range.start(), range.end()).toMinutes() / SLOT_DURATION_MINUTES);

        StringBuilder text = new StringBuilder("Подтвердите создание расписания:\n\n");
        text.append("👤 ").append(specialist.getFirstname()).append(" ").append(specialist.getLastname()).append("\n");
        text.append("🕐 ").append(range.start().format(TIME_FMT)).append("–").append(range.end().format(TIME_FMT)).append("\n");
        text.append("📊 Слотов в день: ").append(slotCount).append(" (по 30 мин)\n\n");
        text.append("📅 Дни:\n");
        context.getSelectedDates().stream()
                .sorted()
                .forEach(d -> text.append("  • ").append(d.format(DATE_FULL_FMT)).append("\n"));

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(btn("✅ Создать", "SCHED_CONFIRM"), btn("❌ Отмена", "SCHED_CANCEL")));
        rows.add(List.of(btn("◀️ Изменить время", SCHED_BACK_TO_TIME)));
        rows.add(List.of(btn("◀️ В меню", CALLBACK_MANAGER_MAIN_MENU)));
        SendMessage message = new SendMessage(chatId.toString(), text.toString());
        message.setReplyMarkup(new InlineKeyboardMarkup(rows));
        return message;
    }

    private WorkdayRange parseWorkdayRange(String input) {
        if (input == null) return null;
        String[] parts = input.split("-");
        if (parts.length != 2) return null;
        try {
            LocalTime start = LocalTime.parse(parts[0].trim(), TIME_FMT);
            LocalTime end = LocalTime.parse(parts[1].trim(), TIME_FMT);
            if (!end.isAfter(start)) return null;
            return new WorkdayRange(start, end);
        } catch (Exception e) {
            return null;
        }
    }

    private InlineKeyboardButton btn(String text, String callbackData) {
        InlineKeyboardButton b = new InlineKeyboardButton();
        b.setText(text);
        b.setCallbackData(callbackData);
        return b;
    }

    private SendMessage unknownAction(Long chatId) {
        return messageWithMenu(chatId, "Неизвестное действие.");
    }

    private record WorkdayRange(LocalTime start, LocalTime end) {}
}

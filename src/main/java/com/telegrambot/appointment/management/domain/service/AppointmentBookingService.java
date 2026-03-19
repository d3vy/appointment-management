package com.telegrambot.appointment.management.domain.service;

import com.telegrambot.appointment.management.domain.model.appointment.*;
import com.telegrambot.appointment.management.domain.model.appointment.booking.BookingContext;
import com.telegrambot.appointment.management.domain.model.appointment.booking.BookingPath;
import com.telegrambot.appointment.management.domain.model.appointment.booking.BookingStep;
import com.telegrambot.appointment.management.domain.model.user.client.Client;
import com.telegrambot.appointment.management.domain.model.user.specialist.Specialist;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.client.ClientRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.handler.*;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.specialist.SpecialistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Service
public class AppointmentBookingService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentBookingService.class);
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd.MM (EEE)");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final BookingContextRepository bookingContextRepository;
    private final ServiceRepository serviceRepository;
    private final SpecialistRepository specialistRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleSlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClientRepository clientRepository;

    public AppointmentBookingService(BookingContextRepository bookingContextRepository,
                                     ServiceRepository serviceRepository,
                                     SpecialistRepository specialistRepository,
                                     ScheduleRepository scheduleRepository,
                                     ScheduleSlotRepository slotRepository,
                                     AppointmentRepository appointmentRepository,
                                     ClientRepository clientRepository) {
        this.bookingContextRepository = bookingContextRepository;
        this.serviceRepository = serviceRepository;
        this.specialistRepository = specialistRepository;
        this.scheduleRepository = scheduleRepository;
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
        this.clientRepository = clientRepository;
    }

    public SendMessage startBooking(Long telegramId, Long chatId) {
        BookingContext context = new BookingContext();
        context.setTelegramId(telegramId);
        context.setStep(BookingStep.SELECT_PATH);
        bookingContextRepository.save(context);

        InlineKeyboardButton byService    = btn("🔍 Выбрать услугу",      "BOOK_PATH_SERVICE");
        InlineKeyboardButton bySpecialist = btn("👤 Выбрать специалиста", "BOOK_PATH_SPECIALIST");
        InlineKeyboardButton cancel       = btn("❌ Отмена",               "BOOK_CANCEL");

        return buildMessage(chatId, "Как хотите записаться?", List.of(
                List.of(byService),
                List.of(bySpecialist),
                List.of(cancel)
        ));
    }

    @Transactional
    public SendMessage handleCallback(Long telegramId, Long chatId, String data) {
        if ("BOOK_CANCEL".equals(data)) {
            bookingContextRepository.deleteById(telegramId);
            return new SendMessage(chatId.toString(), "Запись отменена.");
        }

        BookingContext context = bookingContextRepository.findById(telegramId).orElse(null);
        if (context == null) {
            return new SendMessage(chatId.toString(), "Сессия истекла. Начните заново: /make_appointment");
        }

        return switch (context.getStep()) {
            case SELECT_PATH              -> handlePathSelection(context, chatId, data);
            case SELECT_SERVICE           -> handleServiceSelection(context, chatId, data);
            case SELECT_SPECIALIST        -> handleSpecialistSelection(context, chatId, data);
            case SELECT_SPECIALIST_SERVICE -> handleSpecialistServiceSelection(context, chatId, data);
            case SELECT_DAY               -> handleDaySelection(context, chatId, data);
            case SELECT_SLOT              -> handleSlotSelection(context, chatId, data);
            case CONFIRM                  -> handleConfirmation(context, chatId, data);
            default -> new SendMessage(chatId.toString(), "Неизвестный шаг. Начните заново: /make_appointment");
        };
    }

    public boolean isBooking(Long telegramId) {
        return bookingContextRepository.existsById(telegramId);
    }

    private SendMessage handlePathSelection(BookingContext context, Long chatId, String data) {
        return switch (data) {
            case "BOOK_PATH_SERVICE" -> {
                context.setPath(BookingPath.BY_SERVICE);
                context.setStep(BookingStep.SELECT_SERVICE);
                bookingContextRepository.save(context);
                yield showServices(chatId);
            }
            case "BOOK_PATH_SPECIALIST" -> {
                context.setPath(BookingPath.BY_SPECIALIST);
                context.setStep(BookingStep.SELECT_SPECIALIST);
                bookingContextRepository.save(context);
                yield showAllSpecialists(chatId);
            }
            default -> new SendMessage(chatId.toString(), "Выберите вариант из меню.");
        };
    }

    private SendMessage handleServiceSelection(BookingContext context, Long chatId, String data) {
        if (!data.startsWith("BOOK_SRV_")) return unknownStep(chatId);
        int serviceId = Integer.parseInt(data.replace("BOOK_SRV_", ""));
        context.setSelectedServiceId(serviceId);
        context.setStep(BookingStep.SELECT_SPECIALIST);
        bookingContextRepository.save(context);
        return showSpecialistsByService(chatId, serviceId);
    }

    private SendMessage handleSpecialistSelection(BookingContext context, Long chatId, String data) {
        if (!data.startsWith("BOOK_SPEC_")) return unknownStep(chatId);
        int specialistId = Integer.parseInt(data.replace("BOOK_SPEC_", ""));
        context.setSelectedSpecialistId(specialistId);

        if (context.getPath() == BookingPath.BY_SPECIALIST) {
            context.setStep(BookingStep.SELECT_SPECIALIST_SERVICE);
            bookingContextRepository.save(context);
            return showServicesBySpecialist(chatId, specialistId);
        } else {
            context.setStep(BookingStep.SELECT_DAY);
            bookingContextRepository.save(context);
            return showAvailableDays(chatId, specialistId);
        }
    }

    private SendMessage handleSpecialistServiceSelection(BookingContext context, Long chatId, String data) {
        if (!data.startsWith("BOOK_SRV_")) return unknownStep(chatId);
        int serviceId = Integer.parseInt(data.replace("BOOK_SRV_", ""));
        context.setSelectedServiceId(serviceId);
        context.setStep(BookingStep.SELECT_DAY);
        bookingContextRepository.save(context);
        return showAvailableDays(chatId, context.getSelectedSpecialistId());
    }

    private SendMessage handleDaySelection(BookingContext context, Long chatId, String data) {
        if (!data.startsWith("BOOK_DAY_")) return unknownStep(chatId);
        int scheduleId = Integer.parseInt(data.replace("BOOK_DAY_", ""));
        context.setSelectedScheduleId(scheduleId);
        context.setStep(BookingStep.SELECT_SLOT);
        bookingContextRepository.save(context);
        return showAvailableSlots(chatId, scheduleId);
    }

    private SendMessage handleSlotSelection(BookingContext context, Long chatId, String data) {
        if (!data.startsWith("BOOK_SLOT_")) return unknownStep(chatId);
        int slotId = Integer.parseInt(data.replace("BOOK_SLOT_", ""));
        context.setSelectedSlotId(slotId);
        context.setStep(BookingStep.CONFIRM);
        bookingContextRepository.save(context);
        return showConfirmation(chatId, context);
    }

    private SendMessage handleConfirmation(BookingContext context, Long chatId, String data) {
        if (!"BOOK_CONFIRM".equals(data)) return unknownStep(chatId);

        Client client = clientRepository.findByTelegramId(context.getTelegramId())
                .orElseThrow(() -> new IllegalStateException("Client not found: " + context.getTelegramId()));
        Specialist specialist = specialistRepository.findById(context.getSelectedSpecialistId()).orElseThrow();
        Service service = serviceRepository.findById(context.getSelectedServiceId()).orElseThrow();
        ScheduleSlot slot = slotRepository.findById(context.getSelectedSlotId()).orElseThrow();

        if (slot.isBooked()) {
            bookingContextRepository.deleteById(context.getTelegramId());
            return new SendMessage(chatId.toString(),
                    "⚠️ Этот слот уже занят. Попробуйте снова: /make_appointment");
        }

        slot.setBooked(true);
        slotRepository.save(slot);

        Appointment appointment = new Appointment();
        appointment.setClient(client);
        appointment.setSpecialist(specialist);
        appointment.setService(service);
        appointment.setSlot(slot);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointmentRepository.save(appointment);

        bookingContextRepository.deleteById(context.getTelegramId());
        log.info("Appointment created: clientId={}, specialistId={}, slotId={}",
                client.getId(), specialist.getId(), slot.getId());

        String text = String.format("""
                ✅ Запись подтверждена!
                
                👤 Специалист: %s %s
                💈 Услуга: %s
                📅 Дата: %s
                🕐 Время: %s
                💰 Стоимость: %s ₽
                """,
                specialist.getFirstname(), specialist.getLastname(),
                service.getName(),
                slot.getSchedule().getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                slot.getStartTime().format(TIME_FMT),
                service.getPrice().toPlainString()
        );
        return new SendMessage(chatId.toString(), text);
    }

    private SendMessage showServices(Long chatId) {
        List<Service> services = serviceRepository.findAll();
        if (services.isEmpty()) return new SendMessage(chatId.toString(), "Нет доступных услуг.");

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Service s : services) {
            rows.add(List.of(btn(
                    String.format("%s — %s мин, %s ₽", s.getName(), s.getDurationMinutes(), s.getPrice().toPlainString()),
                    "BOOK_SRV_" + s.getId()
            )));
        }
        rows.add(List.of(btn("❌ Отмена", "BOOK_CANCEL")));
        return buildMessage(chatId, "Выберите услугу:", rows);
    }

    private SendMessage showAllSpecialists(Long chatId) {
        List<Specialist> specialists = specialistRepository.findAll();
        if (specialists.isEmpty()) return new SendMessage(chatId.toString(), "Нет доступных специалистов.");
        return buildSpecialistList(chatId, specialists, "Выберите специалиста:");
    }

    private SendMessage showSpecialistsByService(Long chatId, Integer serviceId) {
        List<Specialist> specialists = specialistRepository.findAllByServiceId(serviceId);
        if (specialists.isEmpty()) {
            return new SendMessage(chatId.toString(),
                    "По этой услуге нет доступных специалистов. Выберите другую: /make_appointment");
        }
        return buildSpecialistList(chatId, specialists, "Выберите специалиста:");
    }

    private SendMessage showServicesBySpecialist(Long chatId, Integer specialistId) {
        Specialist specialist = specialistRepository.findById(specialistId).orElseThrow();
        List<Service> services = new ArrayList<>(specialist.getServices());
        if (services.isEmpty()) return new SendMessage(chatId.toString(), "У специалиста нет услуг.");

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Service s : services) {
            rows.add(List.of(btn(
                    String.format("%s — %s мин, %s ₽", s.getName(), s.getDurationMinutes(), s.getPrice().toPlainString()),
                    "BOOK_SRV_" + s.getId()
            )));
        }
        rows.add(List.of(btn("❌ Отмена", "BOOK_CANCEL")));
        return buildMessage(chatId, "Выберите услугу:", rows);
    }

    private SendMessage showAvailableDays(Long chatId, Integer specialistId) {
        List<Schedule> schedules = scheduleRepository.findAvailableBySpecialist(specialistId, LocalDate.now());
        if (schedules.isEmpty()) {
            return new SendMessage(chatId.toString(),
                    "У этого специалиста нет свободных дней. Выберите другого: /make_appointment");
        }
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Schedule sc : schedules) {
            rows.add(List.of(btn(sc.getDate().format(DAY_FMT), "BOOK_DAY_" + sc.getId())));
        }
        rows.add(List.of(btn("❌ Отмена", "BOOK_CANCEL")));
        return buildMessage(chatId, "Выберите день:", rows);
    }

    private SendMessage showAvailableSlots(Long chatId, Integer scheduleId) {
        List<ScheduleSlot> slots = slotRepository.findFreeByScheduleId(scheduleId);
        if (slots.isEmpty()) {
            return new SendMessage(chatId.toString(), "В этот день нет свободных слотов. Выберите другой день.");
        }
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (ScheduleSlot sl : slots) {
            String label = String.format("%s – %s", sl.getStartTime().format(TIME_FMT), sl.getEndTime().format(TIME_FMT));
            rows.add(List.of(btn(label, "BOOK_SLOT_" + sl.getId())));
        }
        rows.add(List.of(btn("❌ Отмена", "BOOK_CANCEL")));
        return buildMessage(chatId, "Выберите время:", rows);
    }

    private SendMessage showConfirmation(Long chatId, BookingContext context) {
        Specialist specialist = specialistRepository.findById(context.getSelectedSpecialistId()).orElseThrow();
        Service service = serviceRepository.findById(context.getSelectedServiceId()).orElseThrow();
        ScheduleSlot slot = slotRepository.findById(context.getSelectedSlotId()).orElseThrow();

        String text = String.format("""
                Подтвердите запись:
                
                👤 Специалист: %s %s
                💈 Услуга: %s (%s мин)
                📅 Дата: %s
                🕐 Время: %s
                💰 Стоимость: %s ₽
                """,
                specialist.getFirstname(), specialist.getLastname(),
                service.getName(), service.getDurationMinutes(),
                slot.getSchedule().getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                slot.getStartTime().format(TIME_FMT),
                service.getPrice().toPlainString()
        );
        return buildMessage(chatId, text, List.of(
                List.of(btn("✅ Подтвердить", "BOOK_CONFIRM")),
                List.of(btn("❌ Отмена",      "BOOK_CANCEL"))
        ));
    }

    private SendMessage buildSpecialistList(Long chatId, List<Specialist> specialists, String header) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Specialist sp : specialists) {
            rows.add(List.of(btn(sp.getFirstname() + " " + sp.getLastname(), "BOOK_SPEC_" + sp.getId())));
        }
        rows.add(List.of(btn("❌ Отмена", "BOOK_CANCEL")));
        return buildMessage(chatId, header, rows);
    }

    private SendMessage buildMessage(Long chatId, String text, List<List<InlineKeyboardButton>> rows) {
        SendMessage msg = new SendMessage(chatId.toString(), text);
        msg.setReplyMarkup(new InlineKeyboardMarkup(rows));
        return msg;
    }

    private InlineKeyboardButton btn(String text, String callbackData) {
        InlineKeyboardButton b = new InlineKeyboardButton();
        b.setText(text);
        b.setCallbackData(callbackData);
        return b;
    }

    private SendMessage unknownStep(Long chatId) {
        return new SendMessage(chatId.toString(), "Неизвестное действие. Начните заново: /make_appointment");
    }
}

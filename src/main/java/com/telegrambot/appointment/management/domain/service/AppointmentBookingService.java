package com.telegrambot.appointment.management.domain.service;

import com.telegrambot.appointment.management.domain.model.appointment.*;
import com.telegrambot.appointment.management.domain.model.appointment.booking.BookingContext;
import com.telegrambot.appointment.management.domain.model.appointment.booking.BookingPath;
import com.telegrambot.appointment.management.domain.model.appointment.booking.BookingStep;
import com.telegrambot.appointment.management.domain.model.user.client.Client;
import com.telegrambot.appointment.management.domain.model.user.specialist.Specialist;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.appointment.AppointmentRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.appointment.ScheduleRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.appointment.ScheduleSlotRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.appointment.ServiceRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.client.ClientRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.context.BookingContextRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.specialist.SpecialistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.LocalTime;
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
        return buildPathSelectionMessage(chatId);
    }

    @Transactional
    public SendMessage handleCallback(Long telegramId, Long chatId, String data) {
        if ("BOOK_CANCEL".equals(data)) {
            bookingContextRepository.deleteById(telegramId);
            return buildMessage(chatId, "Запись отменена.",
                    List.of(List.of(btn("◀️ В меню", "CLIENT_MAIN_MENU"))));
        }

        BookingContext context = bookingContextRepository.findById(telegramId).orElse(null);
        if (context == null) {
            return buildMessage(chatId, "Сессия истекла. Начните заново: /make_appointment",
                    List.of(List.of(btn("◀️ В меню", "CLIENT_MAIN_MENU"))));
        }

        if ("BOOK_NAV_BACK".equals(data)) {
            return navigateBack(context, chatId);
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

    @Transactional(readOnly = true)
    public SendMessage buildClientAppointmentsMessage(Long telegramId, Long chatId) {
        Client client = clientRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new IllegalStateException("Client not found: " + telegramId));

        List<Appointment> appointments = appointmentRepository.findByClientIdAndStatus(
                client.getId(), AppointmentStatus.CONFIRMED);

        if (appointments.isEmpty()) {
            return buildMessage(chatId, "📋 У вас нет активных записей.",
                    List.of(List.of(btn("◀️ В меню", "CLIENT_MAIN_MENU"))));
        }

        StringBuilder text = new StringBuilder("📋 *Ваши записи* (")
                .append(appointments.size()).append("):\n\n");

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (int i = 0; i < appointments.size(); i++) {
            Appointment appointment = appointments.get(i);
            ScheduleSlot slot = appointment.getSlot();
            text.append(i + 1).append(". ")
                    .append(slot.getSchedule().getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                    .append(" в ").append(slot.getStartTime().format(TIME_FMT))
                    .append("\n   👤 ").append(appointment.getSpecialist().getFirstname())
                    .append(" ").append(appointment.getSpecialist().getLastname())
                    .append("\n   💈 ").append(appointment.getService().getName())
                    .append(" — ").append(appointment.getService().getPrice().toPlainString()).append(" ₽")
                    .append("\n\n");

            rows.add(List.of(btn(
                    "❌ Отменить запись #" + (i + 1),
                    "CANCEL_APPT_" + appointment.getId()
            )));
        }

        rows.add(List.of(btn("◀️ В меню", "CLIENT_MAIN_MENU")));

        SendMessage message = new SendMessage(chatId.toString(), text.toString().trim());
        message.setParseMode("Markdown");
        message.setReplyMarkup(new InlineKeyboardMarkup(rows));
        return message;
    }

    @Transactional
    public SendMessage cancelAppointment(Long telegramId, Integer appointmentId, Long chatId) {
        Appointment appointment = appointmentRepository.findByIdWithSlotAndBookedSlots(appointmentId).orElse(null);

        List<List<InlineKeyboardButton>> menuRow = List.of(List.of(btn("◀️ В меню", "CLIENT_MAIN_MENU")));

        if (appointment == null || !appointment.getClient().getTelegramId().equals(telegramId)) {
            return buildMessage(chatId, "⚠️ Запись не найдена.", menuRow);
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            return buildMessage(chatId, "⚠️ Эта запись уже отменена.", menuRow);
        }

        releaseSlots(appointment);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);

        return buildMessage(chatId, "✅ Запись отменена.", menuRow);
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
        return showAvailableSlots(chatId, scheduleId, context.getSelectedServiceId());
    }

    private SendMessage handleSlotSelection(BookingContext context, Long chatId, String data) {
        if (!data.startsWith("BOOK_SLOT_")) return unknownStep(chatId);
        int slotId = Integer.parseInt(data.replace("BOOK_SLOT_", ""));
        context.setSelectedSlotId(slotId);
        context.setStep(BookingStep.CONFIRM);
        bookingContextRepository.save(context);
        return showConfirmation(chatId, context);
    }

    @Transactional
    public SendMessage handleConfirmation(BookingContext context, Long chatId, String data) {
        if (!"BOOK_CONFIRM".equals(data)) return unknownStep(chatId);

        Client client = clientRepository.findByTelegramId(context.getTelegramId())
                .orElseThrow(() -> new IllegalStateException("Client not found: " + context.getTelegramId()));
        Specialist specialist = specialistRepository.findById(context.getSelectedSpecialistId()).orElseThrow();
        Service service = serviceRepository.findById(context.getSelectedServiceId()).orElseThrow();

        int slotsNeeded = (int) Math.ceil((double) service.getDurationMinutes() / 30);
        List<ScheduleSlot> allSlots = slotRepository.findAllByScheduleIdOrderedForUpdate(context.getSelectedScheduleId());

        ScheduleSlot startSlot = allSlots.stream()
                .filter(s -> s.getId().equals(context.getSelectedSlotId()))
                .findFirst()
                .orElseThrow();

        int startIndex = allSlots.indexOf(startSlot);
        if (!isConsecutiveFreeBlock(allSlots, startIndex, slotsNeeded)) {
            bookingContextRepository.deleteById(context.getTelegramId());
            return new SendMessage(chatId.toString(),
                    "⚠️ Время уже занято. Попробуйте снова: /menu");
        }

        List<ScheduleSlot> slotsToBook = new ArrayList<>(allSlots.subList(startIndex, startIndex + slotsNeeded));
        slotsToBook.forEach(s -> s.setBooked(true));
        slotRepository.saveAll(slotsToBook);

        Appointment appointment = new Appointment();
        appointment.setClient(client);
        appointment.setSpecialist(specialist);
        appointment.setService(service);
        appointment.setSlot(startSlot);
        appointment.setBookedSlots(slotsToBook);
        appointment.setSlotsCount(slotsNeeded);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        try {
            appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            bookingContextRepository.deleteById(context.getTelegramId());
            log.warn("Concurrent booking conflict for telegramId={}, slotId={}",
                    context.getTelegramId(), startSlot.getId());
            return new SendMessage(chatId.toString(),
                    "⚠️ Это время только что заняли. Выберите другое: /make_appointment");
        }

        bookingContextRepository.deleteById(context.getTelegramId());
        log.info("Appointment created: clientId={}, specialistId={}, startSlotId={}, slotsCount={}",
                client.getId(), specialist.getId(), startSlot.getId(), slotsNeeded);

        LocalTime endTime = startSlot.getStartTime().plusMinutes((long) slotsNeeded * 30);
        String text = String.format("""
            ✅ Запись подтверждена!
            
            👤 Специалист: %s %s
            💈 Услуга: %s
            📅 Дата: %s
            🕐 Время: %s – %s
            💰 Стоимость: %s ₽
            """,
                specialist.getFirstname(), specialist.getLastname(),
                service.getName(),
                startSlot.getSchedule().getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                startSlot.getStartTime().format(TIME_FMT),
                endTime.format(TIME_FMT),
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
        rows.add(List.of(btn("◀️ Назад", "BOOK_NAV_BACK")));
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
        rows.add(List.of(btn("◀️ Назад", "BOOK_NAV_BACK")));
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
        rows.add(List.of(btn("◀️ Назад", "BOOK_NAV_BACK")));
        rows.add(List.of(btn("❌ Отмена", "BOOK_CANCEL")));
        return buildMessage(chatId, "Выберите день:", rows);
    }

    private SendMessage showAvailableSlots(Long chatId, Integer scheduleId, Integer serviceId) {
        Service service = serviceRepository.findById(serviceId).orElseThrow();
        int slotsNeeded = (int) Math.ceil((double) service.getDurationMinutes() / 30);

        List<ScheduleSlot> allSlots = slotRepository.findAllByScheduleIdOrdered(scheduleId);
        List<ScheduleSlot> validStarts = findValidStartSlots(allSlots, slotsNeeded);

        if (validStarts.isEmpty()) {
            return new SendMessage(chatId.toString(),
                    "В этот день нет свободного времени для выбранной услуги. Выберите другой день.");
        }

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (ScheduleSlot startSlot : validStarts) {
            LocalTime endTime = startSlot.getStartTime().plusMinutes((long) slotsNeeded * 30);
            String label = startSlot.getStartTime().format(TIME_FMT) + " – " + endTime.format(TIME_FMT);
            rows.add(List.of(btn(label, "BOOK_SLOT_" + startSlot.getId())));
        }
        rows.add(List.of(btn("◀️ Назад", "BOOK_NAV_BACK")));
        rows.add(List.of(btn("❌ Отмена", "BOOK_CANCEL")));
        return buildMessage(chatId, "Выберите время:", rows);
    }

    private SendMessage showConfirmation(Long chatId, BookingContext context) {
        Specialist specialist = specialistRepository.findById(context.getSelectedSpecialistId()).orElseThrow();
        Service service = serviceRepository.findById(context.getSelectedServiceId()).orElseThrow();
        ScheduleSlot slot = slotRepository.findById(context.getSelectedSlotId()).orElseThrow();

        int slotsNeeded = (int) Math.ceil((double) service.getDurationMinutes() / 30);
        LocalTime endTime = slot.getStartTime().plusMinutes((long) slotsNeeded * 30);

        String text = String.format("""
            Подтвердите запись:
            
            👤 Специалист: %s %s
            💈 Услуга: %s (%s мин)
            📅 Дата: %s
            🕐 Время: %s – %s
            💰 Стоимость: %s ₽
            """,
                specialist.getFirstname(), specialist.getLastname(),
                service.getName(), service.getDurationMinutes(),
                slot.getSchedule().getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                slot.getStartTime().format(TIME_FMT),
                endTime.format(TIME_FMT),
                service.getPrice().toPlainString()
        );
        return buildMessage(chatId, text, List.of(
                List.of(btn("✅ Подтвердить", "BOOK_CONFIRM")),
                List.of(btn("◀️ Назад", "BOOK_NAV_BACK")),
                List.of(btn("❌ Отмена", "BOOK_CANCEL"))
        ));
    }

    private SendMessage buildPathSelectionMessage(Long chatId) {
        InlineKeyboardButton byService = btn("🔍 Выбрать услугу", "BOOK_PATH_SERVICE");
        InlineKeyboardButton bySpecialist = btn("👤 Выбрать специалиста", "BOOK_PATH_SPECIALIST");
        InlineKeyboardButton cancel = btn("❌ Отмена", "BOOK_CANCEL");
        return buildMessage(chatId, "Как хотите записаться?", List.of(
                List.of(byService),
                List.of(bySpecialist),
                List.of(cancel)
        ));
    }

    private SendMessage navigateBack(BookingContext context, Long chatId) {
        return switch (context.getStep()) {
            case SELECT_PATH -> buildPathSelectionMessage(chatId);
            case SELECT_SERVICE -> {
                context.setStep(BookingStep.SELECT_PATH);
                context.setPath(null);
                context.setSelectedServiceId(null);
                bookingContextRepository.save(context);
                yield buildPathSelectionMessage(chatId);
            }
            case SELECT_SPECIALIST -> {
                if (context.getPath() == BookingPath.BY_SERVICE) {
                    context.setStep(BookingStep.SELECT_SERVICE);
                    context.setSelectedSpecialistId(null);
                    Integer serviceId = context.getSelectedServiceId();
                    bookingContextRepository.save(context);
                    yield showSpecialistsByService(chatId, serviceId);
                }
                context.setStep(BookingStep.SELECT_PATH);
                context.setPath(null);
                context.setSelectedSpecialistId(null);
                bookingContextRepository.save(context);
                yield buildPathSelectionMessage(chatId);
            }
            case SELECT_SPECIALIST_SERVICE -> {
                context.setStep(BookingStep.SELECT_SPECIALIST);
                context.setSelectedSpecialistId(null);
                context.setSelectedServiceId(null);
                bookingContextRepository.save(context);
                yield showAllSpecialists(chatId);
            }
            case SELECT_DAY -> {
                context.setSelectedScheduleId(null);
                if (context.getPath() == BookingPath.BY_SERVICE) {
                    context.setStep(BookingStep.SELECT_SPECIALIST);
                    context.setSelectedSpecialistId(null);
                    Integer serviceId = context.getSelectedServiceId();
                    bookingContextRepository.save(context);
                    if (serviceId == null) {
                        yield unknownStep(chatId);
                    }
                    yield showSpecialistsByService(chatId, serviceId);
                }
                Integer specialistIdForServices = context.getSelectedSpecialistId();
                context.setStep(BookingStep.SELECT_SPECIALIST_SERVICE);
                context.setSelectedServiceId(null);
                bookingContextRepository.save(context);
                if (specialistIdForServices == null) {
                    yield unknownStep(chatId);
                }
                yield showServicesBySpecialist(chatId, specialistIdForServices);
            }
            case SELECT_SLOT -> {
                context.setStep(BookingStep.SELECT_DAY);
                context.setSelectedSlotId(null);
                Integer specialistId = context.getSelectedSpecialistId();
                bookingContextRepository.save(context);
                if (specialistId == null) {
                    yield unknownStep(chatId);
                }
                yield showAvailableDays(chatId, specialistId);
            }
            case CONFIRM -> {
                context.setStep(BookingStep.SELECT_SLOT);
                context.setSelectedSlotId(null);
                Integer scheduleId = context.getSelectedScheduleId();
                Integer serviceId = context.getSelectedServiceId();
                bookingContextRepository.save(context);
                if (scheduleId == null || serviceId == null) {
                    yield unknownStep(chatId);
                }
                yield showAvailableSlots(chatId, scheduleId, serviceId);
            }
        };
    }

    private SendMessage buildSpecialistList(Long chatId, List<Specialist> specialists, String header) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Specialist sp : specialists) {
            rows.add(List.of(btn(sp.getFirstname() + " " + sp.getLastname(), "BOOK_SPEC_" + sp.getId())));
        }
        rows.add(List.of(btn("◀️ Назад", "BOOK_NAV_BACK")));
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

    private List<ScheduleSlot> findValidStartSlots(List<ScheduleSlot> allSlots, int slotsNeeded) {
        List<ScheduleSlot> validStarts = new ArrayList<>();
        for (int i = 0; i <= allSlots.size() - slotsNeeded; i++) {
            if (isConsecutiveFreeBlock(allSlots, i, slotsNeeded)) {
                validStarts.add(allSlots.get(i));
            }
        }
        return validStarts;
    }

    private boolean isConsecutiveFreeBlock(List<ScheduleSlot> slots, int startIndex, int slotsNeeded) {
        for (int i = startIndex; i < startIndex + slotsNeeded; i++) {
            ScheduleSlot current = slots.get(i);
            if (current.isBooked()) return false;
            if (i > startIndex) {
                ScheduleSlot previous = slots.get(i - 1);
                if (!previous.getEndTime().equals(current.getStartTime())) return false;
            }
        }
        return true;
    }

    private void releaseSlots(Appointment appointment) {
        List<ScheduleSlot> slots = appointment.getBookedSlots();
        if (slots == null || slots.isEmpty()) {
            ScheduleSlot single = appointment.getSlot();
            if (single != null) {
                single.setBooked(false);
                slotRepository.save(single);
            }
            return;
        }
        slots.forEach(slot -> slot.setBooked(false));
        slotRepository.saveAll(slots);
    }
}

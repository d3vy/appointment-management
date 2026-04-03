package com.telegrambot.appointment.management.domain.service;

import com.telegrambot.appointment.management.domain.model.appointment.Schedule;
import com.telegrambot.appointment.management.domain.model.appointment.ScheduleSlot;
import com.telegrambot.appointment.management.domain.model.appointment.Service;
import com.telegrambot.appointment.management.domain.model.appointment.Appointment;
import com.telegrambot.appointment.management.domain.model.appointment.AppointmentStatus;
import com.telegrambot.appointment.management.domain.model.appointment.booking.BookingContext;
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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentBookingServiceTest {

    @Mock
    private BookingContextRepository bookingContextRepository;
    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private SpecialistRepository specialistRepository;
    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private ScheduleSlotRepository slotRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private SpecialistNotificationService specialistNotificationService;

    @Test
    void selectDayToday_showsOnlyFutureSlots() {
        LocalTime now = LocalTime.now();
        Assumptions.assumeTrue(now.isBefore(LocalTime.of(23, 59)));

        AppointmentBookingService bookingService = createService();
        BookingContext context = bookingContext(101L, 10, 20);
        Schedule schedule = schedule(100, 10, LocalDate.now());
        Service service = service(20, 30);

        ScheduleSlot pastSlot = slot(1, schedule, now.minusMinutes(10));
        ScheduleSlot futureSlot = slot(2, schedule, now.plusMinutes(1));

        when(bookingContextRepository.findById(101L)).thenReturn(Optional.of(context));
        when(scheduleRepository.findById(100)).thenReturn(Optional.of(schedule));
        when(serviceRepository.findById(20)).thenReturn(Optional.of(service));
        when(slotRepository.findAllByScheduleIdOrdered(100)).thenReturn(List.of(pastSlot, futureSlot));

        SendMessage result = bookingService.handleCallback(101L, 501L, "BOOK_DAY_100");

        List<String> callbackData = callbackData(result);
        assertFalse(callbackData.contains("BOOK_SLOT_1"));
        assertTrue(callbackData.contains("BOOK_SLOT_2"));
    }

    @Test
    void selectDayToday_whenAllSlotsPast_returnsNoTimeMessage() {
        LocalTime now = LocalTime.now();

        AppointmentBookingService bookingService = createService();
        BookingContext context = bookingContext(102L, 10, 20);
        Schedule schedule = schedule(110, 10, LocalDate.now());
        Service service = service(20, 30);

        ScheduleSlot pastSlotOne = slot(11, schedule, pastOrEqual(now, 10));
        ScheduleSlot pastSlotTwo = slot(12, schedule, pastOrEqual(now, 20));

        when(bookingContextRepository.findById(102L)).thenReturn(Optional.of(context));
        when(scheduleRepository.findById(110)).thenReturn(Optional.of(schedule));
        when(serviceRepository.findById(20)).thenReturn(Optional.of(service));
        when(slotRepository.findAllByScheduleIdOrdered(110)).thenReturn(List.of(pastSlotOne, pastSlotTwo));

        SendMessage result = bookingService.handleCallback(102L, 502L, "BOOK_DAY_110");

        assertTrue(result.getText().contains("нет свободного времени"));
    }

    @Test
    void selectDayFuture_keepsAllValidSlots() {
        AppointmentBookingService bookingService = createService();
        BookingContext context = bookingContext(103L, 10, 20);
        Schedule schedule = schedule(120, 10, LocalDate.now().plusDays(1));
        Service service = service(20, 30);

        ScheduleSlot morningSlot = slot(21, schedule, LocalTime.of(10, 0));
        ScheduleSlot noonSlot = slot(22, schedule, LocalTime.of(12, 0));

        when(bookingContextRepository.findById(103L)).thenReturn(Optional.of(context));
        when(scheduleRepository.findById(120)).thenReturn(Optional.of(schedule));
        when(serviceRepository.findById(20)).thenReturn(Optional.of(service));
        when(slotRepository.findAllByScheduleIdOrdered(120)).thenReturn(List.of(morningSlot, noonSlot));

        SendMessage result = bookingService.handleCallback(103L, 503L, "BOOK_DAY_120");

        List<String> callbackData = callbackData(result);
        assertTrue(callbackData.contains("BOOK_SLOT_21"));
        assertTrue(callbackData.contains("BOOK_SLOT_22"));
    }

    @Test
    void confirmBooking_sendsSpecialistNotification() {
        AppointmentBookingService bookingService = createService();
        BookingContext context = new BookingContext();
        context.setTelegramId(104L);
        context.setSelectedSpecialistId(10);
        context.setSelectedServiceId(20);
        context.setSelectedScheduleId(300);
        context.setSelectedSlotId(33);
        context.setStep(BookingStep.CONFIRM);

        Client client = new Client();
        client.setId(1000);
        client.setTelegramId(104L);
        client.setFirstname("Ivan");
        client.setLastname("Petrov");

        Specialist specialist = new Specialist();
        specialist.setId(10);
        specialist.setFirstname("Anna");
        specialist.setLastname("Smirnova");
        specialist.setTelegramId(9999L);

        Schedule schedule = new Schedule();
        schedule.setId(300);
        schedule.setDate(LocalDate.now().plusDays(1));
        schedule.setSpecialist(specialist);

        Service service = service(20, 30);
        ScheduleSlot slot = slot(33, schedule, LocalTime.of(10, 0));

        when(clientRepository.findByTelegramId(104L)).thenReturn(Optional.of(client));
        when(specialistRepository.findById(10)).thenReturn(Optional.of(specialist));
        when(serviceRepository.findById(20)).thenReturn(Optional.of(service));
        when(slotRepository.findAllByScheduleIdOrderedForUpdate(300)).thenReturn(List.of(slot));

        bookingService.handleConfirmation(context, 504L, "BOOK_CONFIRM");

        verify(specialistNotificationService).notifyAboutNewAppointment(org.mockito.ArgumentMatchers.any(Appointment.class));
    }

    @Test
    void cancelAppointment_sendsSpecialistCancellationNotification() {
        AppointmentBookingService bookingService = createService();

        Client client = new Client();
        client.setId(2000);
        client.setTelegramId(105L);

        Specialist specialist = new Specialist();
        specialist.setId(11);
        specialist.setTelegramId(8888L);

        Schedule schedule = new Schedule();
        schedule.setId(400);
        schedule.setDate(LocalDate.now().plusDays(2));
        schedule.setSpecialist(specialist);

        Service service = service(30, 60);
        ScheduleSlot firstSlot = slot(41, schedule, LocalTime.of(11, 0));
        firstSlot.setBooked(true);
        ScheduleSlot secondSlot = slot(42, schedule, LocalTime.of(11, 30));
        secondSlot.setBooked(true);

        Appointment appointment = new Appointment();
        appointment.setId(700);
        appointment.setClient(client);
        appointment.setSpecialist(specialist);
        appointment.setService(service);
        appointment.setSlot(firstSlot);
        appointment.setBookedSlots(List.of(firstSlot, secondSlot));
        appointment.setSlotsCount(2);
        appointment.setStatus(AppointmentStatus.CONFIRMED);

        when(appointmentRepository.findByIdWithSlotAndBookedSlots(700)).thenReturn(Optional.of(appointment));

        bookingService.cancelAppointment(105L, 700, 505L);

        verify(specialistNotificationService).notifyAboutClientCancellation(appointment);
    }

    private AppointmentBookingService createService() {
        return new AppointmentBookingService(
                bookingContextRepository,
                serviceRepository,
                specialistRepository,
                scheduleRepository,
                slotRepository,
                appointmentRepository,
                clientRepository,
                specialistNotificationService
        );
    }

    private BookingContext bookingContext(Long telegramId, Integer specialistId, Integer serviceId) {
        BookingContext context = new BookingContext();
        context.setTelegramId(telegramId);
        context.setStep(BookingStep.SELECT_DAY);
        context.setSelectedSpecialistId(specialistId);
        context.setSelectedServiceId(serviceId);
        return context;
    }

    private Schedule schedule(Integer scheduleId, Integer specialistId, LocalDate date) {
        Specialist specialist = new Specialist();
        specialist.setId(specialistId);
        Schedule schedule = new Schedule();
        schedule.setId(scheduleId);
        schedule.setSpecialist(specialist);
        schedule.setDate(date);
        return schedule;
    }

    private Service service(Integer id, Integer durationMinutes) {
        Service service = new Service();
        service.setId(id);
        service.setDurationMinutes(durationMinutes);
        service.setPrice(BigDecimal.valueOf(1000));
        service.setName("Haircut");
        return service;
    }

    private ScheduleSlot slot(Integer id, Schedule schedule, LocalTime startTime) {
        ScheduleSlot slot = new ScheduleSlot();
        slot.setId(id);
        slot.setSchedule(schedule);
        slot.setStartTime(startTime);
        slot.setEndTime(startTime.plusMinutes(30));
        slot.setBooked(false);
        return slot;
    }

    private LocalTime pastOrEqual(LocalTime now, int minutesBack) {
        if (now.toSecondOfDay() > minutesBack * 60L) {
            return now.minusMinutes(minutesBack);
        }
        return LocalTime.MIDNIGHT;
    }

    private List<String> callbackData(SendMessage message) {
        InlineKeyboardMarkup markup = (InlineKeyboardMarkup) message.getReplyMarkup();
        return markup.getKeyboard().stream()
                .flatMap(List::stream)
                .map(InlineKeyboardButton::getCallbackData)
                .toList();
    }
}

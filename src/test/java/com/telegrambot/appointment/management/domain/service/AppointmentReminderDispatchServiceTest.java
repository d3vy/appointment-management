package com.telegrambot.appointment.management.domain.service;

import com.telegrambot.appointment.management.domain.exception.TelegramDeliveryException;
import com.telegrambot.appointment.management.domain.model.appointment.*;
import com.telegrambot.appointment.management.domain.model.user.client.Client;
import com.telegrambot.appointment.management.domain.model.user.specialist.Specialist;
import com.telegrambot.appointment.management.domain.port.TelegramTextMessageSender;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.appointment.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentReminderDispatchServiceTest {

    private static final int APPOINTMENT_ID = 42;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private TelegramTextMessageSender messageSender;

    @InjectMocks
    private AppointmentReminderDispatchService dispatchService;

    private Appointment appointment;

    @BeforeEach
    void setUp() {
        Client client = new Client();
        client.setTelegramId(100L);
        client.setNotificationsEnabled(true);

        Specialist specialist = new Specialist();
        specialist.setFirstname("Ann");
        specialist.setLastname("Smith");

        Service service = new Service();
        service.setName("Haircut");

        Schedule schedule = new Schedule();
        schedule.setDate(LocalDate.of(2026, 3, 30));

        ScheduleSlot slot = new ScheduleSlot();
        slot.setSchedule(schedule);
        slot.setStartTime(LocalTime.of(14, 0));

        appointment = new Appointment();
        appointment.setId(APPOINTMENT_ID);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setDayReminderSent(false);
        appointment.setHourReminderSent(false);
        appointment.setClient(client);
        appointment.setSpecialist(specialist);
        appointment.setService(service);
        appointment.setSlot(slot);

        when(appointmentRepository.findByIdForReminderDispatch(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));
    }

    @Test
    void dispatchDayReminder_sendFails_doesNotPersistFlag() {
        doThrow(new TelegramDeliveryException("telegram down", new RuntimeException()))
                .when(messageSender).sendText(anyLong(), any());

        assertThrows(TelegramDeliveryException.class,
                () -> dispatchService.dispatchDayReminder(APPOINTMENT_ID));

        verify(appointmentRepository, never()).save(any());
        assertFalse(appointment.isDayReminderSent());
    }

    @Test
    void dispatchDayReminder_success_setsFlagAndSaves() {
        dispatchService.dispatchDayReminder(APPOINTMENT_ID);

        verify(messageSender).sendText(eq(100L), any());
        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());
        assertTrue(captor.getValue().isDayReminderSent());
    }

    @Test
    void dispatchDayReminder_skipsWhenAlreadySent() {
        appointment.setDayReminderSent(true);

        dispatchService.dispatchDayReminder(APPOINTMENT_ID);

        verify(messageSender, never()).sendText(anyLong(), any());
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void dispatchHourReminder_sendFails_doesNotPersistFlag() {
        doThrow(new TelegramDeliveryException("telegram down", new RuntimeException()))
                .when(messageSender).sendText(anyLong(), any());

        assertThrows(TelegramDeliveryException.class,
                () -> dispatchService.dispatchHourReminder(APPOINTMENT_ID));

        verify(appointmentRepository, never()).save(any());
        assertFalse(appointment.isHourReminderSent());
    }

    @Test
    void dispatchHourReminder_success_setsFlagAndSaves() {
        dispatchService.dispatchHourReminder(APPOINTMENT_ID);

        verify(messageSender).sendText(eq(100L), any());
        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());
        assertTrue(captor.getValue().isHourReminderSent());
    }
}

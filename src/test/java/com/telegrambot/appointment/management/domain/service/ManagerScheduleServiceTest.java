package com.telegrambot.appointment.management.domain.service;

import com.telegrambot.appointment.management.domain.model.appointment.Appointment;
import com.telegrambot.appointment.management.domain.model.appointment.AppointmentStatus;
import com.telegrambot.appointment.management.domain.model.appointment.Schedule;
import com.telegrambot.appointment.management.domain.model.appointment.ScheduleSlot;
import com.telegrambot.appointment.management.domain.model.appointment.Service;
import com.telegrambot.appointment.management.domain.model.user.client.Client;
import com.telegrambot.appointment.management.domain.model.user.manager.Manager;
import com.telegrambot.appointment.management.domain.model.user.specialist.Specialist;
import com.telegrambot.appointment.management.domain.port.TelegramTextMessageSender;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.appointment.AppointmentRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.appointment.ScheduleRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.appointment.ScheduleSlotRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.context.ManagerScheduleContextRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.manager.ManagerRepository;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.specialist.SpecialistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerScheduleServiceTest {

    @Mock
    private ManagerScheduleContextRepository scheduleContextRepository;
    @Mock
    private SpecialistRepository specialistRepository;
    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private ScheduleSlotRepository slotRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private ManagerRepository managerRepository;
    @Mock
    private TelegramTextMessageSender messageSender;
    @Mock
    private SpecialistNotificationService specialistNotificationService;

    @InjectMocks
    private ManagerScheduleService managerScheduleService;

    @Test
    void confirmDeleteDay_notifiesClientsAndSpecialists() {
        long managerTelegramId = 5000L;
        long chatId = 7000L;
        int scheduleId = 900;
        int specialistId = 300;

        Specialist specialist = new Specialist();
        specialist.setId(specialistId);
        specialist.setFirstname("Anna");
        specialist.setLastname("Smirnova");
        specialist.setTelegramId(4444L);

        Manager manager = new Manager();
        manager.setId(200);
        manager.setTelegramId(managerTelegramId);

        Schedule schedule = new Schedule();
        schedule.setId(scheduleId);
        schedule.setSpecialist(specialist);
        schedule.setCreatedBy(manager);
        schedule.setDate(LocalDate.now().plusDays(1));

        Appointment firstAppointment = appointment(1001, specialist, 8101L, LocalTime.of(10, 0), schedule);
        Appointment secondAppointment = appointment(1002, specialist, 8102L, LocalTime.of(11, 0), schedule);
        List<Appointment> activeAppointments = List.of(firstAppointment, secondAppointment);

        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(appointmentRepository.findConfirmedByScheduleId(scheduleId)).thenReturn(activeAppointments);
        when(specialistRepository.findById(specialistId)).thenReturn(Optional.of(specialist));
        when(scheduleRepository.findBySpecialistIdWithSlots(eq(specialistId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        SendMessage result = managerScheduleService.confirmDeleteDay(managerTelegramId, chatId, scheduleId);

        assertEquals(String.valueOf(chatId), result.getChatId());
        verify(appointmentRepository).saveAll(activeAppointments);
        verify(messageSender, times(2)).sendText(any(Long.class), any(String.class));
        verify(specialistNotificationService).notifyAboutManagerCancellation(firstAppointment);
        verify(specialistNotificationService).notifyAboutManagerCancellation(secondAppointment);
        assertEquals(AppointmentStatus.CANCELLED, firstAppointment.getStatus());
        assertEquals(AppointmentStatus.CANCELLED, secondAppointment.getStatus());
    }

    private Appointment appointment(int appointmentId,
                                    Specialist specialist,
                                    long clientTelegramId,
                                    LocalTime startTime,
                                    Schedule schedule) {
        Client client = new Client();
        client.setId(appointmentId + 1);
        client.setTelegramId(clientTelegramId);
        client.setFirstname("Client");
        client.setLastname(String.valueOf(appointmentId));

        Service service = new Service();
        service.setId(appointmentId + 10);
        service.setName("Haircut");

        ScheduleSlot slot = new ScheduleSlot();
        slot.setId(appointmentId + 100);
        slot.setSchedule(schedule);
        slot.setStartTime(startTime);
        slot.setEndTime(startTime.plusMinutes(30));

        Appointment appointment = new Appointment();
        appointment.setId(appointmentId);
        appointment.setClient(client);
        appointment.setSpecialist(specialist);
        appointment.setService(service);
        appointment.setSlot(slot);
        appointment.setSlotsCount(1);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        return appointment;
    }
}

package com.telegrambot.appointment.management.domain.service;

import com.telegrambot.appointment.management.domain.model.appointment.Appointment;
import com.telegrambot.appointment.management.infrastructure.persistence.repository.appointment.AppointmentRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentReminderService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentReminderService.class);

    private static final int REMINDER_WINDOW_MINUTES = 10;

    private final AppointmentRepository appointmentRepository;
    private final AppointmentReminderDispatchService reminderDispatchService;

    public AppointmentReminderService(AppointmentRepository appointmentRepository,
                                      AppointmentReminderDispatchService reminderDispatchService) {
        this.appointmentRepository = appointmentRepository;
        this.reminderDispatchService = reminderDispatchService;
    }

    @Scheduled(cron = "0 */10 * * * *")
    @SchedulerLock(name = "sendDayReminders", lockAtLeastFor = "PT30S", lockAtMostFor = "PT9M")
    public void sendDayReminders() {
        LocalDateTime from = LocalDateTime.now().plusHours(24).minusMinutes(REMINDER_WINDOW_MINUTES);
        LocalDateTime to = LocalDateTime.now().plusHours(24).plusMinutes(REMINDER_WINDOW_MINUTES);

        List<Appointment> due = appointmentRepository.findDueForDayReminder(from, to);
        log.info("Day reminders: found {} appointments", due.size());

        for (Appointment appointment : due) {
            Integer id = appointment.getId();
            try {
                reminderDispatchService.dispatchDayReminder(id);
            } catch (RuntimeException e) {
                log.error("Day reminder failed appointmentId={}", id, e);
            }
        }
    }

    @Scheduled(cron = "0 */10 * * * *")
    @SchedulerLock(name = "sendHourReminders", lockAtLeastFor = "PT30S", lockAtMostFor = "PT9M")
    public void sendHourReminders() {
        LocalDateTime from = LocalDateTime.now().plusHours(1).minusMinutes(REMINDER_WINDOW_MINUTES);
        LocalDateTime to = LocalDateTime.now().plusHours(1).plusMinutes(REMINDER_WINDOW_MINUTES);

        List<Appointment> due = appointmentRepository.findDueForHourReminder(from, to);
        log.info("Hour reminders: found {} appointments", due.size());

        for (Appointment appointment : due) {
            Integer id = appointment.getId();
            try {
                reminderDispatchService.dispatchHourReminder(id);
            } catch (RuntimeException e) {
                log.error("Hour reminder failed appointmentId={}", id, e);
            }
        }
    }
}

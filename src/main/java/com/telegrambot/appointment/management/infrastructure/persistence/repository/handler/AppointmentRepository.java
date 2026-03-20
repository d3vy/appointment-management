package com.telegrambot.appointment.management.infrastructure.persistence.repository.handler;

import com.telegrambot.appointment.management.domain.model.appointment.Appointment;
import com.telegrambot.appointment.management.domain.model.appointment.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.specialist sp
            JOIN FETCH a.service srv
            JOIN FETCH a.slot sl
            JOIN FETCH sl.schedule sc
            WHERE a.client.id = :clientId
              AND a.status = :status
            ORDER BY sc.date, sl.startTime
            """)
    List<Appointment> findByClientIdAndStatus(@Param("clientId") Integer clientId,
                                              @Param("status") AppointmentStatus status);

    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.client c
            JOIN FETCH a.service srv
            JOIN FETCH a.slot sl
            JOIN FETCH sl.schedule sc
            WHERE a.specialist.id = :specialistId
              AND a.status = :status
            ORDER BY sc.date, sl.startTime
            """)
    List<Appointment> findBySpecialistIdAndStatus(@Param("specialistId") Integer specialistId,
                                                  @Param("status") AppointmentStatus status);

    @Query(value = """
            SELECT a.* FROM client.appointments a
            JOIN specialist.schedule_slots sl ON sl.id = a.slot_id
            JOIN specialist.schedules sc ON sc.id = sl.schedule_id
            JOIN client.clients c ON c.id = a.client_id
            WHERE a.status = 'CONFIRMED'
              AND a.day_reminder_sent = false
              AND c.notifications_enabled = true
              AND (sc.date + sl.start_time) BETWEEN :from AND :to
            """, nativeQuery = true)
    List<Appointment> findDueForDayReminder(@Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT a.* FROM client.appointments a
            JOIN specialist.schedule_slots sl ON sl.id = a.slot_id
            JOIN specialist.schedules sc ON sc.id = sl.schedule_id
            JOIN client.clients c ON c.id = a.client_id
            WHERE a.status = 'CONFIRMED'
              AND a.hour_reminder_sent = false
              AND c.notifications_enabled = true
              AND (sc.date + sl.start_time) BETWEEN :from AND :to
            """, nativeQuery = true)
    List<Appointment> findDueForHourReminder(@Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);
}

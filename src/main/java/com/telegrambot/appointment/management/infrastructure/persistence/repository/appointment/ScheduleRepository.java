package com.telegrambot.appointment.management.infrastructure.persistence.repository.appointment;

import com.telegrambot.appointment.management.domain.model.appointment.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {

    @Query("""
            SELECT sc FROM Schedule sc
            WHERE sc.specialist.id = :specialistId
              AND sc.date >= :from
              AND EXISTS (
                  SELECT 1 FROM ScheduleSlot sl
                  WHERE sl.schedule = sc AND sl.booked = false
              )
            ORDER BY sc.date
            """)
    List<Schedule> findAvailableBySpecialist(@Param("specialistId") Integer specialistId,
                                             @Param("from") LocalDate from);

    @Query("""
        SELECT sc FROM Schedule sc
        LEFT JOIN FETCH sc.slots sl
        WHERE sc.specialist.id = :specialistId
          AND sc.date BETWEEN :from AND :to
        ORDER BY sc.date, sl.startTime
        """)
    List<Schedule> findBySpecialistIdWithSlots(@Param("specialistId") Integer specialistId,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to);

    Optional<Schedule> findBySpecialistIdAndDate(Integer specialistId, LocalDate date);
}

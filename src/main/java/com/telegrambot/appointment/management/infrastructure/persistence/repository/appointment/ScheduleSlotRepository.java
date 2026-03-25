package com.telegrambot.appointment.management.infrastructure.persistence.repository.appointment;

import com.telegrambot.appointment.management.domain.model.appointment.ScheduleSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, Integer> {

    @Query("SELECT sl FROM ScheduleSlot sl WHERE sl.schedule.id = :scheduleId AND sl.booked = false ORDER BY sl.startTime")
    List<ScheduleSlot> findFreeByScheduleId(@Param("scheduleId") Integer scheduleId);

    @Query("""
    SELECT sl FROM ScheduleSlot sl
    WHERE sl.schedule.id = :scheduleId
    ORDER BY sl.startTime
    """)
    List<ScheduleSlot> findAllByScheduleIdOrdered(@Param("scheduleId") Integer scheduleId);

    @Query("""
    SELECT sl FROM ScheduleSlot sl
    WHERE sl.schedule.id = :scheduleId
    ORDER BY sl.startTime
    """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ScheduleSlot> findAllByScheduleIdOrderedForUpdate(@Param("scheduleId") Integer scheduleId);


    @Query("""
        SELECT sl FROM ScheduleSlot sl
        WHERE sl.schedule.id IN :scheduleIds
          AND sl.booked = false
        ORDER BY sl.startTime
        """)
    List<ScheduleSlot> findFreeByScheduleIds(@Param("scheduleIds") List<Integer> scheduleIds);


}

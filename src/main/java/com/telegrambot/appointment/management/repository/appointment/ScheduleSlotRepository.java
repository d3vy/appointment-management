package com.telegrambot.appointment.management.repository.appointment;

import com.telegrambot.appointment.management.model.appointment.schedule.ScheduleSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, Integer> {

    @Query("SELECT sl FROM ScheduleSlot sl WHERE sl.schedule.id = :scheduleId AND sl.booked = false ORDER BY sl.startTime")
    List<ScheduleSlot> findFreeByScheduleId(@Param("scheduleId") Integer scheduleId);
}
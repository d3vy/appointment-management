package com.telegrambot.appointment.management.model.appointment.schedule;

import jakarta.persistence.*;

import java.time.LocalTime;

@Entity
@Table(schema = "specialist", name = "schedule_slots")
public class ScheduleSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "is_booked", nullable = false)
    private boolean booked = false;

    public ScheduleSlot() {}

    public ScheduleSlot(Schedule schedule, LocalTime startTime, LocalTime endTime) {
        this.schedule = schedule;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Schedule getSchedule() { return schedule; }
    public void setSchedule(Schedule schedule) { this.schedule = schedule; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public boolean isBooked() { return booked; }
    public void setBooked(boolean booked) { this.booked = booked; }
}

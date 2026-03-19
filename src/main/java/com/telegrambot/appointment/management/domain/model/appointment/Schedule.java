package com.telegrambot.appointment.management.domain.model.appointment;

import com.telegrambot.appointment.management.domain.model.user.manager.Manager;
import com.telegrambot.appointment.management.domain.model.user.specialist.Specialist;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        schema = "specialist",
        name = "schedules",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_schedule_specialist_date",
                columnNames = {"specialist_id", "date"}
        )
)
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specialist_id", nullable = false)
    private Specialist specialist;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_manager_id", nullable = false)
    private Manager createdBy;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScheduleSlot> slots = new ArrayList<>();

    public Schedule() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Specialist getSpecialist() { return specialist; }
    public void setSpecialist(Specialist specialist) { this.specialist = specialist; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public Manager getCreatedBy() { return createdBy; }
    public void setCreatedBy(Manager createdBy) { this.createdBy = createdBy; }
    public List<ScheduleSlot> getSlots() { return slots; }
    public void setSlots(List<ScheduleSlot> slots) { this.slots = slots; }
}

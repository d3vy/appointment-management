package com.telegrambot.appointment.management.domain.model.appointment;

import com.telegrambot.appointment.management.domain.model.user.client.Client;
import com.telegrambot.appointment.management.domain.model.user.specialist.Specialist;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(schema = "client", name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specialist_id", nullable = false)
    private Specialist specialist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false, unique = true)
    private ScheduleSlot slot;

    @Column(name = "slots_count", nullable = false)
    private int slotsCount = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.CONFIRMED;

    @Column(name = "day_reminder_sent", nullable = false)
    private boolean dayReminderSent = false;

    @Column(name = "hour_reminder_sent", nullable = false)
    private boolean hourReminderSent = false;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            schema = "client",
            name = "appointment_slots",
            joinColumns = @JoinColumn(name = "appointment_id"),
            inverseJoinColumns = @JoinColumn(name = "slot_id")
    )
    private List<ScheduleSlot> bookedSlots = new ArrayList<>();


    public Appointment() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public Specialist getSpecialist() { return specialist; }
    public void setSpecialist(Specialist specialist) { this.specialist = specialist; }
    public Service getService() { return service; }
    public void setService(Service service) { this.service = service; }
    public ScheduleSlot getSlot() { return slot; }
    public void setSlot(ScheduleSlot slot) { this.slot = slot; }
    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }
    public boolean isDayReminderSent() {return dayReminderSent;}
    public void setDayReminderSent(boolean dayReminderSent) {this.dayReminderSent = dayReminderSent;}
    public boolean isHourReminderSent() {return hourReminderSent;}
    public void setHourReminderSent(boolean hourReminderSent) {this.hourReminderSent = hourReminderSent;}
    public int getSlotsCount() {return slotsCount;}
    public void setSlotsCount(int slotsCount) {this.slotsCount = slotsCount;}
    public List<ScheduleSlot> getBookedSlots() {return bookedSlots;}
    public void setBookedSlots(List<ScheduleSlot> bookedSlots) {this.bookedSlots = bookedSlots;}
}

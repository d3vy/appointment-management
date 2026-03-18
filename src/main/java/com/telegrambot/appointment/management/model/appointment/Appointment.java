package com.telegrambot.appointment.management.model.appointment;

import com.telegrambot.appointment.management.model.appointment.schedule.ScheduleSlot;
import com.telegrambot.appointment.management.model.user.Client;
import com.telegrambot.appointment.management.model.user.Specialist;
import jakarta.persistence.*;

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

    // unique — один слот не может быть занят двумя записями
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false, unique = true)
    private ScheduleSlot slot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.CONFIRMED;

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
}

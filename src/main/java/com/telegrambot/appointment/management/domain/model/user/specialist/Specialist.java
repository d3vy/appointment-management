package com.telegrambot.appointment.management.domain.model.user.specialist;

import com.telegrambot.appointment.management.domain.model.appointment.Service;
import com.telegrambot.appointment.management.domain.model.user.User;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(schema = "specialist", name = "specialists")
public class Specialist extends User {

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            schema = "specialist",
            name = "specialist_services",
            joinColumns = @JoinColumn(name = "specialist_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private Set<Service> services = new HashSet<>();

    public Set<Service> getServices() { return services; }
    public void setServices(Set<Service> services) { this.services = services; }
}

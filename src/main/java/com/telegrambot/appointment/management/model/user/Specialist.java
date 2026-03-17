package com.telegrambot.appointment.management.model.user;

import com.telegrambot.appointment.management.model.Service;
import jakarta.persistence.*;

@Entity
@Table(schema = "specialist", name = "specialists")
public class Specialist extends User {

    // lastname унаследован от User — НЕ переобъявлять здесь.
    // Дублирование поля вызывает конфликт маппинга JPA.

    @ManyToOne
    @JoinColumn(name = "service_id")
    private Service service;

    public Service getService() { return service; }
    public void setService(Service service) { this.service = service; }
}

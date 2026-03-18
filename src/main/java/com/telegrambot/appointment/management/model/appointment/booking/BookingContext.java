package com.telegrambot.appointment.management.model.appointment.booking;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;


@RedisHash("booking_context")
public class BookingContext implements Serializable {

    @Id
    private Long telegramId;

    private BookingStep step;

    // Путь входа: через услугу или через специалиста
    private BookingPath path;

    private Integer selectedServiceId;
    private Integer selectedSpecialistId;
    private Integer selectedScheduleId;
    private Integer selectedSlotId;

    @TimeToLive
    private long ttl = 900L; // 15 минут

    public BookingContext() {}

    public Long getTelegramId() { return telegramId; }
    public void setTelegramId(Long telegramId) { this.telegramId = telegramId; }

    public BookingStep getStep() { return step; }
    public void setStep(BookingStep step) { this.step = step; }

    public BookingPath getPath() { return path; }
    public void setPath(BookingPath path) { this.path = path; }

    public Integer getSelectedServiceId() { return selectedServiceId; }
    public void setSelectedServiceId(Integer selectedServiceId) { this.selectedServiceId = selectedServiceId; }

    public Integer getSelectedSpecialistId() { return selectedSpecialistId; }
    public void setSelectedSpecialistId(Integer selectedSpecialistId) { this.selectedSpecialistId = selectedSpecialistId; }

    public Integer getSelectedScheduleId() { return selectedScheduleId; }
    public void setSelectedScheduleId(Integer selectedScheduleId) { this.selectedScheduleId = selectedScheduleId; }

    public Integer getSelectedSlotId() { return selectedSlotId; }
    public void setSelectedSlotId(Integer selectedSlotId) { this.selectedSlotId = selectedSlotId; }

    public long getTtl() { return ttl; }
    public void setTtl(long ttl) { this.ttl = ttl; }
}

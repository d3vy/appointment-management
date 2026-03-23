package com.telegrambot.appointment.management.domain.model.user.manager;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RedisHash("manager_schedule_context")
public class ManagerScheduleContext implements Serializable {

    @Id
    private Long telegramId;

    private ManagerScheduleStep step;
    private Integer selectedSpecialistId;
    private List<LocalDate> selectedDates = new ArrayList<>();
    private String workdayInput; // "09:00-18:00"

    @TimeToLive
    private long ttl = 600L;

    public ManagerScheduleContext() {}

    public ManagerScheduleContext(Long telegramId,
                                  ManagerScheduleStep step,
                                  Integer selectedSpecialistId,
                                  List<LocalDate> selectedDates,
                                  String workdayInput) {
        this.telegramId = telegramId;
        this.step = step;
        this.selectedSpecialistId = selectedSpecialistId;
        this.selectedDates = selectedDates;
        this.workdayInput = workdayInput;
        this.ttl = ttl;
    }

    public Long getTelegramId() { return telegramId; }
    public void setTelegramId(Long telegramId) { this.telegramId = telegramId; }
    public ManagerScheduleStep getStep() { return step; }
    public void setStep(ManagerScheduleStep step) { this.step = step; }
    public Integer getSelectedSpecialistId() { return selectedSpecialistId; }
    public void setSelectedSpecialistId(Integer selectedSpecialistId) { this.selectedSpecialistId = selectedSpecialistId; }
    public List<LocalDate> getSelectedDates() { return selectedDates; }
    public void setSelectedDates(List<LocalDate> selectedDates) { this.selectedDates = selectedDates; }
    public String getWorkdayInput() { return workdayInput; }
    public void setWorkdayInput(String workdayInput) { this.workdayInput = workdayInput; }
    public long getTtl() { return ttl; }
    public void setTtl(long ttl) { this.ttl = ttl; }
}

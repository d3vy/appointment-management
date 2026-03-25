package com.telegrambot.appointment.management.domain.model.user.manager;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;

@RedisHash("manager_pending_action")
public class ManagerPendingAction implements Serializable {

    @Id
    private Long telegramId;

    private ManagerAction action;

    private String draftServiceName;
    private String draftServicePrice;
    private String draftServiceDurationMinutes;

    @TimeToLive
    private long ttl = 300L;

    public ManagerPendingAction() {
    }

    public ManagerPendingAction(Long telegramId, ManagerAction action) {
        this.telegramId = telegramId;
        this.action = action;
    }

    public Long getTelegramId() {
        return telegramId;
    }

    public void setTelegramId(Long telegramId) {
        this.telegramId = telegramId;
    }

    public ManagerAction getAction() {
        return action;
    }

    public void setAction(ManagerAction action) {
        this.action = action;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public String getDraftServiceName() {
        return draftServiceName;
    }

    public void setDraftServiceName(String draftServiceName) {
        this.draftServiceName = draftServiceName;
    }

    public String getDraftServicePrice() {
        return draftServicePrice;
    }

    public void setDraftServicePrice(String draftServicePrice) {
        this.draftServicePrice = draftServicePrice;
    }

    public String getDraftServiceDurationMinutes() {
        return draftServiceDurationMinutes;
    }

    public void setDraftServiceDurationMinutes(String draftServiceDurationMinutes) {
        this.draftServiceDurationMinutes = draftServiceDurationMinutes;
    }
}

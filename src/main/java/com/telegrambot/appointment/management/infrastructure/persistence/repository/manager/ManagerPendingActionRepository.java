package com.telegrambot.appointment.management.infrastructure.persistence.repository.manager;

import com.telegrambot.appointment.management.domain.model.user.manager.ManagerPendingAction;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ManagerPendingActionRepository extends CrudRepository<ManagerPendingAction, Long> {
}

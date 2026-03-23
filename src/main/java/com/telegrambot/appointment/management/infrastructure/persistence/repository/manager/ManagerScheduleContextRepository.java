package com.telegrambot.appointment.management.infrastructure.persistence.repository.manager;

import com.telegrambot.appointment.management.domain.model.user.manager.ManagerScheduleContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ManagerScheduleContextRepository extends CrudRepository<ManagerScheduleContext, Long> {
}

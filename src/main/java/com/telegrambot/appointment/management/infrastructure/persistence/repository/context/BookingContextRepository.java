package com.telegrambot.appointment.management.infrastructure.persistence.repository.context;

import com.telegrambot.appointment.management.domain.model.appointment.booking.BookingContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingContextRepository extends CrudRepository<BookingContext, Long> {
}

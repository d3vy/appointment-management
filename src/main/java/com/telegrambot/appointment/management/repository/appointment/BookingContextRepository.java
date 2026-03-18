package com.telegrambot.appointment.management.repository.appointment;

import com.telegrambot.appointment.management.model.appointment.booking.BookingContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingContextRepository extends CrudRepository<BookingContext, Long> {
}

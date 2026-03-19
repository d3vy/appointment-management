package com.telegrambot.appointment.management.infrastructure.persistence.repository.specialist;

import com.telegrambot.appointment.management.domain.model.user.specialist.Specialist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecialistRepository extends JpaRepository<Specialist, Integer> {
    Optional<Specialist> findByTelegramId(Long telegramId);
    boolean existsByTelegramId(Long telegramId);

    @Query("SELECT s FROM Specialist s JOIN s.services srv WHERE srv.id = :serviceId")
    List<Specialist> findAllByServiceId(@Param("serviceId") Integer serviceId);
}

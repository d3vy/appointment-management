CREATE TABLE IF NOT EXISTS client.appointment_slots
(
    appointment_id INTEGER NOT NULL,
    slot_id        INTEGER NOT NULL,
    CONSTRAINT pk_appointment_slots PRIMARY KEY (appointment_id, slot_id),
    CONSTRAINT fk_appt_slots_appointment FOREIGN KEY (appointment_id)
        REFERENCES client.appointments (id) ON DELETE CASCADE,
    CONSTRAINT fk_appt_slots_slot FOREIGN KEY (slot_id)
        REFERENCES specialist.schedule_slots (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_appointment_slots_slot
    ON client.appointment_slots (slot_id);
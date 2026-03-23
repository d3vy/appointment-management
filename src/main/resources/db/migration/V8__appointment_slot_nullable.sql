ALTER TABLE client.appointments
    ALTER COLUMN slot_id DROP NOT NULL;

ALTER TABLE client.appointments
    DROP CONSTRAINT IF EXISTS fk_appt_slot,
    ADD CONSTRAINT fk_appt_slot
        FOREIGN KEY (slot_id) REFERENCES specialist.schedule_slots(id)
        ON DELETE SET NULL;
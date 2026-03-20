ALTER TABLE client.appointments
    ADD COLUMN IF NOT EXISTS day_reminder_sent  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS hour_reminder_sent BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_appointments_reminders
    ON client.appointments (status, day_reminder_sent, hour_reminder_sent);
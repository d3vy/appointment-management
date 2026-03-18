CREATE SCHEMA IF NOT EXISTS client;
CREATE SCHEMA IF NOT EXISTS manager;
CREATE SCHEMA IF NOT EXISTS specialist;
CREATE SCHEMA IF NOT EXISTS service;

CREATE TABLE IF NOT EXISTS client.clients
(
    id           SERIAL PRIMARY KEY,
    telegram_id  BIGINT       NOT NULL UNIQUE,
    username     VARCHAR(255) UNIQUE,
    firstname    VARCHAR(255),
    lastname     VARCHAR(255),
    phone_number VARCHAR(255) UNIQUE
);

CREATE TABLE IF NOT EXISTS manager.managers
(
    id           SERIAL PRIMARY KEY,
    telegram_id  BIGINT       NOT NULL UNIQUE,
    username     VARCHAR(255) UNIQUE,
    firstname    VARCHAR(255),
    lastname     VARCHAR(255),
    phone_number VARCHAR(255) UNIQUE
);

CREATE TABLE IF NOT EXISTS manager.manager_whitelist
(
    username VARCHAR(255) PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS specialist.specialists
(
    id           SERIAL PRIMARY KEY,
    telegram_id  BIGINT       NOT NULL UNIQUE,
    username     VARCHAR(255) UNIQUE,
    firstname    VARCHAR(255),
    lastname     VARCHAR(255),
    phone_number VARCHAR(255) UNIQUE
);

CREATE TABLE IF NOT EXISTS service.services
(
    id               SERIAL PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    price            NUMERIC      NOT NULL,
    duration_minutes INTEGER      NOT NULL DEFAULT 60
);

ALTER TABLE specialist.specialists
    DROP COLUMN IF EXISTS service_id;

CREATE TABLE IF NOT EXISTS specialist.specialist_services
(
    specialist_id INTEGER NOT NULL,
    service_id    INTEGER NOT NULL,
    CONSTRAINT pk_specialist_services PRIMARY KEY (specialist_id, service_id),
    CONSTRAINT fk_ss_specialist FOREIGN KEY (specialist_id) REFERENCES specialist.specialists (id) ON DELETE CASCADE,
    CONSTRAINT fk_ss_service    FOREIGN KEY (service_id)    REFERENCES service.services (id)       ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS specialist.schedules
(
    id                    SERIAL PRIMARY KEY,
    specialist_id         INTEGER NOT NULL,
    date                  DATE    NOT NULL,
    created_by_manager_id INTEGER NOT NULL,
    CONSTRAINT uq_schedule_specialist_date UNIQUE (specialist_id, date),
    CONSTRAINT fk_schedule_specialist FOREIGN KEY (specialist_id)         REFERENCES specialist.specialists (id) ON DELETE CASCADE,
    CONSTRAINT fk_schedule_manager   FOREIGN KEY (created_by_manager_id) REFERENCES manager.managers (id)
);

CREATE TABLE IF NOT EXISTS specialist.schedule_slots
(
    id          SERIAL PRIMARY KEY,
    schedule_id INTEGER NOT NULL,
    start_time  TIME    NOT NULL,
    end_time    TIME    NOT NULL,
    is_booked   BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_slot_schedule FOREIGN KEY (schedule_id) REFERENCES specialist.schedules (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_slots_schedule_booked
    ON specialist.schedule_slots (schedule_id, is_booked);

CREATE TABLE IF NOT EXISTS client.appointments
(
    id            SERIAL PRIMARY KEY,
    client_id     INTEGER     NOT NULL,
    specialist_id INTEGER     NOT NULL,
    service_id    INTEGER     NOT NULL,
    slot_id       INTEGER     NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    CONSTRAINT uq_appointment_slot    UNIQUE (slot_id),
    CONSTRAINT fk_appt_client     FOREIGN KEY (client_id)     REFERENCES client.clients (id),
    CONSTRAINT fk_appt_specialist FOREIGN KEY (specialist_id) REFERENCES specialist.specialists (id),
    CONSTRAINT fk_appt_service    FOREIGN KEY (service_id)    REFERENCES service.services (id),
    CONSTRAINT fk_appt_slot       FOREIGN KEY (slot_id)       REFERENCES specialist.schedule_slots (id),
    CONSTRAINT chk_appt_status    CHECK (status IN ('CONFIRMED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_appointments_client
    ON client.appointments (client_id, status);

CREATE INDEX IF NOT EXISTS idx_appointments_specialist
    ON client.appointments (specialist_id, status);
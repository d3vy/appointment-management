INSERT INTO service.services (name, price, duration_minutes)
SELECT seed.name, seed.price, seed.duration_minutes
FROM (VALUES ('Стрижка', 1500.00::numeric, 60),
             ('Окрашивание', 3500.00::numeric, 120),
             ('Укладка', 800.00::numeric, 30)) AS seed(name, price, duration_minutes)
WHERE NOT EXISTS (
    SELECT 1
    FROM service.services s
    WHERE s.name = seed.name
);

INSERT INTO manager.manager_whitelist (username)
SELECT seed.username
FROM (VALUES ('demo_manager'),
             ('theiliachuvikin')) AS seed(username)
WHERE NOT EXISTS (
    SELECT 1
    FROM manager.manager_whitelist mw
    WHERE mw.username = seed.username
);

INSERT INTO specialist.specialist_whitelist (username)
SELECT seed.username
FROM (VALUES ('demo_master_anna'),
             ('demo_master_olga')) AS seed(username)
WHERE NOT EXISTS (
    SELECT 1
    FROM specialist.specialist_whitelist sw
    WHERE sw.username = seed.username
);

INSERT INTO manager.managers (telegram_id, username, firstname, lastname, phone_number)
SELECT seed.telegram_id, seed.username, seed.firstname, seed.lastname, seed.phone_number
FROM (VALUES (700000001::bigint, 'demo_manager', 'Demo', 'Manager', '+79000000001')) AS seed(telegram_id, username, firstname, lastname, phone_number)
WHERE NOT EXISTS (
    SELECT 1
    FROM manager.managers m
    WHERE m.telegram_id = seed.telegram_id
);

INSERT INTO specialist.specialists (telegram_id, username, firstname, lastname, phone_number)
SELECT seed.telegram_id, seed.username, seed.firstname, seed.lastname, seed.phone_number
FROM (VALUES (700000101::bigint, 'demo_master_anna', 'Anna', 'Petrova', '+79000000101'),
             (700000102::bigint, 'demo_master_olga', 'Olga', 'Smirnova', '+79000000102')) AS seed(telegram_id, username, firstname, lastname, phone_number)
WHERE NOT EXISTS (
    SELECT 1
    FROM specialist.specialists s
    WHERE s.telegram_id = seed.telegram_id
);

INSERT INTO client.clients (telegram_id, username, firstname, lastname, phone_number, notifications_enabled)
SELECT seed.telegram_id, seed.username, seed.firstname, seed.lastname, seed.phone_number, seed.notifications_enabled
FROM (VALUES (700000201::bigint, 'demo_client_ivan', 'Ivan', 'Sidorov', '+79000000201', TRUE),
             (700000202::bigint, 'demo_client_maria', 'Maria', 'Ivanova', '+79000000202', TRUE)) AS seed(telegram_id, username, firstname, lastname, phone_number, notifications_enabled)
WHERE NOT EXISTS (
    SELECT 1
    FROM client.clients c
    WHERE c.telegram_id = seed.telegram_id
);

INSERT INTO public.user_roles (telegram_id, role)
SELECT seed.telegram_id, seed.role
FROM (VALUES (700000001::bigint, 'MANAGER'::varchar),
             (700000101::bigint, 'SPECIALIST'::varchar),
             (700000102::bigint, 'SPECIALIST'::varchar),
             (700000201::bigint, 'CLIENT'::varchar),
             (700000202::bigint, 'CLIENT'::varchar)) AS seed(telegram_id, role)
WHERE NOT EXISTS (
    SELECT 1
    FROM public.user_roles ur
    WHERE ur.telegram_id = seed.telegram_id
);

INSERT INTO specialist.specialist_services (specialist_id, service_id)
SELECT specialist_data.specialist_id, service_data.service_id
FROM (
         SELECT s.id AS specialist_id, s.username
         FROM specialist.specialists s
         WHERE s.username IN ('demo_master_anna', 'demo_master_olga')
     ) specialist_data
         JOIN (
    SELECT sv.id AS service_id, sv.name
    FROM service.services sv
    WHERE sv.name IN ('Стрижка', 'Окрашивание', 'Укладка')
) service_data ON (specialist_data.username = 'demo_master_anna')
    OR (specialist_data.username = 'demo_master_olga' AND service_data.name IN ('Стрижка', 'Укладка'))
WHERE NOT EXISTS (
    SELECT 1
    FROM specialist.specialist_services ss
    WHERE ss.specialist_id = specialist_data.specialist_id
      AND ss.service_id = service_data.service_id
);

INSERT INTO specialist.schedules (specialist_id, date, created_by_manager_id)
SELECT specialist_data.id, schedule_seed.work_date, manager_data.id
FROM (
         VALUES ('demo_master_anna', CURRENT_DATE + 1),
                ('demo_master_anna', CURRENT_DATE + 2),
                ('demo_master_olga', CURRENT_DATE + 1),
                ('demo_master_olga', CURRENT_DATE + 2)
     ) AS schedule_seed(username, work_date)
         JOIN specialist.specialists specialist_data ON specialist_data.username = schedule_seed.username
         JOIN manager.managers manager_data ON manager_data.username = 'demo_manager'
WHERE NOT EXISTS (
    SELECT 1
    FROM specialist.schedules s
    WHERE s.specialist_id = specialist_data.id
      AND s.date = schedule_seed.work_date
);

INSERT INTO specialist.schedule_slots (schedule_id, start_time, end_time, is_booked)
SELECT schedule_data.schedule_id, slot_seed.start_time, slot_seed.end_time, FALSE
FROM (
         SELECT s.id AS schedule_id, sp.username, s.date
         FROM specialist.schedules s
                  JOIN specialist.specialists sp ON sp.id = s.specialist_id
         WHERE sp.username IN ('demo_master_anna', 'demo_master_olga')
           AND s.date IN (CURRENT_DATE + 1, CURRENT_DATE + 2)
     ) schedule_data
         CROSS JOIN (
    VALUES (TIME '10:00', TIME '10:30'),
           (TIME '10:30', TIME '11:00'),
           (TIME '11:00', TIME '11:30'),
           (TIME '13:00', TIME '13:30'),
           (TIME '13:30', TIME '14:00')
) AS slot_seed(start_time, end_time)
WHERE NOT EXISTS (
    SELECT 1
    FROM specialist.schedule_slots slot
    WHERE slot.schedule_id = schedule_data.schedule_id
      AND slot.start_time = slot_seed.start_time
);

INSERT INTO client.appointments (
    client_id,
    specialist_id,
    service_id,
    slot_id,
    status,
    day_reminder_sent,
    hour_reminder_sent,
    slots_count
)
SELECT appointment_seed.client_id,
       appointment_seed.specialist_id,
       appointment_seed.service_id,
       appointment_seed.slot_id,
       'CONFIRMED',
       FALSE,
       FALSE,
       1
FROM (
         SELECT client_data.id AS client_id,
                specialist_data.id AS specialist_id,
                service_data.id AS service_id,
                slot_data.id AS slot_id
         FROM client.clients client_data
                  JOIN specialist.specialists specialist_data ON specialist_data.username = 'demo_master_anna'
                  JOIN service.services service_data ON service_data.name = 'Стрижка'
                  JOIN specialist.schedules schedule_data
                       ON schedule_data.specialist_id = specialist_data.id
                           AND schedule_data.date = CURRENT_DATE + 1
                  JOIN specialist.schedule_slots slot_data
                       ON slot_data.schedule_id = schedule_data.id
                           AND slot_data.start_time = TIME '10:00'
         WHERE client_data.username = 'demo_client_ivan'

         UNION ALL

         SELECT client_data.id AS client_id,
                specialist_data.id AS specialist_id,
                service_data.id AS service_id,
                slot_data.id AS slot_id
         FROM client.clients client_data
                  JOIN specialist.specialists specialist_data ON specialist_data.username = 'demo_master_olga'
                  JOIN service.services service_data ON service_data.name = 'Укладка'
                  JOIN specialist.schedules schedule_data
                       ON schedule_data.specialist_id = specialist_data.id
                           AND schedule_data.date = CURRENT_DATE + 2
                  JOIN specialist.schedule_slots slot_data
                       ON slot_data.schedule_id = schedule_data.id
                           AND slot_data.start_time = TIME '13:00'
         WHERE client_data.username = 'demo_client_maria'
     ) appointment_seed
WHERE NOT EXISTS (
    SELECT 1
    FROM client.appointments a
    WHERE a.slot_id = appointment_seed.slot_id
);

INSERT INTO client.appointment_slots (appointment_id, slot_id)
SELECT appointment_data.id, appointment_data.slot_id
FROM client.appointments appointment_data
         JOIN client.clients client_data ON client_data.id = appointment_data.client_id
WHERE client_data.username IN ('demo_client_ivan', 'demo_client_maria')
  AND appointment_data.slot_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM client.appointment_slots aps
    WHERE aps.appointment_id = appointment_data.id
      AND aps.slot_id = appointment_data.slot_id
);

UPDATE specialist.schedule_slots slot
SET is_booked = TRUE
WHERE EXISTS (
    SELECT 1
    FROM client.appointments appointment_data
    WHERE appointment_data.slot_id = slot.id
      AND appointment_data.status = 'CONFIRMED'
);

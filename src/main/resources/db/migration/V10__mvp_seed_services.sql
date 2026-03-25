INSERT INTO service.services (name, price, duration_minutes)
SELECT name, price, duration_minutes
FROM (VALUES ('Стрижка', 1500.00::numeric, 60),
             ('Окрашивание', 3500.00::numeric, 120),
             ('Укладка', 800.00::numeric, 30)) AS seeded(name, price, duration_minutes)
WHERE NOT EXISTS (SELECT 1 FROM service.services LIMIT 1);

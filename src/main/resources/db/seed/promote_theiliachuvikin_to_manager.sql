BEGIN;

CREATE TEMP TABLE _promote_manager AS
SELECT id AS specialist_id,
       telegram_id,
       username,
       firstname,
       lastname,
       phone_number
FROM specialist.specialists
WHERE lower(username) = lower('theiliachuvikin');

DELETE FROM client.appointments a
USING _promote_manager p
WHERE a.specialist_id = p.specialist_id;

DELETE FROM specialist.specialist_whitelist
WHERE lower(username) = lower('theiliachuvikin');

DELETE FROM specialist.specialists s
USING _promote_manager p
WHERE s.id = p.specialist_id;

INSERT INTO manager.managers (telegram_id, username, firstname, lastname, phone_number)
SELECT telegram_id, username, firstname, lastname, phone_number
FROM _promote_manager
ON CONFLICT (telegram_id) DO UPDATE SET
    username     = EXCLUDED.username,
    firstname    = COALESCE(EXCLUDED.firstname, manager.managers.firstname),
    lastname     = COALESCE(EXCLUDED.lastname, manager.managers.lastname),
    phone_number = COALESCE(EXCLUDED.phone_number, manager.managers.phone_number);

INSERT INTO manager.manager_whitelist (username)
VALUES ('theiliachuvikin')
ON CONFLICT (username) DO NOTHING;

UPDATE public.user_roles ur
SET role = 'MANAGER'
FROM _promote_manager p
WHERE ur.telegram_id = p.telegram_id;

INSERT INTO public.user_roles (telegram_id, role)
SELECT p.telegram_id, 'MANAGER'::varchar
FROM _promote_manager p
WHERE NOT EXISTS (SELECT 1 FROM public.user_roles ur WHERE ur.telegram_id = p.telegram_id);

COMMIT;

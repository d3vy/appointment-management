CREATE TABLE IF NOT EXISTS public.telegram_notification_outbox
(
    id              BIGSERIAL   NOT NULL,
    event_type      VARCHAR(64) NOT NULL,
    appointment_id  INT         NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at    TIMESTAMPTZ,
    attempt_count   INT         NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_error      VARCHAR(2000),
    CONSTRAINT pk_telegram_notification_outbox PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_telegram_outbox_pending_event_appointment
    ON public.telegram_notification_outbox (event_type, appointment_id)
    WHERE processed_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_telegram_outbox_pending_due
    ON public.telegram_notification_outbox (next_attempt_at, id)
    WHERE processed_at IS NULL;

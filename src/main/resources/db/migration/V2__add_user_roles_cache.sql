CREATE TABLE IF NOT EXISTS public.user_roles
(
    telegram_id BIGINT      NOT NULL,
    role        VARCHAR(20) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (telegram_id),
    CONSTRAINT chk_user_roles_role CHECK (role IN ('CLIENT', 'SPECIALIST', 'MANAGER', 'NOT_REGISTERED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_user_roles_telegram_id ON public.user_roles (telegram_id);

COMMENT ON TABLE public.user_roles IS
    'Кэш роли пользователя для быстрого лукапа. Заполняется при регистрации.';

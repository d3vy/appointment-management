# appointment-management

Telegram-бот на Spring Boot для записи клиентов к специалистам: роли (клиент / специалист / менеджер), расписание, напоминания, каталог услуг.

## Требования

- Java 21
- Maven 3.9+
- PostgreSQL 16 и Redis 7 (удобно поднять через Docker)

## Быстрый старт с Docker

1. Скопируйте переменные окружения:

   ```bash
   cp .env.example .env
   ```

   Заполните `BOT_NAME` и `BOT_TOKEN` (от [@BotFather](https://t.me/BotFather)).

2. Поднимите Postgres и Redis с портами на localhost (для `mvn spring-boot:run` на машине разработчика):

   ```bash
   docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d postgres redis
   ```

   Без `docker-compose.dev.yml` в стеке только внутренняя сеть: Postgres и Redis **не** публикуют порты наружу (так безопаснее на сервере при `docker compose up --build`).

3. Запустите приложение (переменные из `.env` подхватит `spring-dotenv`):

   ```bash
   mvn spring-boot:run
   ```

   Для отладочного логирования SQL (только локально): `mvn spring-boot:run -Dspring-boot.run.profiles=dev`.

   `REDIS_PASSWORD` в `.env` должен совпадать с тем, что подставляет Compose (по умолчанию в примере — `devredis`; в проде задайте сложное значение).

Строка подключения по умолчанию совместима с проброшенными портами (`localhost:5432`, БД `appointment_bot`).

4. Опционально — всё в Docker, включая приложение (профиль `prod`, нужен заполненный `.env`, в том числе `ACTUATOR_PASSWORD` и надёжные `DB_PASSWORD`, `REDIS_PASSWORD`):

   ```bash
   docker compose up -d --build
   ```

   Образ приложения задаёт `SPRING_PROFILES_ACTIVE=prod` по умолчанию; для локального запуска jar без Docker переопределите профиль при необходимости.

   Один контейнер с ботом на один `BOT_TOKEN` (long polling не масштабируется на несколько реплик с одним токеном).

## MVP-данные

- После миграций Flyway в пустую базу добавляются демо-услуги (см. `V10__mvp_seed_services.sql`).
- Менеджер в боте: меню **«Привязать услугу»** — назначение услуг из каталога зарегистрированным специалистам.
- Менеджер: **«Добавить услугу»** — пошаговое создание услуги (название, цена, длительность кратно 30 минут).

## Тестовые данные для ручной проверки

SQL-скрипт `src/main/resources/db/seed/test_data.sql` добавляет тестовые данные:

- whitelist для ролей manager/specialist;
- пользователей manager/specialist/client;
- связи специалистов с услугами;
- расписания и слоты на ближайшие 2 дня;
- подтверждённые записи клиентов в слоты.

Запуск для локального PostgreSQL:

```bash
psql -h localhost -U postgres -d appointment_bot -f src/main/resources/db/seed/test_data.sql
```

Запуск для PostgreSQL в Docker Compose:

```bash
docker compose exec -T postgres psql -U postgres -d appointment_bot < src/main/resources/db/seed/test_data.sql
```

Логины/username в Telegram для теста:

- менеджер: `demo_manager`
- специалисты: `demo_master_anna`, `demo_master_olga`
- клиенты: `demo_client_ivan`, `demo_client_maria`

## Полезные эндпоинты

- `GET /actuator/health` — агрегированный статус. В профиле `prod` без аутентификации доступен только он; остальные эндпоинты Actuator — по HTTP Basic (`ACTUATOR_USERNAME` / `ACTUATOR_PASSWORD`). Компонент `telegram` в ответе: `registered` — бот зарегистрирован; `starting` — ещё не завершена регистрация; при ошибке регистрации общий статус будет `DOWN`.
- `GET /actuator/prometheus` (только `prod`) — текстовый экспорт метрик Micrometer для Prometheus; доступен с теми же учётными данными Actuator, что и остальные защищённые эндпоинты.

## Продакшен (MVP)

### Профиль `prod`

- `SPRING_PROFILES_ACTIVE=prod` (в Docker-образе задаётся по умолчанию; на VPS не отключайте без необходимости — иначе Actuator останется без HTTP Basic, см. [`ProdActuatorSecurityConfiguration.java`](src/main/java/com/telegrambot/appointment/management/infrastructure/config/ProdActuatorSecurityConfiguration.java).)
- Обязательно задайте сильный `ACTUATOR_PASSWORD` (и при желании нестандартный `ACTUATOR_USERNAME`). Слабый пароль или дефолтный логин дают доступ к JVM/приложенческим метрикам любому, кто достучится до `/actuator/*`.
- **Сеть:** по возможности не выставляйте `/actuator/prometheus` в открытый интернет; ограничьте reverse-proxy по IP Prometheus/VPN или вынесите management listener на `127.0.0.1` и скрейпьте через sidecar (см. комментарий в [`application-prod.yaml`](src/main/resources/application-prod.yaml)).
- `docker compose` в репозитории требует задать в `.env` как минимум `DB_PASSWORD`, `REDIS_PASSWORD`, `ACTUATOR_PASSWORD` (без небезопасных значений по умолчанию).
- JVM и cron-напоминания используют часовой пояс процесса: при необходимости задайте `-Duser.timezone=Europe/Moscow` (или другой) в командной строке `java`.
- Уведомления специалисту/клиенту после изменения записи ставятся в transactional outbox (`public.telegram_notification_outbox`); доставка в Telegram выполняется фоновым воркером с повторами и ShedLock.

### Whitelist менеджера и специалиста

Регистрация с ролью менеджера или специалиста возможна только если **Telegram @username** занесён в БД (без учёта регистра):

```sql
INSERT INTO manager.manager_whitelist (username) VALUES ('username_менеджера');
INSERT INTO specialist.specialist_whitelist (username) VALUES ('username_мастера');
```

Клиенты whitelist не требуют.

### Бэкапы и восстановление PostgreSQL

Пример логического бэкапа:

```bash
pg_dump -h localhost -U postgres -d appointment_bot -Fc -f appointment_bot.dump
```

Восстановление в пустую БД (осторожно с `--clean` на проде):

```bash
pg_restore -h localhost -U postgres -d appointment_bot --clean appointment_bot.dump
```

Имеет смысл периодически проверять восстановление на копии.

### Короткий runbook после сбоя

1. Убедиться, что доступны PostgreSQL и Redis, переменные окружения совпадают с боевыми.
2. При необходимости восстановить БД из бэкапа.
3. Запустить приложение; Flyway применит миграции при старте.
4. Проверить `GET /actuator/health` и сценарий в боте (`/start`, запись).

### Сеть Docker

- Состав `docker compose up --build`: Postgres и Redis доступны только сервису `app` внутри сети Compose; наружу по умолчанию проброшен только порт приложения (`8080`).
- Для доступа к БД/Redis с хоста при разработке используйте второй файл: `docker compose -f docker-compose.yml -f docker-compose.dev.yml`.

### Юридический минимум

Черновой чеклист для договора с салоном и учёта персональных данных — в [LEGAL.md](LEGAL.md).

## Тесты

Юнит-тесты не требуют Docker. Интеграционный тест `AppointmentApplicationTests` использует Testcontainers и **автоматически отключается**, если Docker недоступен (`@Testcontainers(disabledWithoutDocker = true)`). Чтобы прогнать его локально, запустите Docker и выполните `mvn test`.

## Заметки по деплою

- Напоминания защищены **ShedLock** (таблица `shedlock`): при нескольких репликах задача выполняется только на одном инстансе.
- Двойное бронирование слота снижается за счёт `SELECT … FOR UPDATE` на слоты при подтверждении и уникального индекса на `client.appointment_slots(slot_id)`.
- Профиль `prod`, бэкапы, whitelist и образ приложения описаны в разделе **Продакшен (MVP)** выше.

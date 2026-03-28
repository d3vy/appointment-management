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

2. Поднимите Postgres и Redis:

   ```bash
   docker compose up -d
   ```

3. Запустите приложение (переменные из `.env` подхватит `spring-dotenv`):

   ```bash
   mvn spring-boot:run
   ```

   Для отладочного логирования SQL (только локально): `mvn spring-boot:run -Dspring-boot.run.profiles=dev`.

Строка подключения по умолчанию совместима с `docker-compose.yml` (`localhost:5432`, БД `appointment_bot`).

4. Опционально — всё в Docker, включая приложение (профиль `prod`, нужен заполненный `.env`, в том числе `ACTUATOR_PASSWORD`):

   ```bash
   docker compose up -d --build
   ```

   Один контейнер с ботом на один `BOT_TOKEN` (long polling не масштабируется на несколько реплик с одним токеном).

## MVP-данные

- После миграций Flyway в пустую базу добавляются демо-услуги (см. `V10__mvp_seed_services.sql`).
- Менеджер в боте: меню **«Привязать услугу»** — назначение услуг из каталога зарегистрированным специалистам.
- Менеджер: **«Добавить услугу»** — пошаговое создание услуги (название, цена, длительность кратно 30 минут).

## Полезные эндпоинты

- `GET /actuator/health` — агрегированный статус. В профиле `prod` без аутентификации доступен только он; остальные эндпоинты Actuator — по HTTP Basic (`ACTUATOR_USERNAME` / `ACTUATOR_PASSWORD`). Компонент `telegram` в ответе: `registered` — бот зарегистрирован; `starting` — ещё не завершена регистрация; при ошибке регистрации общий статус будет `DOWN`.

## Продакшен (MVP)

### Профиль `prod`

- `SPRING_PROFILES_ACTIVE=prod`
- Обязательно задайте сильный `ACTUATOR_PASSWORD` (и при желании `ACTUATOR_USERNAME`).
- JVM и cron-напоминания используют часовой пояс процесса: при необходимости задайте `-Duser.timezone=Europe/Moscow` (или другой) в командной строке `java`.

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

### Юридический минимум

Черновой чеклист для договора с салоном и учёта персональных данных — в [LEGAL.md](LEGAL.md).

## Тесты

Юнит-тесты не требуют Docker. Интеграционный тест `AppointmentApplicationTests` использует Testcontainers и **автоматически отключается**, если Docker недоступен (`@Testcontainers(disabledWithoutDocker = true)`). Чтобы прогнать его локально, запустите Docker и выполните `mvn test`.

## Заметки по деплою

- Напоминания защищены **ShedLock** (таблица `shedlock`): при нескольких репликах задача выполняется только на одном инстансе.
- Двойное бронирование слота снижается за счёт `SELECT … FOR UPDATE` на слоты при подтверждении и уникального индекса на `client.appointment_slots(slot_id)`.
- Профиль `prod`, бэкапы, whitelist и образ приложения описаны в разделе **Продакшен (MVP)** выше.

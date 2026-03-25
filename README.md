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

Строка подключения по умолчанию совместима с `docker-compose.yml` (`localhost:5432`, БД `appointment_bot`).

## MVP-данные

- После миграций Flyway в пустую базу добавляются демо-услуги (см. `V10__mvp_seed_services.sql`).
- Менеджер в боте: меню **«Привязать услугу»** — назначение услуг из каталога зарегистрированным специалистам.
- Менеджер: **«Добавить услугу»** — пошаговое создание услуги (название, цена, длительность кратно 30 минут).

## Полезные эндпоинты

- `GET /actuator/health` — проверка живости (Spring Boot Actuator).

## Тесты

Юнит-тесты не требуют Docker. Интеграционный тест `AppointmentApplicationTests` использует Testcontainers и **автоматически отключается**, если Docker недоступен (`@Testcontainers(disabledWithoutDocker = true)`). Чтобы прогнать его локально, запустите Docker и выполните `mvn test`.

## Заметки по деплою

- Напоминания защищены **ShedLock** (таблица `shedlock`): при нескольких репликах задача выполняется только на одном инстансе.
- Двойное бронирование слота снижается за счёт `SELECT … FOR UPDATE` на слоты при подтверждении и уникального индекса на `client.appointment_slots(slot_id)`.

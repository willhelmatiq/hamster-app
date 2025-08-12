# Hamster Activity Tracker

Сервис реального времени, который:
- принимает события от датчиков (вход/выход/вращение/поломка),
- считает круги (1 круг = 5 секунд),
- детектирует неактивных хомяков/датчиков,
- формирует ежедневные отчёты (in-memory),
- выгружает агрегаты за прошлые дни в PostgreSQL,
- отдаёт отчёты по HTTP (из памяти/БД).

## Архитектура

- `engine/` — ядро: `EventProcessor` обрабатывает поток `EventBus` и пишет в `TrackerState`.
- `service/` — прикладная логика:
    - `InactivityMonitor` — раз в минуту проверяет `hamster/sensorLastSeen` и шлёт алерты.
    - `DailyStatsExporter` — по крону выгружает в БД отчёты за прошлый день (UPSERT).
    - `DailyReportService` — читает отчёт: сегодня — in-memory, прошедшие даты — из БД.
- `repository/` — `DailyStatsRepository` (JDBC) читает `daily_stats`.
- `controller/` — контроллеры и модели API.

## Конфигурация

`src/main/resources/application.yml` (пример):

```yaml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/hamsterhub
    username: hamster
    password: hamster
  flyway:
    enabled: true

tracker:
  active-threshold: 10
  cleanup-interval-ms: 600000
  deduplication-windowMs: 250
  export-cron: "0 5 0 * * *"     # каждый день в 00:05
  export-days-back: 1            # экспортируем Day-1
  hamster-inactivity-ms: 60000    # 1 час = 3600000
  sensor-inactivity-ms: 300000     # 30 минут = 1800000
  workers: 4
  zone-id: "Europe/Moscow"
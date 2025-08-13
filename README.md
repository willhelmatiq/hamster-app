# Hamster App

Проект состоит из hamster-sensor-simulator - [Сервис-симулятор](hamster-sensor-simulator/README.md), который генерирует события `HamsterEvent`, 
hamster-activity-tracker - [Трекер](hamster-activity-tracker/README.md), который обрабатывает события `HamsterEvent` 
и hamster-common - общая часть, где описаны `HamsterEvent`.
hamster-sensor-simulator посылает события в hamster-activity-tracker по HTTP

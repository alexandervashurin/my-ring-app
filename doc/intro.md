# Introduction to my-ring-app

## Обзор

**Система управления персоналом** — веб-приложение для автоматизации кадрового учёта на производственном предприятии, построенное на Clojure/Ring.

## Основные возможности

- CRUD-операции с работниками
- Учёт рабочего времени
- Расчёт зарплаты (4 системы оплаты)
- Дашборд с аналитикой
- Экспорт данных (CSV, Excel, PDF)
- REST API для интеграций
- Аутентификация и авторизация (4 роли)
- Email уведомления

## Технологический стек

- **Clojure** 1.11.1
- **Ring** 1.9.6
- **Compojure** 1.7.0
- **SQLite** 3.x (dev) / **PostgreSQL** (prod)
- **Leiningen** 2.x

## Быстрый старт

```bash
# Клонирование
git clone https://github.com/alexandervashurin/my-ring-app.git
cd my-ring-app

# Установка зависимостей
lein deps

# Запуск
lein run
```

Приложение будет доступно по адресу: http://localhost:3000

## Структура проекта

```
my-ring-app/
├── src/my_ring_app/
│   ├── core.clj           # Точка входа, middleware
│   ├── routes.clj         # Маршруты Compojure
│   ├── controllers.clj    # Контроллеры
│   ├── model.clj          # Модель данных (БД)
│   ├── validation.clj     # Валидация данных
│   ├── util.clj           # Вспомогательные функции
│   └── views/             # HTML-шаблоны
├── test/my_ring_app/      # Тесты
├── resources/             # Ресурсы (логи, статика)
└── doc/                   # Документация
```

## Документация

- [README.md](../README.md) — основная документация
- [DOCUMENTATION.md](../DOCUMENTATION.md) — полная документация API
- [DEVELOPMENT_PLAN.md](../DEVELOPMENT_PLAN.md) — план развития
- [DOCKER.md](../DOCKER.md) — Docker развёртывание
- [POSTGRES_DEPLOYMENT.md](../POSTGRES_DEPLOYMENT.md) — PostgreSQL

## Лицензия

Eclipse Public License 2.0 (EPL-2.0)

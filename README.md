# 🏭 Система управления персоналом (CRUD-приложение на Clojure/Ring)

[![Clojure](https://img.shields.io/badge/Clojure-1.11.1-blue.svg)](https://clojure.org/)
[![License](https://img.shields.io/badge/license-EPL--2.0-green.svg)](LICENSE)
[![Ring](https://img.shields.io/badge/Ring-1.9.6-purple.svg)](https://github.com/ring-clojure/ring)
[![SQLite](https://img.shields.io/badge/SQLite-3.x-lightgrey.svg)](https://www.sqlite.org/)
[![Tests](https://img.shields.io/badge/tests-228%20tests%20%7C%20582%20assertions-brightgreen.svg)]()
[![Status](https://img.shields.io/badge/status-production--ready-success.svg)]()

**Система управления персоналом** — полноценное веб-приложение для управления базой данных работников предприятия с поддержкой всех операций CRUD (Create, Read, Update, Delete).

## 📑 Оглавление

- [О проекте](#-о-проекте)
- [Возможности](#-возможности)
- [Скриншоты](#-скриншоты)
- [Технологический стек](#-технологический-стек)
- [Быстрый старт](#-быстрый-старт)
- [Структура проекта](#-структура-проекта)
- [API Endpoints](#-api-endpoints)
- [База данных](#-база-данных)
- [Логирование](#-логирование)
- [Тестирование](#-тестирование)
- [Конфигурация](#-конфигурация)
- [Развёртывание](#-развёртывание)
- [Устранение проблем](#-устранение-проблем)
- [Вклад в проект](#-вклад-в-проект)
- [Лицензия](#-лицензия)
- [Автор](#-автор)

## 📸 Скриншоты

> **Примечание:** Если скриншоты не отображаются, проверьте наличие файлов в папке `screenshots/`

### Главная страница
![Главная страница](screenshots/main.png)

### Список работников
![Список работников](screenshots/workers-list.png)

### Форма добавления работника
![Форма добавления](screenshots/add-worker.png)

### Форма редактирования
![Форма редактирования](screenshots/edit-worker.png)

## 🎯 О проекте

**Система управления персоналом** — это полноценное веб-приложение для автоматизации кадрового учёта на предприятии.

### Проблемы, которые решает приложение:
- 📌 **Централизация данных** — вся информация о работниках в одном месте
- 📌 **Автоматизация расчётов** — расчёт зарплаты и учёта рабочего времени
- 📌 **Быстрый поиск** — мгновенный поиск по базе работников
- 📌 **Контроль изменений** — аудит всех операций с данными
- 📌 **Гибкость** — поддержка различных систем оплаты труда

### Целевая аудитория:
- Малые и средние предприятия
- Отделы кадров
- Бухгалтерии
- Руководители подразделений

## ✨ Функциональность

### Основные возможности:
- ✅ **Просмотр** списка всех работников с расширенной информацией и поиском по ФИО и цеху
- ✅ **CRUD** для работников, справочников и организаций
- ✅ **Умные формы** с выпадающими списками для справочников
- ✅ **Автоматическое переключение** между окладом и почасовой ставкой
- ✅ **Современный адаптивный интерфейс** (ru/en)

### Аутентификация и роли:
- 🔐 **Регистрация, вход, выход** и смена пароля
- 🛡️ **Глобальные роли**: `admin`, `manager`, `hr`, `viewer`
- 🏢 **Роли в организациях**: `org_admin`, `org_manager`, `org_hr`, `org_viewer`
- ⏱️ **Мульти-тенантность** — разграничение данных между организациями
- 📋 **Журнал сессий** — активные сессии, неудачные входы, статистика

### Мульти-тенантность и тарифы:
- 🏭 **Организации** — управление организациями и их участниками
- 💳 **Тарифные планы**: Free (10 работников/1 организация), Pro (50/3), Enterprise (без ограничений)
- 📊 **Контроль лимитов** — проверка превышения числа работников по тарифу

### Работа со справочниками:
- 📋 Цеха
- 💰 Системы оплаты труда
- 👥 Категории работников
- 🔢 Разряды
- 🕐 Режимы работы
- 💸 Оклады
- ⏱️ Почасовые ставки
- 📑 Тарифные планы

### Отчёты и экспорт:
- 📊 **Расчёт зарплаты** — просмотр информации о зарплате работника
- 🕐 **Учёт рабочего времени** — отслеживание отработанных часов, больничных и командировочных дней
- 📄 **PDF-отчёты** — по работнику, списку работников и зарплате за период (с кириллицей)
- 📁 **Экспорт** в CSV и Excel (XLSX), выгрузка в 1С (XML)
- 📈 **Дашборд** с аналитикой (по цехам, категориям, распределению зарплат)

### Прочее:
- 🗄️ **Просмотр БД** — отображение всех таблиц базы данных с содержимым
- 🕵️ **Аудит** — журнал изменений по сущностям и пользователям
- 🩺 **Мониторинг** — health/ready/live, Prometheus-метрики, статистика
- 🔔 **Email-уведомления** — тест SMTP, уведомления о новых работниках, днях рождения и юбилеях
- 🔄 **Версионирование API** — поддержка `/api/v1/*` и `/api/v2/*`
- ⚡ **SSE** — быстрый polling дашборда (`/api/dashboard/poll`)

## 🛠 Технологический стек

### Backend
| Технология | Версия | Назначение |
|------------|--------|------------|
| **Clojure** | 1.11.1 | Функциональный язык программирования на платформе JVM |
| **Ring** | 1.9.6 | Веб-фреймворк, абстракция над HTTP |
| **Compojure** | 1.7.0 | Библиотека маршрутизации для Ring |
| **Jetty** | 9.4.x | Встроенный веб-сервер |
| **buddy-hashers** | 2.0.167 | Хеширование паролей (bcrypt) |
| **data.json** | 2.4.0 | Работа с JSON |
| **java-time** | 1.4.3 | Работа с датами и временем |

### База данных
| Технология | Версия | Назначение |
|------------|--------|------------|
| **SQLite** | 3.x | Встраиваемая реляционная СУБД |
| **PostgreSQL** | 42.6.0 | Драйвер для PostgreSQL (опционально) |
| **SQLite JDBC** | 3.45.1.0 | Драйвер для подключения к SQLite |
| **java.jdbc** | 0.7.12 | Clojure-библиотека для работы с JDBC |
| **HikariCP** | 4.1.0 | Пул соединений |

### Отчёты и экспорт
| Технология | Версия | Назначение |
|------------|--------|------------|
| **clj-pdf** | 2.6.1 | Генерация PDF-отчётов (кириллица через TTF-шрифт) |
| **Apache POI** | 5.2.3 | Экспорт в Excel (XLSX) |
| **data.csv** | 1.0.1 | Экспорт в CSV |
| **data.xml** | 0.2.0-alpha6 | Выгрузка для 1С (XML) |

### Логирование
| Технология | Версия | Назначение |
|------------|--------|------------|
| **SLF4J** | 2.0.9 | Фасад для логирования |
| **Logback** | 1.4.11 | Реализация логирования |
| **tools.logging** | 1.2.4 | Clojure-обёртка над SLF4J |

### Инфраструктура
| Инструмент | Назначение |
|------------|------------|
| **Leiningen** | Управление зависимостями и сборка |
| **Git** | Система контроля версий |

## 📋 Требования

| Зависимость | Минимальная версия | Рекомендуемая версия |
|-------------|-------------------|---------------------|
| **Java JDK** | 17 | 21 |
| **Leiningen** | 2.10.0 | 2.11.x |
| **SQLite** | 3.30 | 3.45+ |

### Проверка установленных зависимостей

```bash
# Проверка Java
java -version

# Проверка Leiningen
lein --version

# Проверка SQLite (опционально)
sqlite3 --version
```

### Установка зависимостей

#### Ubuntu/Debian
```bash
sudo apt update
sudo apt install openjdk-17-jdk sqlite3
```

#### macOS (Homebrew)
```bash
brew install openjdk@17 sqlite3
```

#### Windows
Скачайте и установите:
- [Java JDK](https://adoptium.net/)
- [SQLite](https://www.sqlite.org/download.html)

#### Установка Leiningen (все платформы)
```bash
# macOS/Linux
curl https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein > /usr/local/bin/lein
chmod +x /usr/local/bin/lein

# Windows (через Chocolatey)
choco install leiningen
```

## 🚀 Быстрый старт

### 1. Клонирование репозитория

```bash
git clone https://github.com/alexandervashurin/my-ring-app.git
cd my-ring-app
```

### 2. Установка зависимостей

```bash
lein deps
```

> **Примечание:** Первая загрузка зависимостей может занять несколько минут

### 3. Подготовка базы данных

Убедитесь, что файл `igra.db` находится в корне проекта:

```bash
ls -la igra.db
```

Если базы данных нет, создайте её из дампа:

```bash
sqlite3 igra.db < igra.db.sql
```

### 4. Запуск приложения в режиме разработки

```bash
lein run
```

Приложение будет доступно по адресу: **http://localhost:3000**

> 💡 **Совет:** Для автоматического перезапуска при изменении кода используйте `lein ring server`

### 5. Сборка и запуск в виде JAR-файла (для продакшена)

#### Сборка

```bash
lein uberjar
```

Эта команда создаст два файла:

- `target/uberjar/my-ring-app-0.1.0-SNAPSHOT.jar` — обычный JAR
- `target/uberjar/my-ring-app-0.1.0-SNAPSHOT-standalone.jar` — standalone JAR со всеми зависимостями (используйте этот!)

#### Запуск

```bash
java -jar target/uberjar/my-ring-app-0.1.0-SNAPSHOT-standalone.jar
```

#### Настройка порта

По умолчанию приложение запускается на порту 3000. Чтобы изменить порт:

```bash
# Через переменную окружения
PORT=8080 java -jar target/uberjar/my-ring-app-0.1.0-SNAPSHOT-standalone.jar
```

---

## 🐳 Docker (опционально)

### Создание Docker-образа

Создайте файл `Dockerfile` в корне проекта:

```dockerfile
FROM eclipse-temurin:17-jdk-alpine

RUN apk add --no-cache leiningen sqlite

WORKDIR /app

COPY . .

RUN lein deps && lein uberjar

EXPOSE 3000

CMD ["java", "-jar", "target/uberjar/my-ring-app-0.1.0-SNAPSHOT-standalone.jar"]
```

### Сборка и запуск контейнера

```bash
# Сборка образа
docker build -t my-ring-app .

# Запуск контейнера
docker run -p 3000:3000 -v $(pwd)/logs:/app/logs my-ring-app
```

## 📁 Структура проекта

```
my-ring-app/
├── src/
│   └── my_ring_app/
│       ├── core.clj           # Точка входа, middleware
│       ├── routes.clj         # Маршруты (Compojure)
│       ├── controllers.clj    # Контроллеры (обработка запросов)
│       ├── model.clj          # Модель данных (работа с БД)
│       ├── validation.clj     # Валидация данных
│       ├── util.clj           # Общие вспомогательные функции
│       ├── auth.clj           # Аутентификация и авторизация
│       ├── config.clj         # Конфигурация
│       ├── logger.clj         # Логирование
│       ├── migration.clj      # Система миграций БД
│       ├── cache.clj          # Кэш справочников (Atom)
│       ├── rate_limit.clj     # Rate limiting middleware
│       ├── api_version.clj    # API versioning (v1/v2)
│       ├── i18n.clj           # Мультиязычность (ru/en)
│       ├── email.clj          # Email уведомления
│       ├── pdf_reports.clj    # Генерация PDF отчётов
│       ├── sse.clj            # Server-Sent Events (polling дашборда)
│       ├── tariff.clj         # Тарифные планы и лимиты
│       ├── session_audit.clj  # Журнал сессий
│       ├── controllers/
│       │   ├── auth.clj       # Вход/выход, профиль, смена пароля
│       │   ├── organizations.clj # Управление организациями
│       │   └── references.clj # CRUD справочников (цеха, разряды, и т.д.)
│       ├── api/
│       │   ├── dashboard.clj  # API дашборда и аналитики
│       │   ├── workers.clj    # API работников
│       │   ├── salary.clj     # API зарплаты и учёта времени
│       │   ├── export.clj     # API экспорта (CSV/Excel)
│       │   ├── reports.clj    # API PDF отчётов
│       │   ├── audit.clj      # API аудита
│       │   ├── monitoring.clj # API мониторинга
│       │   ├── notifications.clj # API уведомлений
│       │   ├── onec.clj       # API интеграции с 1С
│       │   ├── organizations.clj # API организаций
│       │   ├── tariff.clj     # API тарифов
│       │   └── session_audit.clj # API сессий
│       └── views/
│           ├── layout.clj     # Общий HTML-шаблон
│           ├── home.clj       # Главная страница
│           ├── dashboard.clj  # Дашборд с аналитикой
│           ├── workers.clj    # Страница работников
│           ├── salary.clj     # Страница зарплаты
│           ├── work_time.clj  # Учёт рабочего времени
│           ├── organizations.clj # Страницы организаций
│           ├── references.clj # Страницы справочников
│           ├── tables.clj     # Просмотр таблиц БД
│           ├── auth.clj       # Страницы входа/регистрации
│           └── helpers.clj    # Вспомогательные функции
├── test/
│   └── my_ring_app/
│       ├── core_test.clj      # Интеграционные тесты
│       ├── model_test.clj     # Тесты модели
│       ├── validation_test.clj # Тесты валидации
│       ├── util_test.clj      # Тесты util.clj
│       ├── cache_test.clj     # Тесты кэша
│       ├── migration_test.clj # Тесты миграций
│       ├── auth_test.clj      # Тесты аутентификации
│       ├── controllers_test.clj # Тесты контроллеров
│       ├── tariff_test.clj    # Тесты тарифов
│       ├── session_audit_test.clj # Тесты журнала сессий
│       ├── email_test.clj     # Тесты email
│       ├── pdf_reports_test.clj # Тесты PDF-отчётов
│       ├── api_version_test.clj # Тесты версионирования API
│       ├── sse_test.clj       # Тесты SSE
│       ├── logger_test.clj    # Тесты логгера
│       ├── test_helper.clj    # Общие утилиты тестов (изолированная БД)
│       └── api/
│           ├── export_test.clj # Тесты экспорта
│           └── workers_test.clj # Тесты API работников
├── .github/workflows/
│   ├── clojure.yml            # CI: тесты + uberjar
│   ├── docker.yml             # CD: Docker build + push
│   └── deploy.yml             # Deploy: SSH
├── resources/
│   ├── migrations/             # SQL миграции
│   ├── logback.xml            # Конфигурация логгера
│   └── public/
│       ├── css/app.css        # Стили
│       ├── js/validation.js   # Клиентская валидация
│       └── api-docs.html      # Swagger UI
├── igra.db                    # База данных SQLite
├── igra.db.sql                # Дамп базы данных
├── project.clj                # Конфигурация Leiningen
├── Dockerfile                 # Docker-образ
├── docker-compose.yml         # Docker Compose (app + PostgreSQL)
└── README.md                  # Этот файл
```

## 🗄️ База данных

### Схема базы данных

Приложение использует следующие таблицы:

| Таблица | Описание |
|---------|----------|
| `Работник` | Основная информация о работниках |
| `Цех` | Справочник цехов предприятия |
| `Система_оплаты` | Справочник систем оплаты труда |
| `Категория_работника` | Категории работников (рабочий, специалист, руководитель) |
| `Разряд` | Тарифные разряды |
| `Режим_работы` | Режимы работы (односменный, двухсменный, etc.) |
| `Оклад` | Должностные оклады |
| `Почасовые_ставки` | Почасовые тарифные ставки |
| `Учет_рабочего_времени` | Записи об отработанном времени |
| `Начисление_заработной_платы` | Расчёты заработной платы |
| `Пользователь` | Учётные записи (глобальные роли) |
| `Организация` | Организации (мульти-тенантность) |
| `Тарифный_план` | Тарифные планы (Free/Pro/Enterprise) |
| `Сессия` | Журнал входов и сессий |
| `Аудит_изменений` | Журнал аудита изменений |

### Работа с базой данных

Приложение поддерживает две СУБД:

- **SQLite** (по умолчанию) — встраиваемая БД, файл `igra.db`:
  ```bash
  # Просмотр структуры БД
  sqlite3 igra.db ".schema"

  # Просмотр всех таблиц
  sqlite3 igra.db ".tables"

  # Экспорт дампа
  sqlite3 igra.db ".dump" > backup.sql

  # Импорт из дампа
  sqlite3 igra.db < backup.sql
  ```

- **PostgreSQL** — для больших объёмов данных. Запуск:
  ```bash
  DB_TYPE=postgresql DB_USER=my_ring_app DB_PASSWORD=secret \
  DB_HOST=localhost DB_NAME=my_ring_app lein run
  ```

  При первом запуске миграции применяются автоматически (трансляция SQLite-специфики в PG-синтаксис). Проверка подключения:
  ```bash
  psql "postgresql://my_ring_app:secret@localhost:5432/my_ring_app" -c "SELECT 1"
  ```

## 🔒 Безопасность

### Реализованные механизмы защиты

| Механизм | Описание |
|----------|----------|
| **XSS-защита** | HTML-экранирование всех пользовательских данных |
| **CSRF-защита** | Middleware для защиты от CSRF-атак |
| **Clickjacking** | Заголовок X-Frame-Options: DENY |
| **MIME-sniffing** | Заголовок X-Content-Type-Options: nosniff |
| **CSP** | Content-Security-Policy для ограничения источников скриптов |
| **SRI** | Subresource Integrity для статических ресурсов |
| **Secure Cookie** | Флаг Secure для сессионных cookies |
| **Session Revalidation** | Проверка активности пользователя при каждом запросе |
| **Brute-force Protection** | Блокировка после 5 неудачных попыток (15 минут) |
| **Mass Assignment Protection** | Валидация полей при обновлении пользователя |
| **Input Validation** | Валидация email, year/month, ID на всех endpoint'ах |

### Рекомендации для продакшена

1. **Настройте HTTPS** через обратный прокси (nginx/Apache)
2. **Смените пароль admin** по умолчанию
3. **Настройте SECRET_KEY** для сессий
4. **Регулярно делайте бэкапы** базы данных
5. **Настройте firewall** для ограничения доступа к порту

## ⚡ Производительность

### Оптимизации (реализованы)

- ✅ **HikariCP** — пул соединений с настраиваемым размером
- ✅ **PostgreSQL** — полноценная поддержка PostgreSQL (миграции, рантайм, HikariCP-пул) в дополнение к SQLite
- ✅ **Database indexes** — 14 индексов для ускорения запросов
- ✅ **Кэш справочников** — 7 таблиц в Atom, автообновление ~1 раз/день
- ✅ **Type hints** — все reflection warnings устранены
- ✅ **N+1 оптимизация** — JOIN вместо per-worker запросов
- ✅ **Rate limiting** — защита от DDoS/абуза

### Рекомендации для масштабирования

- **Балансировщик нагрузки** для нескольких экземпляров
- **CDN** для статических ресурсов
- **Репликация PostgreSQL** (read replicas) для чтения больших объёмов

Приложение использует **Logback** для логирования всех событий.

### Типы логов

| Файл | Описание | Расположение |
|------|----------|--------------|
| **app.log** | Все логи приложения | `logs/app.log` |
| **error.log** | Только ошибки | `logs/error.log` |
| **audit.log** | Аудит действий пользователей | `logs/audit.log` |
| **app-{дата}.log** | Архивные логи (ежедневная ротация) | `logs/` |

### Уровни логирования

| Уровень | Описание |
|---------|----------|
| **DEBUG** | Отладочная информация (SQL-запросы, детали) |
| **INFO** | Общая информация о работе приложения |
| **WARN** | Предупреждения (валидация не пройдена, запись не найдена) |
| **ERROR** | Ошибки (исключения, сбои) |

### Примеры логов

```
2026-02-05 15:30:45.123 [main] INFO my-ring-app.core - Сервер запускается на порту 3000
2026-02-05 15:31:20.456 [qtp534199755-20] INFO my-ring-app.logger - REQUEST: GET /workers | IP: 127.0.0.1
2026-02-05 15:31:20.789 [qtp534199755-20] INFO my-ring-app.logger - RESPONSE: 200 /workers
2026-02-05 15:32:10.234 [qtp534199755-22] INFO my-ring-app.audit - ACTION: CREATE | ENTITY: Worker | ID: 9 | DETAILS: Создан работник Иванов Петр
2026-02-05 15:33:45.678 [qtp534199755-24] ERROR my-ring-app.logger - ERROR: SQL error | MESSAGE: Ошибка при создании записи
```

### Просмотр логов

```bash
# Просмотр всех логов в реальном времени
tail -f logs/app.log

# Просмотр только ошибок
tail -f logs/error.log

# Просмотр аудита действий
tail -f logs/audit.log
```

Конфигурация логгера находится в файле `resources/logback.xml`.

## 🧮 API Endpoints

### HTML Routes

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/` | Главная страница |
| `GET` | `/dashboard` | Дашборд с аналитикой |
| `GET` | `/login` | Страница входа |
| `POST` | `/login` | Вход в систему |
| `POST` | `/logout` | Выход из системы |
| `GET` | `/profile` | Профиль пользователя |
| `POST` | `/change-password` | Смена пароля |
| `GET` | `/sessions` | Мои сессии |
| `GET` | `/organizations` | Список организаций (admin) |
| `GET` | `/organizations/new` | Форма создания организации |
| `POST` | `/organizations/create` | Создание организации |
| `GET` | `/organizations/:id` | Детали организации |
| `GET` | `/organizations/:id/edit` | Форма редактирования |
| `POST` | `/organizations/:id/update` | Обновление организации |
| `POST` | `/organizations/:id/delete` | Удаление организации |
| `POST` | `/organizations/:id/users/:user-id/role` | Смена роли пользователя |
| `POST` | `/organizations/:id/update-plan` | Смена тарифного плана |
| `GET` | `/workers` | Список работников (с поиском) |
| `GET` | `/workers/new` | Форма создания работника |
| `POST` | `/workers/create` | Создание работника |
| `GET` | `/workers/:id/edit` | Форма редактирования |
| `POST` | `/workers/:id/update` | Обновление работника |
| `POST` | `/workers/:id/delete` | Удаление работника |
| `GET` | `/workers/:id/salary` | Зарплата работника |
| `GET` | `/workers/:id/work-time` | Учёт рабочего времени |
| `GET` | `/work-time/:id/edit` | Редактирование учёта времени |
| `POST` | `/work-time/:id/update` | Обновление учёта времени |
| `GET` | `/shops`, `/ranks`, `/payment-systems`, `/categories`, `/work-modes`, `/salaries`, `/hourly-rates` | Справочники (CRUD) |
| `GET` | `/tariffs` | Тарифные планы (admin) |
| `GET` | `/db` | Просмотр всех таблиц БД |

### REST API

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/api/workers` | Список работников (JSON) |
| `GET` | `/api/workers/:id` | Работник по ID |
| `POST` | `/api/workers` | Создание работника |
| `PUT` | `/api/workers/:id` | Обновление работника |
| `DELETE` | `/api/workers/:id` | Удаление работника |
| `GET` | `/api/workers/search` | Поиск работников |
| `GET` | `/api/salary/:worker-id` | Зарплата работника |
| `GET` | `/api/work-time/:worker-id` | Учёт рабочего времени |
| `PUT` | `/api/work-time/:id` | Обновление учёта времени |
| `GET` | `/api/dashboard` | Данные дашборда |
| `GET` | `/api/dashboard/stats` | Статистика дашборда |
| `GET` | `/api/analytics/workers-by-shop` | Распределение по цехам |
| `GET` | `/api/analytics/workers-by-category` | Распределение по категориям |
| `GET` | `/api/analytics/salary-distribution` | Распределение зарплат |
| `GET` | `/api/export/workers.csv` | Экспорт в CSV |
| `GET` | `/api/export/workers.xlsx` | Экспорт в Excel |
| `GET` | `/api/export/salary.csv` | Экспорт зарплаты |
| `GET` | `/api/health` | Health check |
| `GET` | `/api/ready` | Readiness check |
| `GET` | `/api/live` | Liveness check |
| `GET` | `/api/metrics` | Prometheus метрики |
| `GET` | `/api/stats` | Статистика приложения |
| `GET` | `/api/audit` | Журнал аудита |
| `GET` | `/api/audit/stats` | Статистика аудита |
| `GET` | `/api/audit/:entity-type/:entity-id` | Аудит по сущности |
| `GET` | `/api/audit/user/:username` | Аудит по пользователю |
| `GET` | `/api/sessions` | Все сессии |
| `GET` | `/api/sessions/active` | Активные сессии |
| `GET` | `/api/sessions/failed` | Неудачные входы |
| `GET` | `/api/sessions/stats` | Статистика сессий |
| `GET` | `/api/organizations` | Список организаций |
| `GET` | `/api/organizations/:id` | Организация по ID |
| `POST` | `/api/organizations` | Создание организации |
| `PUT` | `/api/organizations/:id` | Обновление организации |
| `DELETE` | `/api/organizations/:id` | Деактивация организации |
| `GET` | `/api/organizations/:id/users` | Пользователи организации |
| `PUT` | `/api/organizations/:id/users/:user-id/role` | Смена роли |
| `GET` | `/api/tariffs` | Список тарифных планов |
| `GET` | `/api/tariffs/current` | Тариф текущей организации |
| `GET` | `/api/tariffs/org/:id` | Тариф организации (admin) |
| `PUT` | `/api/tariffs/org/:id` | Смена тарифа (admin) |
| `GET` | `/api/tariffs/check-workers` | Проверка лимита работников |
| `GET` | `/api/reports/worker/:id/pdf` | PDF отчёт по работнику |
| `GET` | `/api/reports/workers/pdf` | PDF список работников |
| `GET` | `/api/reports/salary/pdf` | PDF отчёт по зарплате (`?year=&month=`) |
| `GET` | `/api/dashboard/poll` | Быстрый polling дашборда (SSE) |
| `GET` | `/api/migrations` | Статус миграций БД |
| `POST` | `/api/cache/refresh` | Обновление кэша справочников |
| `GET` | `/api-docs` | Swagger UI документация API |
| `GET` | `/api/notifications/test` | Тест SMTP |
| `POST` | `/api/notifications/new-worker` | Уведомление о новом работнике |
| `POST` | `/api/notifications/birthday` | Уведомление о дне рождения |
| `POST` | `/api/notifications/anniversary` | Уведомление о юбилее |
| `GET` | `/api/1c/docs` | Документация по API 1С |

> 💡 **Совместимость:** эндпоинты также доступны с префиксом версии — `/api/v1/...` и `/api/v2/...`

### Auth API

| Метод | Путь | Описание |
|-------|------|----------|
| `POST` | `/api/auth/login` | Вход в систему |
| `POST` | `/api/auth/logout` | Выход из системы |
| `GET` | `/api/auth/profile` | Профиль пользователя |

### 1С Integration API

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/api/1c/workers` | Выгрузка работников для 1С |
| `GET` | `/api/1c/salary` | Выгрузка зарплаты для 1С |
| `POST` | `/api/1c/workers/import` | Импорт работников из 1С |
| `GET` | `/api/1c/docs` | Документация по формату XML |

## 🔧 Устранение проблем

### Частые ошибки и их решение

#### 1. Ошибка: "Could not find artifact"

```bash
# Очистите кэш и переустановите зависимости
lein clean
lein deps
```

#### 2. Ошибка: "Port already in use"

```bash
# Найдите процесс на порту 3000
lsof -i :3000

# Завершите процесс
kill -9 <PID>

# Или используйте другой порт
PORT=8080 lein run
```

#### 3. Ошибка: "Database is locked"

```bash
# Убедитесь, что нет других процессов, работающих с БД
lsof igra.db

# Проверьте права доступа
chmod 644 igra.db
```

#### 4. Ошибка: "Java heap space"

```bash
# Увеличьте память JVM
export JVM_OPTS="-Xmx1g"
lein run
```

#### 5. Ошибка компиляции AOT

```bash
# Полная очистка и пересборка
rm -rf target
lein clean
lein uberjar
```

### Логи для отладки

```bash
# Включите DEBUG-логирование
# Откройте resources/logback.xml и измените уровень:
<root level="DEBUG">

# Просмотр логов в реальном времени
tail -f logs/app.log
```

## 🧪 Тестирование

### Запуск всех тестов

```bash
lein test
```

### Запуск тестов с отладочной информацией

```bash
lein test :verbose
```

### Покрытие кода тестами

```bash
# Установите lein-coverage
lein install plugin lein-coverage "0.2.1"

# Запуск с покрытием
lein coverage
```

### Написание тестов

Тесты находятся в `test/my_ring_app/`:

- **validation_test.clj** — тесты валидации
- **model_test.clj** — тесты модели
- **core_test.clj** — интеграционные тесты
- **util_test.clj** — тесты вспомогательных функций
- **api/export_test.clj** — тесты экспорта
- **auth_test.clj** — тесты аутентификации
- **controllers_test.clj** — тесты контроллеров
- **tariff_test.clj** — тесты тарифных планов
- **session_audit_test.clj** — тесты журнала сессий
- **email_test.clj** — тесты email-уведомлений
- **pdf_reports_test.clj** — тесты PDF-отчётов (включая org-фильтр)
- **api_version_test.clj** — тесты версионирования API
- **sse_test.clj** — тесты SSE
- **logger_test.clj** — тесты логгера
- **routes_test.clj**, **config_test.clj**, **i18n_test.clj**, **layout_buttons_test.clj**, **rate_limit_test.clj** — прочие тесты

Тесты используют изолированную тестовую БД (миграции + инициализация) через `test_helper.clj`.

Пример теста:

```clojure
(deftest test-validate-worker-valid
  (testing "Валидные данные работника"
    (is (:valid? (validate-worker {:фамилия "Иванов"
                                   :имя "Иван"
                                   :дата_приема "2024-01-01"
                                   :цех_id "1"})))))
```

### Статус тестов

| Файл | Тестов | Утверждений | Статус |
|------|--------|-------------|--------|
| auth_test.clj | 44 | 94 | ✅ |
| controllers_test.clj | 29 | 50 | ✅ |
| routes_test.clj | 20 | 26 | ✅ |
| session_audit_test.clj | 20 | 46 | ✅ |
| tariff_test.clj | 15 | 52 | ✅ |
| api/workers_test.clj | 14 | 43 | ✅ |
| i18n_test.clj | 11 | 18 | ✅ |
| validation_test.clj | 10 | 35 | ✅ |
| pdf_reports_test.clj | 9 | 42 | ✅ |
| rate_limit_test.clj | 7 | 10 | ✅ |
| logger_test.clj | 7 | 9 | ✅ |
| util_test.clj | 6 | 54 | ✅ |
| model_test.clj | 6 | 10 | ✅ |
| cache_test.clj | 5 | 18 | ✅ |
| email_test.clj | 5 | 10 | ✅ |
| api/export_test.clj | 4 | 19 | ✅ |
| layout_buttons_test.clj | 4 | 12 | ✅ |
| migration_test.clj | 4 | 9 | ✅ |
| config_test.clj | 2 | 3 | ✅ |
| core_test.clj | 2 | 5 | ✅ |
| api_version_test.clj | 2 | 6 | ✅ |
| sse_test.clj | 2 | 11 | ✅ |
| **Итого** | **228** | **582** | **✅** |

## 🚀 Развёртывание

### Развёртывание на Linux-сервере

#### 1. Подготовка сервера

```bash
# Установка Java и SQLite
sudo apt update
sudo apt install openjdk-17-jdk sqlite3

# Создание пользователя для приложения
sudo useradd -m -s /bin/bash myringapp
sudo mkdir -p /opt/my-ring-app
sudo chown myringapp:myringapp /opt/my-ring-app
```

#### 2. Копирование приложения

```bash
# Копируйте standalone JAR на сервер
scp target/uberjar/my-ring-app-0.1.0-SNAPSHOT-standalone.jar myringapp@server:/opt/my-ring-app/

# Копируйте базу данных
scp igra.db myringapp@server:/opt/my-ring-app/
```

#### 3. Создание systemd-сервиса

Создайте файл `/etc/systemd/system/my-ring-app.service`:

```ini
[Unit]
Description=My Ring App - HR Management System
After=network.target

[Service]
Type=simple
User=myringapp
WorkingDirectory=/opt/my-ring-app
ExecStart=/usr/bin/java -Xms512m -Xmx1g -jar /opt/my-ring-app/my-ring-app-0.1.0-SNAPSHOT-standalone.jar
Restart=on-failure
Environment="PORT=3000"
Environment="ENV=production"

# Логирование
StandardOutput=journal
StandardError=journal
SyslogIdentifier=my-ring-app

[Install]
WantedBy=multi-user.target
```

#### 4. Запуск сервиса

```bash
# Перезагрузка systemd
sudo systemctl daemon-reload

# Включение автозапуска
sudo systemctl enable my-ring-app

# Запуск
sudo systemctl start my-ring-app

# Проверка статуса
sudo systemctl status my-ring-app

# Просмотр логов
journalctl -u my-ring-app -f
```

### Развёртывание с nginx (reverse proxy)

#### 1. Установка nginx

```bash
sudo apt install nginx
```

#### 2. Конфигурация nginx

Создайте файл `/etc/nginx/sites-available/my-ring-app`:

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Статические файлы (опционально)
    location /static {
        alias /opt/my-ring-app/resources/public;
        expires 30d;
    }
}
```

#### 3. Включение сайта

```bash
sudo ln -s /etc/nginx/sites-available/my-ring-app /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

#### 4. Настройка HTTPS (Let's Encrypt)

```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
```

## 📊 Мониторинг

### Проверка работоспособности

```bash
# Проверка доступности приложения
curl -I http://localhost:3000

# Проверка статуса сервиса
sudo systemctl status my-ring-app

# Просмотр логов в реальном времени
journalctl -u my-ring-app -f
```

### Метрики для мониторинга

- **HTTP статус коды** (ошибки 4xx, 5xx)
- **Время ответа** (среднее, 95-й перцентиль)
- **Использование памяти** (heap/non-heap)
- **Активные подключения** к БД
- **Размер базы данных**

## 🔧 Конфигурация

### Переменные окружения

| Переменная | Значение по умолчанию | Описание |
|------------|----------------------|----------|
| `PORT` | `3000` | Порт веб-сервера |
| `ENV` | `development` | Окружение (development/production) |
| `SESSION_SECRET` | `d3v-s3cr3t-k3y!1` | Секрет для сессий (обязателен в production) |
| `ADMIN_PASSWORD` | `admin` | Пароль администратора (обязателен в production) |
| `DB_TYPE` | `sqlite` | Тип БД (sqlite/postgresql) |
| `DB_USER` | `-` | Пользователь PostgreSQL (обязателен при `DB_TYPE=postgresql`) |
| `DB_PASSWORD` | `-` | Пароль PostgreSQL (обязателен при `DB_TYPE=postgresql`) |
| `DB_HOST` | `localhost` | Хост PostgreSQL |
| `DB_PORT` | `5432` | Порт PostgreSQL |
| `DB_NAME` | `my_ring_app` | Имя базы данных PostgreSQL |
| `DATABASE_URL` | `-` | Полный URL подключения PostgreSQL (`jdbc:postgresql://...`) — переопределяет DB_* |
| `DB_POOL_MAX` | `10` | Макс. размер пула соединений (PostgreSQL) |
| `DB_POOL_MIN` | `2` | Мин. размер пула соединений (PostgreSQL) |
| `JVM_OPTS` | `-` | Опции JVM (например, `-Xmx1g`) |
| `SMTP_HOST` | `-` | SMTP-сервер (если задан — email-уведомления включены) |
| `SMTP_PORT` | `587` | Порт SMTP |
| `SMTP_USER` | `-` | Пользователь SMTP (если требуется авторизация) |
| `SMTP_PASSWORD` | `-` | Пароль SMTP |
| `SMTP_FROM` | `SMTP_USER` | Адрес отправителя (по умолчанию `noreply@localhost`) |
| `SMTP_TLS` | `true` | Использовать STARTTLS |
| `SMTP_SSL` | `false` | Использовать SSL |
| `SMTP_DEBUG` | `false` | Отладочный вывод SMTP |

### Конфигурация логгера

Файл: `resources/logback.xml`

```xml
<configuration>
  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>

  <root level="INFO">
    <appender-ref ref="STDOUT" />
  </root>
</configuration>
```

## ❓ FAQ (Частые вопросы)

### Как изменить порт по умолчанию?

```bash
# Через переменную окружения
PORT=8080 lein run

# Или при запуске JAR
java -Dport=8080 -jar app.jar
```

### Как создать резервную копию базы данных?

```bash
# Экспорт в SQL-файл
sqlite3 igra.db ".dump" > backup-$(date +%Y%m%d).sql

# Или копирование файла БД
cp igra.db igra.db.backup
```

### Как сбросить базу данных к начальному состоянию?

```bash
# Удаление текущей БД
rm igra.db

# Создание из дампа
sqlite3 igra.db < igra.db.sql
```

### Как добавить нового пользователя в базу?

Используйте веб-интерфейс:
1. Откройте http://localhost:3000
2. Нажмите "Работники" → "Добавить работника"
3. Заполните форму и сохраните

### Можно ли использовать PostgreSQL вместо SQLite?

Да, но потребуется:
1. Добавить зависимость `[org.postgresql/postgresql "42.6.0"]` в `project.clj`
2. Изменить `config.clj` для подключения к PostgreSQL
3. Мигрировать схему БД

### Как обновить приложение в продакшене?

```bash
# 1. Собрать новую версию
lein uberjar

# 2. Копировать на сервер
scp target/uberjar/*.jar user@server:/opt/my-ring-app/

# 3. Перезапустить сервис
sudo systemctl restart my-ring-app
```

## 🤝 Вклад в проект

Приветствуется любой вклад в развитие проекта!

### Как внести свой вклад

1. Создайте форк репозитория
2. Создайте ветку для вашей фичи (`git checkout -b feature/amazing-feature`)
3. Убедитесь, что тесты проходят (`lein test`)
4. Закоммитьте изменения (`git commit -m 'Add amazing feature'`)
5. Отправьте в ветку (`git push origin feature/amazing-feature`)
6. Откройте Pull Request

### Требования к коду

- Следуйте [Clojure Style Guide](https://github.com/bbatsov/clojure-style-guide)
- Добавляйте тесты для нового функционала
- Документируйте публичные функции
- Обновляйте README.md при изменении API

### Идеи для улучшения

- [x] Аутентификация и авторизация
- [x] REST API для интеграций
- [x] Экспорт данных в CSV/Excel/PDF
- [x] Печать отчётов (PDF)
- [x] Дашборд с метриками
- [x] Поддержка мультиязычности
- [x] Миграция на PostgreSQL
- [x] Миграции БД
- [x] Кэширование справочников
- [x] CSS в отдельных файлах
- [x] Connection pooling (HikariCP)
- [x] Мульти-тенантность (организации, тарифы)
- [x] Журнал сессий и аудит
- [x] Email-уведомления
- [x] Версионирование API (v1/v2)
- [ ] Покрытие тестами > 70%
- [ ] OAuth2
- [ ] WebSocket

## 📄 Лицензия

Распространяется по лицензии **Eclipse Public License 2.0** (см. файл [LICENSE](LICENSE)).

## 👤 Автор

**Alexander Vashurin**

## 📞 Контакты

- GitHub: [@alexandervashurin](https://github.com/alexandervashurin)
- Email: [alexandervashurin@yandex.ru](mailto:alexandervashurin@yandex.ru)

## 📈 Статус проекта

| Параметр | Значение |
|----------|----------|
| **Версия** | 0.1.0-SNAPSHOT |
| **Статус** | Production Ready |
| **Последнее обновление** | Июль 2026 |
| **Тесты** | 228 тестов, 582 утверждений ✅ |
| **Сборка** | [![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]() |

## 🙏 Благодарности

- [Ring](https://github.com/ring-clojure/ring) — веб-фреймворк для Clojure
- [Compojure](https://github.com/weavejester/compojure) — библиотека маршрутизации
- [Clojure](https://clojure.org/) — язык программирования
- [Leiningen](https://leiningen.org/) — управление проектами

## 📚 Дополнительные ресурсы

- [USER_GUIDE.md](USER_GUIDE.md) — руководство пользователя
- [DOCUMENTATION.md](DOCUMENTATION.md) — техническая документация
- [CHANGELOG.md](CHANGELOG.md) — журнал изменений
- [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md) — план развития
- [DOCKER.md](DOCKER.md) — развёртывание в Docker
- [POSTGRES_DEPLOYMENT.md](POSTGRES_DEPLOYMENT.md) — развёртывание на PostgreSQL
- [Clojure для начинающих](https://clojure-doc.org/)
- [Ring Tutorial](https://github.com/ring-clojure/ring/wiki/Tutorial)
- [Compojure Wiki](https://github.com/weavejester/compojure/wiki)
- [ClojureDocs](https://clojuredocs.org/) — документация и примеры

---

<div align="center">
  <strong>🏭 Система управления персоналом</strong><br>
  CRUD-приложение на Clojure/Ring для управления базой данных работников
</div>

<div align="center">

[Вверх](#-система-управления-персоналом-crud-приложение-на-clojurering)

</div>

# 🏭 Система управления персоналом (CRUD-приложение на Clojure/Ring)

[![Clojure](https://img.shields.io/badge/Clojure-1.11.1-blue.svg)](https://clojure.org/)
[![License](https://img.shields.io/badge/license-EPL--2.0-green.svg)](LICENSE)
[![Ring](https://img.shields.io/badge/Ring-1.9.6-purple.svg)](https://github.com/ring-clojure/ring)
[![SQLite](https://img.shields.io/badge/SQLite-3.x-lightgrey.svg)](https://www.sqlite.org/)

**Система управления персоналом** — полноценное веб-приложение для управления базой данных работников предприятия с поддержкой всех операций CRUD (Create, Read, Update, Delete).

## 📸 Скриншоты

### Главная страница
![Главная страница](screenshots/main.png)

### Список работников
![Список работников](screenshots/workers-list.png)

### Форма добавления работника
![Форма добавления](screenshots/add-worker.png)

### Форма редактирования
![Форма редактирования](screenshots/edit-worker.png)

## ✨ Функциональность

### Основные возможности:
- ✅ **Просмотр** списка всех работников с расширенной информацией
- ✅ **Добавление** новых работников в базу данных
- ✅ **Редактирование** существующих записей
- ✅ **Удаление** работников с подтверждением
- ✅ **Умные формы** с выпадающими списками для справочников
- ✅ **Автоматическое переключение** между окладом и почасовой ставкой
- ✅ **Просмотр всех таблиц** базы данных
- ✅ **Современный адаптивный интерфейс**
- ✅ **Поиск работников** по ФИО и цеху

### Работа со справочниками:
- 📋 Цеха
- 💰 Системы оплаты труда
- 👥 Категории работников
- 🔢 Разряды
- 🕐 Режимы работы
- 💸 Оклады
- ⏱️ Почасовые ставки

### Дополнительные функции:
- 📊 **Расчёт зарплаты** — просмотр информации о зарплате работника
- 🕐 **Учёт рабочего времени** — отслеживание отработанных часов, больничных и командировочных дней
- 🗄️ **Просмотр БД** — отображение всех таблиц базы данных с содержимым

## 🛠 Технологический стек

| Технология | Версия | Назначение |
|------------|--------|------------|
| **Clojure** | 1.11.1 | Язык программирования |
| **Ring** | 1.9.6 | Web-фреймворк |
| **Compojure** | 1.7.0 | Роутинг |
| **Jetty** | встроенный | Веб-сервер |
| **java.jdbc** | 0.7.12 | Работа с базой данных |
| **SQLite JDBC** | 3.45.1.0 | Драйвер SQLite |
| **SQLite** | 3.x | База данных |
| **SLF4J + Logback** | 2.0.9 / 1.4.11 | Логирование |
| **tools.logging** | 1.2.4 | Логирование на Clojure |

## 📋 Требования

- **Java** 17 или выше
- **Leiningen** 2.10.0 или выше
- **SQLite** (встроен в приложение через JDBC)

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

## 📁 Структура проекта

```
my-ring-app/
├── src/
│   └── my_ring_app/
│       ├── core.clj           # Точка входа, основной middleware
│       ├── routes.clj         # Маршруты (Compojure)
│       ├── controllers.clj    # Контроллеры (обработка запросов)
│       ├── model.clj          # Модель данных (работа с БД)
│       ├── validation.clj     # Валидация данных
│       ├── logger.clj         # Логирование
│       ├── config.clj         # Конфигурация
│       └── views/
│           ├── layout.clj     # Общий HTML-шаблон
│           ├── home.clj       # Главная страница
│           ├── workers.clj    # Страница работников
│           ├── salary.clj     # Страница зарплаты
│           ├── work_time.clj  # Учёт рабочего времени
│           └── tables.clj     # Просмотр таблиц БД
├── resources/
│   └── logback.xml            # Конфигурация логгера
├── test/
│   └── my_ring_app/
│       └── core_test.clj      # Тесты
├── doc/
│   └── intro.md               # Документация
├── screenshots/               # Скриншоты интерфейса
├── igra.db                    # База данных SQLite
├── igra.db.sql                # Дамп базы данных
├── project.clj                # Конфигурация Leiningen
└── README.md                  # Этот файл
```

## 📝 Логирование

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

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/` | Главная страница |
| `GET` | `/workers` | Список всех работников (с поиском) |
| `GET` | `/workers/new` | Форма создания работника |
| `POST` | `/workers/create` | Создание работника |
| `GET` | `/workers/:id/edit` | Форма редактирования работника |
| `POST` | `/workers/:id/update` | Обновление работника |
| `POST` | `/workers/:id/delete` | Удаление работника |
| `GET` | `/workers/:id/salary` | Зарплата работника |
| `GET` | `/workers/:id/work-time` | Учёт рабочего времени |
| `GET` | `/work-time/:id/edit` | Редактирование учёта времени |
| `POST` | `/work-time/:id/update` | Обновление учёта времени |
| `GET` | `/db` | Просмотр всех таблиц БД |

## 🧪 Тестирование

```bash
lein test
```

## 🔧 Конфигурация

### Переменные окружения

| Переменная | Значение по умолчанию | Описание |
|------------|----------------------|----------|
| `PORT` | `3000` | Порт веб-сервера |
| `ENV` | `development` | Окружение (development/production) |

### База данных

Приложение использует SQLite базу данных `igra.db`. Схема и начальные данные находятся в файле `igra.db.sql`.

## 📦 Зависимости

Основные зависимости указаны в [`project.clj`](project.clj):

```clojure
[org.clojure/clojure "1.11.1"]
[ring/ring-core "1.9.6"]
[ring/ring-jetty-adapter "1.9.6"]
[ring/ring-defaults "0.3.4"]
[ring/ring-json "0.5.1"]
[compojure "1.7.0"]
[org.clojure/java.jdbc "0.7.12"]
[org.xerial/sqlite-jdbc "3.45.1.0"]
[org.slf4j/slf4j-api "2.0.9"]
[ch.qos.logback/logback-classic "1.4.11"]
[org.clojure/tools.logging "1.2.4"]
```

## 🤝 Вклад в проект

1. Создайте форк репозитория
2. Создайте ветку для вашей фичи (`git checkout -b feature/amazing-feature`)
3. Закоммитьте изменения (`git commit -m 'Add amazing feature'`)
4. Отправьте в ветку (`git push origin feature/amazing-feature`)
5. Откройте Pull Request

## 📄 Лицензия

Распространяется по лицензии **Eclipse Public License 2.0** (см. файл [LICENSE](LICENSE)).

## 👤 Автор

**Alexander Vashurin**

## 📞 Контакты

- GitHub: [@alexandervashurin](https://github.com/alexandervashurin)

## 🙏 Благодарности

- [Ring](https://github.com/ring-clojure/ring) — веб-фреймворк для Clojure
- [Compojure](https://github.com/weavejester/compojure) — библиотека маршрутизации
- [Clojure](https://clojure.org/) — язык программирования

---

<div align="center">
  <strong>🏭 Система управления персоналом</strong><br>
  CRUD-приложение на Clojure/Ring для управления базой данных работников
</div>

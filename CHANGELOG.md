# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased] - 2026-07-28

### ✨ New Features (Q1 2027)
- **DB миграции**: `migration.clj` — лёгкая система миграций с таблицей `schema_migrations`, поддержка Up/Down, идемпотентный запуск
- **Миграция 001**: `001_initial_schema.sql` — фиксирует схему таблиц Пользователь + Аудит_изменений
- **Кэш справочников**: `cache.clj` — Atom-based кэш для 7 справочных таблиц, автообновление ~1 раз/день, ручной refresh
- **API миграций**: `GET /api/migrations` (admin) — статус всех миграций
- **API кэша**: `POST /api/cache/refresh` (admin) — принудительное обновление кэша
- **Клиентская валидация**: `validation.js` — валидация форм работника и учета времени на клиенте (длина полей, формат даты, условная валидация оклад/ставка)
- **Извлечение CSS**: инлайн-стили из `views/auth.clj` перенесены в `app.css`

### ⚡ Performance
- **Reflection warnings**: все reflection warnings устранены (type hints в `export.clj` для Apache POI, `migration.clj` для File I/O)
- **Кэш справочников**: `load-worker-form-data` читает из кэша вместо 7 запросов к БД при каждом рендер формы

### 🧪 Tests
- Новые тесты: `cache_test.clj` (load-all!, getters, cache-status, data consistency) — 5 тестов, 18 утверждений
- Новые тесты: `migration_test.clj` (status, applied detection, idempotent run, rollback) — 4 теста
- Итого: **51 тест, 192 утверждения, 0 ошибок**

### 🔧 Refactoring (массовый)
- Удалён мёртвый код `defroutes api-routes` из всех 9 API namespace'ов (dashboard, monitoring, workers, salary, export, audit, reports, onec, notifications)
- Удалены неиспользуемые require: `compojure.core` (defroutes/GET/POST), `clojure.string` (из monitoring, salary, export), `views.layout` (из export)
- Созданы общие хелперы в `util.clj`: `json-response`, `pagination-meta`
- Извлечён `parse-work-time-params` в `util.clj` (убрано дублирование controllers.clj + api/salary.clj)
- Удалён `get-spravochnik` (дублировал `get-table-data`), все вызовы заменены
- Удалён алиас `validate-worker-update` (вызовы используют `validate-worker` напрямую)
- Удалены неиспользуемые функции из `auth.clj`: `logged-in?`, `get-current-user`
- Удалён мёртвый `render-distribution-charts` из `views/dashboard.clj`
- Удалена неиспользуемая зависимость `clojure.string` из `views/workers.clj`

### ⚡ Performance
- Исправлен N+1 запрос в `monitoring.clj` app-statistics — теперь использует `get-salary-with-details` вместо пофраерных запросов
- `get-db-stats` переиспользует `get-dashboard-stats` (COUNT-запросы) вместо загрузки всех работников
- `require-role` предвычисляет `allowed-roles` как set (аллоцировался per-request)

### 🔒 Security
- `SESSION_SECRET` и `ADMIN_PASSWORD` теперь выбрасывают `IllegalStateException` в production если не заданы (вместо дефолтных значений)

### 🐛 Bug Fixes
- Исправлен unsafe `Integer/parseInt` → `util/parse-int` в `api/salary.clj` update-work-time
- Исправлена несогласованность `clojure.string` full-path vs alias (helpers.clj, dashboard.clj, controllers.clj, api/workers.clj)
- Исправлен double-read в `i18n.clj` (лишний `with-open` + `slurp`)
- Перенесён прямой JDBC из `api/audit.clj` в model layer (новые функции `get-audit-count-by-action`, `get-audit-count-by-entity`)
- Добавлен `get-salary-with-details` в model.clj (JOIN запрос для экспорта и мониторинга)

### 📝 Documentation
- Обновлён DEVELOPMENT_PLAN.md — план на Q1 2027
- Обновлён CHANGELOG.md

### 🏗️ Infrastructure
- Добавлены GitHub Actions workflow'и: тесты + uberjar, Docker build + push to GHCR, SSH deploy

---

## [Unreleased] - 2026-07-27

### 🔒 Security
- Добавлено HTML-экранирование всех пользовательских данных для защиты от XSS-атак
- Добавлены заголовки безопасности (X-Frame-Options, X-Content-Type-Options, X-XSS-Protection, Content-Security-Policy)
- Добавлена защита от CSRF-атак через middleware
- Добавлена проверка активности пользователя при каждом запросе (session revalidation)
- Добавлена защита от brute-force атак (5 неудачных попыток → 15 минут блокировки)
- Добавлена защита от mass assignment при обновлении пользователя
- Добавлен флаг Secure для сессионных cookies
- Добавлено предупреждение при запуске с паролем admin по умолчанию
- Добавлено предупреждение при запуске с дефолтным SECRET_KEY

### 🛡️ Error Handling
- Добавлен глобальный обработчик необработанных исключений (wrap-error-handler)
- Улучшена обработка ошибок при создании/обновлении/удалении работников
- Добавлено безопасное преобразование строк в числа (parse-int)
- Добавлена типизация для логирования ошибок
- Исправлена обработка division-by-zero в вычислениях
- Добавлена обработка NPE в update-work-time

### ✅ Testing
- Добавлены comprehensive тесты для валидации работников (13 тестов)
- Добавлены тесты для валидации учёта рабочего времени (5 тестов)
- Добавлены тесты для функций модели (6 тестов)
- Добавлены интеграционные тесты для core функций
- Добавлены тесты для вспомогательных функций (util.clj) — 21 тест
- Добавлены тесты для экспорта данных — 8 тестов
- Всего: 42 теста, 165 утверждений

### 🔧 Refactoring
- Улучшена структура middleware в core.clj
- Выделены отдельные функции для обработки ошибок
- Убраны reflection warnings добавлением type hints
- Улучшена обработка ID (валидация и очистка)
- Создан общий namespace `util.clj` для вспомогательных функций
- Рефакторинг API endpoints для использования общих функций

### 🐛 Bug Fixes
- Исправлена проблема компиляции AOT для middleware
- Исправлены проблемы с обработкой исключений в контроллерах
- Улучшена обработка пустых значений в формах
- Исправлена ошибка поиска (seq → vec)
- Исправлена ошибка экспорта Excel (POI .getIndex)
- Исправлена ошибка создания записи (ID extraction для SQLite)
- Устранено дублирование запросов к БД
- Добавлена валидация year/month в salary API
- Добавлена валидация email в notification endpoints
- Исправлен unsafe parseInt в workers API
- Добавлена валидация ID в PDF endpoints
- Добавлена валидация table names

### 📝 Documentation
- Полностью переписан README.md с добавлением:
  - Детальной структуры проекта
  - Примеров использования API endpoints
  - Инструкций по развёртыванию
  - Документации по логированию
  - Таблиц технологического стека
- Обновлён CHANGELOG.md

---

## [0.1.1] - 2026-02-04
### Changed
- Documentation on how to make the widgets.

### Removed
- `make-widget-sync` - we're all async, all the time.

### Fixed
- Fixed widget maker to keep working when daylight savings switches over.

## 0.1.0 - 2026-02-04
### Added
- Files from the new template.
- Widget maker public API - `make-widget-sync`.

[Unreleased]: https://github.com/alexandervashurin/my-ring-app/compare/0.1.1...HEAD
[0.1.1]: https://github.com/alexandervashurin/my-ring-app/compare/0.1.0...0.1.1

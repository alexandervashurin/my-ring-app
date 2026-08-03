# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased] - 2026-08-03

### 🔒 Security
- **IDOR-фикс в model**: `get-record-by-id` и `get-work-time-by-id` получили arity с `org-id` — записи чужих организаций больше не возвращаются (ранее любой пользователь мог читать/менять данные работников и учёта времени другой организации по ID)
- **IDOR-фикс в API**: `GET /api/workers/:id`, `PUT /api/workers/:id`, `PUT /api/work-time/:id` возвращают 404 для записей другой организации
- **IDOR-фикс в HTML-контроллерах**: формы редактирования/просмотра работника и учёта времени проверяют принадлежность к организации и редиректят на `/workers` при чужой записи

### 🐛 Bug Fixes
- **`validation/validate-work-time`**: падал с `IllegalArgumentException` (500) при числовых JSON-значениях из API (`empty?`/`re-matches` не работают с Long); значения нормализуются в строки через `str-value`
- **Миграция 004**: при переносе работников в организации 2/3 не обновлялись `organization_id` записей учёта времени и начислений — нарушен инвариант мульти-тенантности; добавлены `UPDATE`-синхронизации (Up и Down)

### 🧪 Tests
- **257 тестов, 653 утверждения** (+15 тестов, +19 утверждений)
- Новые: IDOR-тесты API работников (`workers_test`), новый файл `salary_test` (зарплата/учёт времени, org-scoping), org-scoping HTML-контроллеров, `validate-work-time` с числовыми значениями, реальные тренды дашборда

## [Unreleased] - 2026-08-03

### ⚡ Performance
- **Пагинация HTML-списка работников**: `model/get-workers-page` выполняет `LIMIT/OFFSET` на уровне SQL (вместо загрузки всех записей), `model/count-workers` — через `COUNT(*)`
- **Общий SELECT для списков**: из `get-workers-with-details`/`search-workers` вынесен общий `workers-list-select`, поиск переиспользует общий билдер запросов `workers-page-query`
- **Страница `/db`**: `get-table-data` получил необязательный `limit` (первые 200 строк), добавлен `count-table-rows` через `COUNT(*)` — страница больше не загружает все строки всех таблиц

### 🎨 UI/UX
- **Постраничная навигация** на странице работников: хелпер `views/helpers/render-pagination` (назад/вперёд + номера страниц), поиск сохраняется между страницами через query-параметр `search`
- **Счётчик записей** на странице `/db`: `<small class='table-count'>` показывает общее число записей и «показаны первые N»

### 🔒 Security
- Заголовки `Cache-Control: no-store, no-cache, must-revalidate` и `Pragma: no-cache` добавлены в `wrap-security-headers` — конфиденциальные данные (зарплаты, ФИО) не кэшируются

### 🧪 Tests
- **236 тестов, 612 утверждений** (+8 тестов, +30 утверждений)
- Новые: пагинация страницы работников (`controllers_test`), `get-workers-page`/`count-workers`/`get-table-data` limit/`count-table-rows`, security headers (`core_test`)

## [Unreleased] - 2026-08-03 (вторая партия)

### 🔒 Security
- **Org-scoping экспорта**: `api/export.clj` передаёт `(:org-id request)` в `get-workers-with-details`/`get-salary-with-details` — пользователь видит только свою организацию (раньше любой аутентифицированный пользователь мог выгрузить CSV/XLSX всех организаций). Админ может указать организацию через `?org_id=` (без параметра — все организации)
- **Экспорт-тесты изолированы** от dev-БД: добавлен fixture `setup-db` в `export_test.clj`

### ✨ New Features
- **`/api/export/salary.xlsx`** — экспорт зарплаты в Excel (паритет с работниками). Общие хелперы `records-to-csv`/`records-to-excel` в `api/export.clj` (убрана дупликация конвертеров)

### 🎨 UI/UX
- **Дашборд без фейковых трендов**: удалены хардкод «📈 +2.5%» / «📉 -1.2%» / «Стабильно» и неиспользуемый `render-payroll-chart`; фонд оплаты труда показывает реальную динамику «к прошлому месяцу» (или «нет данных за прошлый месяц»)

### 🔧 Refactoring
- **Маршруты справочников**: 48 строк → макрос `reference-routes` в `routes.clj` (8 справочников по одной строке)

### 🧪 Tests
- **242 теста, 634 утверждения** (+6 тестов, +22 утверждения)
- Новые: org-scoping экспорта CSV/Excel (пользователь, админ с `?org_id=`, админ без параметра), `salary.xlsx`, отсутствие фейковых трендов на дашборде

## [Unreleased] - 2026-07-31

### ✨ New Features

**PDF-отчёты (clj-pdf 2.6.1):**
- Новый модуль `pdf_reports.clj` — реальная генерация PDF вместо заглушек
- **PDF по работнику**: `/api/reports/worker/:id/pdf` — карточка работника с ФИО, цехом, категорией, окладом/ставкой, зарплатой за месяц
- **PDF список работников**: `/api/reports/workers/pdf` — таблица работников (landscape)
- **PDF отчёт по зарплате**: `/api/reports/salary/pdf?year=&month=` — сводная таблица с итоговой строкой
- **Кириллица**: рендеринг через TTF-шрифт (DejaVuSans), `:encoding :unicode`
- **Org-фильтр**: все три генератора принимают `org-id` и ограничивают данные организацией
- Зависимость `clj-pdf 2.6.1` добавлена в `project.clj`

**Орг-фильтр в PDF:**
- `pdf_reports.clj`: новые arity `[x y org-id]` для всех генераторов (старые сохранены)
- `api/reports.clj`: передаёт `(:org-id request)` в генераторы

### 🐛 Bug Fixes
- **`model.clj`**: `(jdbc/query db-spec [(str query) params])` в java.jdbc 0.7.12 не разворачивает вектор параметров → пустые результаты. Заменено на `(into [query] params)` в `get-worker-salary`, `get-worker-salary-history`, `get-worker-work-time`
- **`api_version.clj`**: `wrap-api-v1-rewrite` давал двойной слэш `/api//workers` → запросы `/api/v1/*` фактически не маршрутизировались; исправлено на `(str "/api" (subs uri 7))`

### 🧪 Tests
- **228 тестов, 582 утверждений, 0 failures/0 errors** (+48 тестов, +121 утверждение)
- Новые: `api_version_test.clj` (2), `email_test.clj` (5), `sse_test.clj` (2), `logger_test.clj` (7)
- `pdf_reports_test.clj` расширен org-фильтрами (3 новых теста: worker-org, workers-list-org, salary-org; итого 9 тестов/42 assertions)
- Тесты изолированы от dev-БД: `test_helper.clj` применяет миграции + `auth/init-db!` в отдельной тестовой БД

### 🚀 Deployment
- Прод запущен вручную: `lein uberjar` + `java -jar` (порт 3000, `ENV=production`, `SESSION_SECRET`)
- GitHub Actions-деплой недоступен: хост в частной сети, SSH на порту 50000, отсутствуют необходимые секреты
- Настроены GitHub secrets: `DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_KEY` (ed25519, порт 50000)

### 📝 Documentation
- Добавлен `USER_GUIDE.md` — руководство пользователя (вход, роли, работники, справочники, учёт времени, зарплата, дашборд, организации/тарифы, отчёты/экспорт, мониторинг, FAQ)
- Актуализированы `README.md`, `DOCUMENTATION.md`, `CHANGELOG.md`, `DEVELOPMENT_PLAN.md`

## [Unreleased] - 2026-07-29

### ✨ New Features

**Q3 2027 — Тарифные планы:**
- **Таблицa `Тарифный_план`**: Free/Pro/Enterprise с лимитами работников, организаций и функциями
- **Миграция 006**: колонка `plan_id` в `Организация`, сидирование 3 планов
- **Новый модуль `tariff.clj`**: загрузка планов из БД, проверка лимитов, middleware `require-feature`
- **Проверка лимитов**: создание работника блокируется при превышении лимита тарифа
- **API тарифов**: `/api/tariffs`, `/api/tariffs/current`, `/api/tariffs/org/:id`, `/api/tariffs/check-workers`
- **UI тарифов**: страница организации показывает тариф, использование работников, форму смены плана
- **`format-organization`**: API возвращает данные тарифа в ответе организации
- **`allowed-tables`**: `Тарифный_план` добавлен в белый список

**Q3 2027 — Роли организации:**
- **Организационные роли**: `org_role` на уровне организации — `org_admin`, `org_manager`, `org_hr`, `org_viewer`
- **Миграция 005**: добавлена колонка `org_role` в таблицу `Пользователь`
- **Новый middleware**: `require-org-role` для проверки ролей на уровне организации
- **Эффективные права**: `get-effective-permissions` объединяет глобальные и организационные права
- **Управление ролями**: новый API `/api/organizations/:id/users` и `/api/organizations/:id/users/:user-id/role`
- **UI ролей**: страница организации показывает пользователей с возможностью смены org_role
- **Идемпотентные миграции**: `migration.clj` — ALTER TABLE ADD COLUMN пропускается, если колонка уже существует

**Q3 2027 — Аудит сессий:**
- **Миграция 007**: таблица `Сессия` с индексами для логирования входов/выходов
- **Новый модуль `session_audit.clj`**: `log-login!`, `log-logout!`, `get-user-sessions`, `get-recent-sessions`, `get-failed-logins`, `get-active-sessions`, `get-session-count-by-day`
- **Интеграция с `auth.clj`**: `authenticate` логирует успешные/неудачные попытки входа в БД
- **Интеграция с `controllers/auth.clj`**: logout логирует завершение сессии
- **API сессий**: `/api/sessions`, `/api/sessions/active`, `/api/sessions/failed`, `/api/sessions/stats`
- **UI сессий**: страница `/sessions` с таблицами истории, активных сессий и неудачных попыток
- **`allowed-tables`**: `Сессия` добавлен в белый список
- **`format-session`**: исправлен расчёт длительности (Duration/between вместо `-`)

### 🧪 Tests
- **180 тестов, 461 утверждений** (+20 тестов, +46 утверждений)
- Новые: `session_audit_test.clj` — 20 тестов для логирования, получения, фильтрации и форматирования сессий

## [Unreleased] - 2026-07-28

### ✨ New Features

**Q1 2027:**
- **DB миграции**: `migration.clj` — лёгкая система миграций с таблицей `schema_migrations`, поддержка Up/Down, идемпотентный запуск
- **Миграции 001+002**: начальная схема + 14 индексов для оптимизации запросов
- **Кэш справочников**: `cache.clj` — Atom-based кэш для 7 справочных таблиц, автообновление ~1 раз/день
- **Клиентская валидация**: `validation.js` — валидация форм работника и учета времени на клиенте
- **Извлечение CSS**: инлайн-стили из `views/auth.clj` перенесены в `app.css`

**Q2 2027:**
- **Connection Pooling**: HikariCP — пул соединений с настройкой через env vars (DB_POOL_MAX, DB_POOL_MIN)
- **Database Indexes**: 14 индексов для ускорения JOIN, поиска, зарплатных запросов и аудита
- **Rate Limiting**: скользящее окно по IP (30 req/min для API, 100 req/min для страниц)
- **Dashboard Polling**: `GET /api/dashboard/poll` — быстрый эндпоинт для реалтайм обновлений
- **API Versioning**: `/api/v1/*` маршруты с обратной совместимостью `/api/*`
- **OpenAPI/Swagger**: полная документация API на `/api-docs`

### ⚡ Performance
- HikariCP connection pooling для PostgreSQL и SQLite
- 14 database indexes для оптимизации JOIN и поисковых запросов
- Кэш справочников — `load-worker-form-data` читает из кэша вместо 7 запросов к БД
- N+1 оптимизация в `monitoring.clj` app-statistics
- `get-db-stats` переиспользует `get-dashboard-stats` (COUNT-запросы)
- `require-role` предвычисляет `allowed-roles` как set
- Все reflection warnings устранены (type hints в export.clj, migration.clj)

### 🔒 Security
- `SESSION_SECRET` и `ADMIN_PASSWORD` выбрасывают `IllegalStateException` в production
- Rate limiting middleware — защита от DDoS/абуза (30 API req/min, 100 page req/min)
- XSS, CSRF, Clickjacking, CSP, Secure Cookie, Brute-force protection

### 🧪 Tests
- **51 тест, 192 утверждения, 0 ошибок**
- Новые: `cache_test.clj` (5 тестов), `migration_test.clj` (4 теста)

### 🔧 Refactoring
- Удалён мёртвый код `defroutes api-routes` из всех 9 API namespace'ов
- Удалены неиспользуемые require и функции (logged-in?, get-current-user, get-spravochnik)
- Созданы общие хелперы: `json-response`, `pagination-meta`

### 🏗️ Infrastructure
- HikariCP connection pooling с настраиваемым размером через env vars
- DB миграции (custom runner + schema_migrations table)
- GitHub Actions: тесты + uberjar, Docker build + push, SSH deploy

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

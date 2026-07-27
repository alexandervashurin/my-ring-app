# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

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

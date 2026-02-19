# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased] - 2026-02-19

### 🔒 Security
- Добавлено HTML-экранирование всех пользовательских данных для защиты от XSS-атак
- Добавлены заголовки безопасности (X-Frame-Options, X-Content-Type-Options, X-XSS-Protection, Content-Security-Policy)
- Добавлена защита от CSRF-атак через middleware

### 🛡️ Error Handling
- Добавлен глобальный обработчик необработанных исключений (wrap-error-handler)
- Улучшена обработка ошибок при создании/обновлении/удалении работников
- Добавлено безопасное преобразование строк в числа (parse-int)
- Добавлена типизация для логирования ошибок

### ✅ Testing
- Добавлены comprehensive тесты для валидации работников (13 тестов)
- Добавлены тесты для валидации учёта рабочего времени (5 тестов)
- Добавлены тесты для функций модели (6 тестов)
- Добавлены интеграционные тесты для core функций
- Всего: 18 тестов, 50 утверждений

### 🔧 Refactoring
- Улучшена структура middleware в core.clj
- Выделены отдельные функции для обработки ошибок
- Убраны reflection warnings добавлением type hints
- Улучшена обработка ID (валидация и очистка)

### 📝 Documentation
- Полностью переписан README.md с добавлением:
  - Детальной структуры проекта
  - Примеров использования API endpoints
  - Инструкций по развёртыванию
  - Документации по логированию
  - Таблиц технологического стека
- Обновлён CHANGELOG.md

### 🐛 Bug Fixes
- Исправлена проблема компиляции AOT для middleware
- Исправлены проблемы с обработкой исключений в контроллерах
- Улучшена обработка пустых значений в формах

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

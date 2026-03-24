-- ============================================================================
-- Миграция схемы на PostgreSQL
-- Файл: postgres_migration.sql
-- ============================================================================
-- Использование:
--   psql -U postgres -d my_ring_app -f postgres_migration.sql
-- ============================================================================

-- Включаем транзакцию
BEGIN;

-- ============================================================================
-- 1. Справочные таблицы
-- ============================================================================

-- Категория работника
CREATE TABLE IF NOT EXISTS Категория_работника (
    id SERIAL PRIMARY KEY,
    название_категории TEXT NOT NULL UNIQUE
);

-- Цех
CREATE TABLE IF NOT EXISTS Цех (
    id SERIAL PRIMARY KEY,
    название_цеха TEXT NOT NULL UNIQUE
);

-- Система оплаты
CREATE TABLE IF NOT EXISTS Система_оплаты (
    id SERIAL PRIMARY KEY,
    название_системы TEXT NOT NULL UNIQUE
);

-- Разряд
CREATE TABLE IF NOT EXISTS Разряд (
    id SERIAL PRIMARY KEY,
    номер_разряда INTEGER NOT NULL UNIQUE
);

-- Режим работы
CREATE TABLE IF NOT EXISTS Режим_работы (
    id SERIAL PRIMARY KEY,
    название_режима TEXT NOT NULL UNIQUE
);

-- Оклад
CREATE TABLE IF NOT EXISTS Оклад (
    id SERIAL PRIMARY KEY,
    оклад_в_месяц DECIMAL(10,2) NOT NULL
);

-- Почасовые ставки
CREATE TABLE IF NOT EXISTS Почасовые_ставки (
    id SERIAL PRIMARY KEY,
    ставка_в_час DECIMAL(10,2) NOT NULL
);

-- ============================================================================
-- 2. Основные таблицы
-- ============================================================================

-- Работник
CREATE TABLE IF NOT EXISTS Работник (
    id SERIAL PRIMARY KEY,
    фамилия TEXT NOT NULL,
    имя TEXT NOT NULL,
    отчество TEXT,
    дата_приема DATE NOT NULL,
    цех_id INTEGER NOT NULL REFERENCES Цех(id),
    система_оплаты_id INTEGER NOT NULL REFERENCES Система_оплаты(id),
    категория_работника_id INTEGER NOT NULL REFERENCES Категория_работника(id),
    разряд_id INTEGER NOT NULL REFERENCES Разряд(id),
    режим_работы_id INTEGER NOT NULL REFERENCES Режим_работы(id),
    оклад_id INTEGER REFERENCES Оклад(id),
    почасовая_ставка_id INTEGER REFERENCES Почасовые_ставки(id)
);

-- Индексы для Работник
CREATE INDEX IF NOT EXISTS idx_работник_фамилия ON Работник(фамилия);
CREATE INDEX IF NOT EXISTS idx_работник_имя ON Работник(имя);
CREATE INDEX IF NOT EXISTS idx_работник_цех ON Работник(цех_id);

-- Учет рабочего времени
CREATE TABLE IF NOT EXISTS Учет_рабочего_времени (
    id SERIAL PRIMARY KEY,
    работник_id INTEGER NOT NULL REFERENCES Работник(id),
    год INTEGER NOT NULL,
    месяц INTEGER NOT NULL,
    всего_часов_за_месяц_по_плану INTEGER NOT NULL,
    всего_часов_в_месяц_по_факту INTEGER NOT NULL,
    количество_отработанных_дней INTEGER,
    количество_рабочих_часов_в_день INTEGER,
    всего_отработанных_часов INTEGER,
    сколько_должны_отработать INTEGER,
    больничные_дни INTEGER DEFAULT 0,
    командировочные_дни INTEGER DEFAULT 0
);

-- Индексы для Учет_рабочего_времени
CREATE INDEX IF NOT EXISTS idx_учет_работник ON Учет_рабочего_времени(работник_id);
CREATE INDEX IF NOT EXISTS idx_учет_год_месяц ON Учет_рабочего_времени(год, месяц);

-- Начисление заработной платы
CREATE TABLE IF NOT EXISTS Начисление_заработной_платы (
    id SERIAL PRIMARY KEY,
    учет_рабочего_времени_id INTEGER NOT NULL REFERENCES Учет_рабочего_времени(id),
    год INTEGER NOT NULL,
    месяц INTEGER NOT NULL,
    зарплата_за_больничные_дни DECIMAL(10,2) DEFAULT 0,
    зарплата_за_командировочные_дни DECIMAL(10,2) DEFAULT 0,
    общая_зарплата DECIMAL(10,2) NOT NULL
);

-- Индексы для Начисление_заработной_платы
CREATE INDEX IF NOT EXISTS idx_начисление_учет ON Начисление_заработной_платы(учет_рабочего_времени_id);
CREATE INDEX IF NOT EXISTS idx_начисление_год_месяц ON Начисление_заработной_платы(год, месяц);

-- ============================================================================
-- 3. Таблица пользователей (аутентификация)
-- ============================================================================

CREATE TABLE IF NOT EXISTS Пользователь (
    id SERIAL PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'manager',
    is_active BOOLEAN DEFAULT TRUE,
    last_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Индексы для Пользователь
CREATE INDEX IF NOT EXISTS idx_пользователь_username ON Пользователь(username);
CREATE INDEX IF NOT EXISTS idx_пользователь_email ON Пользователь(email);

-- ============================================================================
-- 4. Начальные данные (справочники)
-- ============================================================================

-- Категория работника
INSERT INTO Категория_работника (название_категории) VALUES
    ('Рабочий'), ('Специалист'), ('Руководитель'), ('Служащий'),
    ('Стажёр'), ('Ученик'), ('Временный работник'), ('Подрядчик')
ON CONFLICT (название_категории) DO NOTHING;

-- Цех
INSERT INTO Цех (название_цеха) VALUES
    ('Литейный цех'), ('Механический цех'), ('Сборочный цех'),
    ('Инструментальный цех'), ('Ремонтный цех'),
    ('Цех механообработки корпусных деталей'), ('Цех изготовления валов и шестерён'),
    ('Цех сборки гидротрансформаторов'), ('Цех сборки планетарных рядов'),
    ('Цех финальной сборки АКПП'), ('Цех тестирования и контроля качества'),
    ('Ремонтно-инструментальный цех'), ('Складской комплекс готовой продукции'),
    ('Цех гальванических покрытий'), ('Термический цех'),
    ('Подрядная организация "АвтоКомплект"'), ('Подрядная организация "ТехноСервис"'),
    ('Подрядная организация "ПромМонтаж"'), ('Аутсорсинг "Кадры-Профи"'),
    ('Стажёры и практиканты')
ON CONFLICT (название_цеха) DO NOTHING;

-- Система оплаты
INSERT INTO Система_оплаты (название_системы) VALUES
    ('Оклад'), ('Почасовая'), ('Сдельная'), ('Смешанная'),
    ('Почасовая с премией'), ('Проектная оплата')
ON CONFLICT (название_системы) DO NOTHING;

-- Разряды
INSERT INTO Разряд (номер_разряда) VALUES
    (1), (2), (3), (4), (5), (6), (7), (8)
ON CONFLICT (номер_разряда) DO NOTHING;

-- Режимы работы
INSERT INTO Режим_работы (название_режима) VALUES
    ('Односменный'), ('Двухсменный'), ('Трехсменный'),
    ('Гибкий график'), ('Вахтовый метод'), ('Частичная занятость'),
    ('Удалённая работа'), ('Вахта 2/2'), ('Стажировка')
ON CONFLICT (название_режима) DO NOTHING;

-- ============================================================================
-- 5. Пользователь по умолчанию (admin / admin123)
-- ============================================================================

INSERT INTO Пользователь (username, email, password_hash, role, is_active)
VALUES (
    'admin',
    'admin@example.com',
    '$2a$10$XQKsK8ZxZxZxZxZxZxZxZxZxZxZxZxZxZxZxZxZxZxZxZxZxZxZxZ',  -- bcrypt hash для 'admin123'
    'admin',
    TRUE
)
ON CONFLICT (username) DO NOTHING;

-- ============================================================================
-- 6. Представления (Views) для отчётности
-- ============================================================================

-- Представление: Работники с деталями
CREATE OR REPLACE VIEW Работники_с_деталями AS
SELECT
    r.id,
    r.фамилия,
    r.имя,
    r.отчество,
    r.дата_приема,
    ц.название_цеха AS цех,
    с.название_системы AS система_оплаты,
    к.название_категории AS категория,
    рз.номер_разряда AS разряд,
    рм.название_режима AS режим,
    о.оклад_в_месяц,
    п.ставка_в_час
FROM Работник r
LEFT JOIN Цех ц ON r.цех_id = ц.id
LEFT JOIN Система_оплаты с ON r.система_оплаты_id = с.id
LEFT JOIN Категория_работника к ON r.категория_работника_id = к.id
LEFT JOIN Разряд рз ON r.разряд_id = рз.id
LEFT JOIN Режим_работы рм ON r.режим_работы_id = рм.id
LEFT JOIN Оклад о ON r.оклад_id = о.id
LEFT JOIN Почасовые_ставки п ON r.почасовая_ставка_id = п.id;

-- ============================================================================
-- 7. Функции и триггеры (опционально)
-- ============================================================================

-- Функция для обновления updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Триггер для таблицы Пользователь
CREATE TRIGGER update_пользователь_updated_at
    BEFORE UPDATE ON Пользователь
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- Завершение транзакции
-- ============================================================================

COMMIT;

-- ============================================================================
-- Проверка после миграции
-- ============================================================================

-- Показать количество таблиц
SELECT COUNT(*) AS total_tables
FROM information_schema.tables
WHERE table_schema = 'public' AND table_type = 'BASE TABLE';

-- Показать количество записей в справочниках
SELECT 'Категория_работника' AS table_name, COUNT(*) AS row_count FROM Категория_работника
UNION ALL
SELECT 'Цех', COUNT(*) FROM Цех
UNION ALL
SELECT 'Система_оплаты', COUNT(*) FROM Система_оплаты
UNION ALL
SELECT 'Разряд', COUNT(*) FROM Разряд
UNION ALL
SELECT 'Режим_работы', COUNT(*) FROM Режим_работы
UNION ALL
SELECT 'Пользователь', COUNT(*) FROM Пользователь;

-- Миграция 003: Мульти-тенантность — таблица Организация + organization_id

-- +migrate Up

-- Таблица Организация
CREATE TABLE IF NOT EXISTS Организация (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    inn TEXT,
    phone TEXT,
    email TEXT,
    address TEXT,
    is_active BOOLEAN DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_org_name ON Организация(name);
CREATE INDEX IF NOT EXISTS idx_org_inn ON Организация(inn);

-- Добавляем organization_id к Пользователь
ALTER TABLE Пользователь ADD COLUMN organization_id INTEGER REFERENCES Организация(id);

-- Добавляем organization_id к Работник
ALTER TABLE Работник ADD COLUMN organization_id INTEGER REFERENCES Организация(id);

-- Добавляем organization_id к Учет_рабочего_времени (через Работник)
ALTER TABLE Учет_рабочего_времени ADD COLUMN organization_id INTEGER REFERENCES Организация(id);

-- Добавляем organization_id к Начисление_заработной_платы (через Учет)
ALTER TABLE Начисление_заработной_платы ADD COLUMN organization_id INTEGER REFERENCES Организация(id);

-- Добавляем organization_id к Аудит_изменений
ALTER TABLE Аудит_изменений ADD COLUMN organization_id INTEGER REFERENCES Организация(id);

-- Создаём организацию по умолчанию и привязываем существующие данные
INSERT OR IGNORE INTO Организация (id, name, is_active) VALUES (1, 'Основная организация', 1);

-- Привязываем существующих пользователей к организации 1
UPDATE Пользователь SET organization_id = 1 WHERE organization_id IS NULL;

-- Привязываем существующих работников к организации 1
UPDATE Работник SET organization_id = 1 WHERE organization_id IS NULL;

-- Привязываем учет времени к организации 1 (через работников)
UPDATE Учет_рабочего_времени
SET organization_id = (SELECT organization_id FROM Работник WHERE id = работник_id)
WHERE organization_id IS NULL;

-- Привязываем начисления к организации 1 (через учет времени)
UPDATE Начисление_заработной_платы
SET organization_id = (SELECT organization_id FROM Учет_рабочего_времени WHERE id = учет_рабочего_времени_id)
WHERE organization_id IS NULL;

-- Привязываем аудит к организации 1
UPDATE Аудит_изменений SET organization_id = 1 WHERE organization_id IS NULL;

-- Индексы для мульти-тенантности
CREATE INDEX IF NOT EXISTS idx_user_org ON Пользователь(organization_id);
CREATE INDEX IF NOT EXISTS idx_worker_org ON Работник(organization_id);
CREATE INDEX IF NOT EXISTS idx_worktime_org ON Учет_рабочего_времени(organization_id);
CREATE INDEX IF NOT EXISTS idx_salary_org ON Начисление_заработной_платы(organization_id);
CREATE INDEX IF NOT EXISTS idx_audit_org ON Аудит_изменений(organization_id);

-- Составные индексы для частых запросов
CREATE INDEX IF NOT EXISTS idx_worker_org_fio ON Работник(organization_id, фамилия, имя);
CREATE INDEX IF NOT EXISTS idx_worktime_worker_org ON Учет_рабочего_времени(работник_id, organization_id);
CREATE INDEX IF NOT EXISTS idx_salary_worktime_org ON Начисление_заработной_платы(учет_рабочего_времени_id, organization_id);

-- +migrate Down

-- Удаляем индексы
DROP INDEX IF EXISTS idx_salary_worktime_org;
DROP INDEX IF EXISTS idx_worktime_worker_org;
DROP INDEX IF EXISTS idx_worker_org_fio;
DROP INDEX IF EXISTS idx_audit_org;
DROP INDEX IF EXISTS idx_salary_org;
DROP INDEX IF EXISTS idx_worktime_org;
DROP INDEX IF EXISTS idx_worker_org;
DROP INDEX IF EXISTS idx_user_org;
DROP INDEX IF EXISTS idx_org_inn;
DROP INDEX IF EXISTS idx_org_name;

-- Удаляем organization_id из таблиц (SQLite не поддерживает DROP COLUMN до 3.35)
-- Полная пересоздация таблиц потребуется для отката

-- Удаляем организацию по умолчанию
DELETE FROM Организация WHERE id = 1;
DROP TABLE IF EXISTS Организация;

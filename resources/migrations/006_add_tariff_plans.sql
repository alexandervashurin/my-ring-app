-- Миграция 006: Тарифные планы
-- Добавляет таблицу тарифных планов и привязку плана к организации

-- +migrate Up

-- Таблица Тарифный_план
CREATE TABLE IF NOT EXISTS "Тарифный_план" (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    max_workers INTEGER NOT NULL DEFAULT 10,
    max_orgs INTEGER NOT NULL DEFAULT 1,
    features TEXT NOT NULL DEFAULT '{}',
    price_monthly INTEGER NOT NULL DEFAULT 0,
    price_yearly INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN DEFAULT 1,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Индексы
CREATE UNIQUE INDEX IF NOT EXISTS idx_plan_code ON "Тарифный_план"(code);
CREATE INDEX IF NOT EXISTS idx_plan_active ON "Тарифный_план"(is_active);

-- Seed тарифных планов
INSERT OR IGNORE INTO "Тарифный_план" (id, code, name, max_workers, max_orgs, features, price_monthly, price_yearly, is_active, sort_order) VALUES
    (1, 'free', 'Бесплатный', 10, 1, '{"export":false,"reports":false,"api":false,"audit":false,"1c":false,"email":false,"analytics":false,"support":"community"}', 0, 0, TRUE, 0),
    (2, 'pro', 'Pro', 50, 3, '{"export":true,"reports":true,"api":false,"audit":true,"1c":false,"email":true,"analytics":true,"support":"email"}', 2990, 29900, TRUE, 1),
    (3, 'enterprise', 'Enterprise', 999999, 999999, '{"export":true,"reports":true,"api":true,"audit":true,"1c":true,"email":true,"analytics":true,"support":"dedicated"}', 9990, 99900, TRUE, 2);

-- Добавляем plan_id в Организация
ALTER TABLE "Организация" ADD COLUMN plan_id INTEGER REFERENCES "Тарифный_план"(id) DEFAULT 1;

-- Обновляем существующие организации: plan_id = 1 (Free)
UPDATE "Организация" SET plan_id = 1 WHERE plan_id IS NULL;

-- Индекс для поиска по плану
CREATE INDEX IF NOT EXISTS idx_org_plan ON "Организация"(plan_id);

-- +migrate Down

DROP INDEX IF EXISTS idx_org_plan;
DROP INDEX IF EXISTS idx_plan_active;
DROP INDEX IF EXISTS idx_plan_code;

-- SQLite — очищаем поле вместо удаления
UPDATE "Организация" SET plan_id = NULL;

DROP TABLE IF EXISTS "Тарифный_план";

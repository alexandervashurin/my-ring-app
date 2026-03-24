-- ============================================================================
-- Миграция: Таблица аудита изменений
-- Файл: migration_002_audit_log.sql
-- ============================================================================

-- +migrate Up
CREATE TABLE IF NOT EXISTS Аудит_изменений (
    id SERIAL PRIMARY KEY,
    entity_type TEXT NOT NULL,           -- Тип сущности (Работник, Цех, etc.)
    entity_id INTEGER NOT NULL,          -- ID сущности
    action TEXT NOT NULL,                -- Действие (CREATE, UPDATE, DELETE)
    user_id INTEGER,                     -- ID пользователя (если есть)
    username TEXT,                       -- Имя пользователя
    old_values JSONB,                    -- Старые значения (для UPDATE/DELETE)
    new_values JSONB,                    -- Новые значения (для CREATE/UPDATE)
    ip_address TEXT,                     -- IP адрес клиента
    user_agent TEXT,                     -- User agent браузера
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- Время изменения
    details TEXT                         -- Дополнительные детали
);

-- Индексы для производительности
CREATE INDEX IF NOT EXISTS idx_аудит_entity ON Аудит_изменений(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_аудит_action ON Аудит_изменений(action);
CREATE INDEX IF NOT EXISTS idx_аудит_user ON Аудит_изменений(username);
CREATE INDEX IF NOT EXISTS idx_аудит_created_at ON Аудит_изменений(created_at DESC);

-- +migrate Down
-- DROP INDEX IF EXISTS idx_аудит_created_at;
-- DROP INDEX IF EXISTS idx_аудит_user;
-- DROP INDEX IF EXISTS idx_аудит_action;
-- DROP INDEX IF EXISTS idx_аудит_entity;
-- DROP TABLE IF EXISTS Аудит_изменений;

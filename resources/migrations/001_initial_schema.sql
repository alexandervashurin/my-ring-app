-- Миграция 001: Таблицы аутентификации и аудита

-- +migrate Up
-- Таблица Пользователь (аутентификация)
CREATE TABLE IF NOT EXISTS Пользователь (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'manager',
    is_active BOOLEAN DEFAULT 1,
    last_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_username ON Пользователь(username);
CREATE INDEX IF NOT EXISTS idx_email ON Пользователь(email);

-- Таблица Аудит_изменений
CREATE TABLE IF NOT EXISTS Аудит_изменений (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    entity_type TEXT NOT NULL,
    entity_id INTEGER NOT NULL,
    action TEXT NOT NULL,
    user_id INTEGER,
    username TEXT,
    old_values TEXT,
    new_values TEXT,
    ip_address TEXT,
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    details TEXT
);

CREATE INDEX IF NOT EXISTS idx_аудит_entity ON Аудит_изменений(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_аудит_action ON Аудит_изменений(action);
CREATE INDEX IF NOT EXISTS idx_аудит_user ON Аудит_изменений(username);
CREATE INDEX IF NOT EXISTS idx_аудит_created_at ON Аудит_изменений(created_at DESC);

-- +migrate Down
DROP INDEX IF EXISTS idx_аудит_created_at;
DROP INDEX IF EXISTS idx_аудит_user;
DROP INDEX IF EXISTS idx_аудит_action;
DROP INDEX IF EXISTS idx_аудит_entity;
DROP TABLE IF EXISTS Аудит_изменений;
DROP INDEX IF EXISTS idx_email;
DROP INDEX IF EXISTS idx_username;
DROP TABLE IF EXISTS Пользователь;

-- Миграция 001: Таблица пользователей
-- Файл: migration_001_create_users_table.sql

-- +migrate Up
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

-- Индексы для производительности
CREATE INDEX IF NOT EXISTS idx_username ON Пользователь(username);
CREATE INDEX IF NOT EXISTS idx_email ON Пользователь(email);

-- +migrate Down
-- DROP INDEX IF EXISTS idx_email;
-- DROP INDEX IF EXISTS idx_username;
-- DROP TABLE IF EXISTS Пользователь;

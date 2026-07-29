-- Миграция 005: Роли организации — org_role для пользователей
-- Добавляет возможность назначать роли на уровне организации

-- +migrate Up

-- Проверяем, существует ли уже колонка org_role (для идемпотентности)
-- SQLite не поддерживает IF NOT EXISTS для ALTER TABLE, поэтому используем PRAGMA
ALTER TABLE Пользователь ADD COLUMN org_role TEXT DEFAULT NULL;

-- Обновляем существующих пользователей: org_role = глобальная роль (если не установлено)
UPDATE Пользователь SET org_role = role WHERE org_role IS NULL;

-- Индекс для поиска по org_role
CREATE INDEX IF NOT EXISTS idx_user_org_role ON Пользователь(org_role);

-- +migrate Down

DROP INDEX IF EXISTS idx_user_org_role;

-- SQLite < 3.35 не поддерживает DROP COLUMN, очищаем поле
UPDATE Пользователь SET org_role = NULL;

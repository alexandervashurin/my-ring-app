-- Миграция 007: Аудит сессий — таблица Сессия для логирования входов/выходов

-- +migrate Up

CREATE TABLE IF NOT EXISTS Сессия (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES Пользователь(id),
    username TEXT NOT NULL,
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    logout_time TIMESTAMP,
    ip_address TEXT,
    user_agent TEXT,
    success BOOLEAN DEFAULT 1,
    fail_reason TEXT,
    organization_id INTEGER REFERENCES Организация(id)
);

CREATE INDEX IF NOT EXISTS idx_session_user ON Сессия(user_id);
CREATE INDEX IF NOT EXISTS idx_session_username ON Сессия(username);
CREATE INDEX IF NOT EXISTS idx_session_login_time ON Сессия(login_time DESC);
CREATE INDEX IF NOT EXISTS idx_session_org ON Сессия(organization_id);
CREATE INDEX IF NOT EXISTS idx_session_success ON Сессия(success);

-- +migrate Down

DROP INDEX IF EXISTS idx_session_success;
DROP INDEX IF EXISTS idx_session_org;
DROP INDEX IF EXISTS idx_session_login_time;
DROP INDEX IF EXISTS idx_session_username;
DROP INDEX IF EXISTS idx_session_user;
DROP TABLE IF EXISTS Сессия;

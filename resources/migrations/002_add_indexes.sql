-- +migrate Up

-- Индексы для ускорения JOIN-запросов в списке работников
CREATE INDEX IF NOT EXISTS idx_worker_цех_id ON "Работник"(цех_id);
CREATE INDEX IF NOT EXISTS idx_worker_система_оплаты_id ON "Работник"(система_оплаты_id);
CREATE INDEX IF NOT EXISTS idx_worker_категория_работника_id ON "Работник"(категория_работника_id);
CREATE INDEX IF NOT EXISTS idx_worker_разряд_id ON "Работник"(разряд_id);
CREATE INDEX IF NOT EXISTS idx_worker_режим_работы_id ON "Работник"(режим_работы_id);

-- Индекс для поиска по ФИО (сортировка)
CREATE INDEX IF NOT EXISTS idx_worker_фамилия_имя ON "Работник"(фамилия, имя);

-- Индекс для запросов зарплаты (worker + year + month)
CREATE INDEX IF NOT EXISTS idx_worktime_worker_id ON "Учет_рабочего_времени"(работник_id);
CREATE INDEX IF NOT EXISTS idx_worktime_year_month ON "Учет_рабочего_времени"(год, месяц);
CREATE INDEX IF NOT EXISTS idx_worktime_worker_year_month ON "Учет_рабочего_времени"(работник_id, год, месяц);

-- Индекс для начислений по учету времени
CREATE INDEX IF NOT EXISTS idx_salary_worktime_id ON "Начисление_заработной_платы"(учет_рабочего_времени_id);

-- Индексы для аудита (фильтрация по entity + username)
CREATE INDEX IF NOT EXISTS idx_audit_entity ON "Аудит_изменений"(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_username ON "Аудит_изменений"(username);
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON "Аудит_изменений"(created_at DESC);

-- Индекс для аутентификации (поиск пользователя)
CREATE INDEX IF NOT EXISTS idx_user_username ON "Пользователь"(username);

-- +migrate Down

DROP INDEX IF EXISTS idx_user_username;
DROP INDEX IF EXISTS idx_audit_created_at;
DROP INDEX IF EXISTS idx_audit_username;
DROP INDEX IF EXISTS idx_audit_entity;
DROP INDEX IF EXISTS idx_salary_worktime_id;
DROP INDEX IF EXISTS idx_worktime_worker_year_month;
DROP INDEX IF EXISTS idx_worktime_year_month;
DROP INDEX IF EXISTS idx_worktime_worker_id;
DROP INDEX IF EXISTS idx_worker_фамилия_имя;
DROP INDEX IF EXISTS idx_worker_режим_работы_id;
DROP INDEX IF EXISTS idx_worker_разряд_id;
DROP INDEX IF EXISTS idx_worker_категория_работника_id;
DROP INDEX IF EXISTS idx_worker_система_оплаты_id;
DROP INDEX IF EXISTS idx_worker_цех_id;

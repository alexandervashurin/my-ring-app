-- Миграция 004: Тестовые организации для разработки
-- Добавляет несколько тестовых организаций для проверки мульти-тенантности

-- +migrate Up

INSERT OR IGNORE INTO Организация (id, name, inn, phone, email, address, is_active) VALUES
    (2, 'ООО "ТехноПром"', '7701234567', '+7 (495) 123-45-67', 'info@technoprom.ru', 'г. Москва, ул. Промышленная, д. 10', 1),
    (3, 'АО "Металлург"', '7802345678', '+7 (812) 234-56-78', 'office@metallurg.spb.ru', 'г. Санкт-Петербург, пр. Металлистов, д. 25', 1),
    (4, 'ПАО "АвтоВАЗ"', '1650034567', '+7 (8352) 034-56-7', 'hr@avtovaz.com', 'г. Тольятти, ул. Автозаводская, д. 1', 1),
    (5, 'ЗАО "СтройМаш"', '5260045678', '+7 (831) 045-67-89', 'kontakt@stroymash.nnov.ru', 'г. Нижний Новгород, ул. Индустриальная, д. 15', 1),
    (6, 'ООО "ЭнергоСеть" (деактивирована)', '6670056789', '+7 (343) 056-78-90', 'info@energoset.ru', 'г. Екатеринбург, ул. Энергетиков, д. 8', 0);

-- Привязываем тестовых работников к организациям для демонстрации
-- Организация 1 (Основная): работники 1, 2, 5, 6
-- Организация 2 (ТехноПром): работники 3, 7
-- Организация 3 (Металлург): работник 4, 8
UPDATE Работник SET organization_id = 1 WHERE id IN (1, 2, 5, 6);
UPDATE Работник SET organization_id = 2 WHERE id IN (3, 7);
UPDATE Работник SET organization_id = 3 WHERE id IN (4, 8);

-- Синхронизируем организацию учёта времени и начислений с работниками
UPDATE Учет_рабочего_времени
SET organization_id = (SELECT organization_id FROM Работник WHERE id = работник_id)
WHERE работник_id IN (SELECT id FROM Работник WHERE organization_id IN (2, 3));

UPDATE Начисление_заработной_платы
SET organization_id = (SELECT organization_id FROM Учет_рабочего_времени WHERE id = учет_рабочего_времени_id)
WHERE учет_рабочего_времени_id IN (SELECT id FROM Учет_рабочего_времени WHERE organization_id IN (2, 3));

-- +migrate Down

UPDATE Работник SET organization_id = 1 WHERE organization_id IN (2, 3);
UPDATE Учет_рабочего_времени SET organization_id = 1
WHERE работник_id IN (SELECT id FROM Работник WHERE organization_id IN (2, 3));
UPDATE Начисление_заработной_платы SET organization_id = 1
WHERE учет_рабочего_времени_id IN (SELECT id FROM Учет_рабочего_времени
                                    WHERE работник_id IN (SELECT id FROM Работник WHERE organization_id IN (2, 3)));
DELETE FROM Организация WHERE id > 1;

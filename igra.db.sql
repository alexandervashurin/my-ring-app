BEGIN TRANSACTION;
CREATE TABLE IF NOT EXISTS Категория_работника (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    название_категории TEXT NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS Начисление_заработной_платы (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    учет_рабочего_времени_id INTEGER NOT NULL,
    год INTEGER NOT NULL,
    месяц INTEGER NOT NULL,
    зарплата_за_больничные_дни DECIMAL(10,2) DEFAULT 0,
    зарплата_за_командировочные_дни DECIMAL(10,2) DEFAULT 0,
    общая_зарплата DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (учет_рабочего_времени_id) REFERENCES Учет_рабочего_времени(id)
);
CREATE TABLE IF NOT EXISTS Оклад (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    оклад_в_месяц DECIMAL(10,2) NOT NULL
);
CREATE TABLE IF NOT EXISTS Почасовые_ставки (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ставка_в_час DECIMAL(10,2) NOT NULL
);
CREATE TABLE IF NOT EXISTS Работник (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    фамилия TEXT NOT NULL,
    имя TEXT NOT NULL,
    отчество TEXT,
    дата_приема DATE NOT NULL,
    цех_id INTEGER NOT NULL,
    система_оплаты_id INTEGER NOT NULL,
    категория_работника_id INTEGER NOT NULL,
    разряд_id INTEGER NOT NULL,
    режим_работы_id INTEGER NOT NULL,
    оклад_id INTEGER,
    почасовая_ставка_id INTEGER,
    FOREIGN KEY (цех_id) REFERENCES Цех(id),
    FOREIGN KEY (система_оплаты_id) REFERENCES Система_оплаты(id),
    FOREIGN KEY (категория_работника_id) REFERENCES Категория_работника(id),
    FOREIGN KEY (разряд_id) REFERENCES Разряд(id),
    FOREIGN KEY (режим_работы_id) REFERENCES Режим_работы(id),
    FOREIGN KEY (оклад_id) REFERENCES Оклад(id),
    FOREIGN KEY (почасовая_ставка_id) REFERENCES Почасовые_ставки(id)
);
CREATE TABLE IF NOT EXISTS Разряд (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    номер_разряда INTEGER NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS Режим_работы (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    название_режима TEXT NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS Система_оплаты (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    название_системы TEXT NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS Учет_рабочего_времени (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    работник_id INTEGER NOT NULL,
    год INTEGER NOT NULL,
    месяц INTEGER NOT NULL,
    всего_часов_за_месяц_по_плану INTEGER NOT NULL,
    всего_часов_в_месяц_по_факту INTEGER NOT NULL,
    количество_отработанных_дней INTEGER,
    количество_рабочих_часов_в_день INTEGER,
    всего_отработанных_часов INTEGER,
    сколько_должны_отработать INTEGER,
    больничные_дни INTEGER DEFAULT 0,
    командировочные_дни INTEGER DEFAULT 0,
    FOREIGN KEY (работник_id) REFERENCES Работник(id)
);
CREATE TABLE IF NOT EXISTS Цех (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    название_цеха TEXT NOT NULL UNIQUE
);
INSERT INTO "Категория_работника" ("id","название_категории") VALUES (1,'Рабочий'),
 (2,'Специалист'),
 (3,'Руководитель'),
 (4,'Служащий');
INSERT INTO "Начисление_заработной_платы" ("id","учет_рабочего_времени_id","год","месяц","зарплата_за_больничные_дни","зарплата_за_командировочные_дни","общая_зарплата") VALUES (1,1,2025,10,0,0,120000),
 (2,2,2025,10,8500,0,85000),
 (3,3,2025,10,0,0,52000),
 (4,4,2025,10,10400,0,41600),
 (5,5,2025,10,0,0,49680),
 (6,6,2025,10,0,32701,65402),
 (7,7,2025,10,47120,0,47120),
 (8,8,2025,10,0,0,31200);
INSERT INTO "Оклад" ("id","оклад_в_месяц") VALUES (1,35000),
 (2,42000),
 (3,52000),
 (4,68000),
 (5,85000),
 (6,120000);
INSERT INTO "Почасовые_ставки" ("id","ставка_в_час") VALUES (1,220.5),
 (2,185.75),
 (3,310),
 (4,275.25),
 (5,195),
 (6,350.8);
INSERT INTO "Работник" ("id","фамилия","имя","отчество","дата_приема","цех_id","система_оплаты_id","категория_работника_id","разряд_id","режим_работы_id","оклад_id","почасовая_ставка_id") VALUES (1,'Петров','Иван','Сергеевич','2020-03-15',1,1,3,6,1,6,NULL),
 (2,'Сидорова','Ольга','Владимировна','2019-11-20',2,1,3,5,1,5,NULL),
 (3,'Козлов','Алексей','Петрович','2021-06-10',3,1,2,4,4,4,NULL),
 (4,'Никитина','Елена','Игоревна','2022-01-15',4,1,2,4,4,3,NULL),
 (5,'Васильев','Михаил','Андреевич','2023-02-28',1,2,1,3,2,NULL,3),
 (6,'Федорова','Анна','Дмитриевна','2022-08-12',2,2,1,2,2,NULL,2),
 (7,'Морозов','Денис','Витальевич','2021-12-01',3,2,1,4,3,NULL,4),
 (8,'Орлова','Светлана','Борисовна','2023-04-05',4,2,1,3,2,NULL,1);
INSERT INTO "Разряд" ("id","номер_разряда") VALUES (1,1),
 (2,2),
 (3,3),
 (4,4),
 (5,5),
 (6,6);
INSERT INTO "Режим_работы" ("id","название_режима") VALUES (1,'Односменный'),
 (2,'Двухсменный'),
 (3,'Трехсменный'),
 (4,'Гибкий график'),
 (5,'Вахтовый метод');
INSERT INTO "Система_оплаты" ("id","название_системы") VALUES (1,'Оклад'),
 (2,'Почасовая'),
 (3,'Сдельная'),
 (4,'Смешанная');
INSERT INTO "Учет_рабочего_времени" ("id","работник_id","год","месяц","всего_часов_за_месяц_по_плану","всего_часов_в_месяц_по_факту","количество_отработанных_дней","количество_рабочих_часов_в_день","всего_отработанных_часов","сколько_должны_отработать","больничные_дни","командировочные_дни") VALUES (1,1,2025,10,160,160,20,8,160,160,0,0),
 (2,2,2025,10,160,152,19,8,152,160,1,0),
 (3,3,2025,10,160,168,21,8,168,160,0,0),
 (4,4,2025,10,160,144,18,8,144,160,2,0),
 (5,5,2025,10,160,160,20,8,160,160,0,0),
 (6,6,2025,10,160,176,22,8,176,160,0,2),
 (7,7,2025,10,160,152,19,8,152,160,1,0),
 (8,8,2025,10,160,160,20,8,160,160,0,0);
INSERT INTO "Цех" ("id","название_цеха") VALUES (1,'Литейный цех'),
 (2,'Механический цех'),
 (3,'Сборочный цех'),
 (4,'Инструментальный цех'),
 (5,'Ремонтный цех');
COMMIT;

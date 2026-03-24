# 🐘 Развёртывание с PostgreSQL

**Версия:** 1.0  
**Дата:** 24 марта 2026 г.

---

## 📋 Содержание

1. [Требования](#требования)
2. [Быстрый старт с Docker](#быстрый-старт-с-docker)
3. [Ручная установка PostgreSQL](#ручная-установка-postgresql)
4. [Миграция данных из SQLite](#миграция-данных-из-sqlite)
5. [Конфигурация приложения](#конфигурация-приложения)
6. [Переменные окружения](#переменные-окружения)

---

## 📦 Требования

| Компонент | Минимальная версия | Рекомендуемая версия |
|-----------|-------------------|---------------------|
| **PostgreSQL** | 13 | 15+ |
| **Java JDK** | 17 | 21 |
| **Docker** | 20.x | 24.x |
| **Docker Compose** | 2.x | 2.20+ |

---

## 🚀 Быстрый старт с Docker

### 1. Клонирование репозитория

```bash
git clone https://github.com/alexandervashurin/my-ring-app.git
cd my-ring-app
```

### 2. Запуск с Docker Compose

```bash
# Запуск PostgreSQL и приложения
docker-compose up -d

# Проверка статуса
docker-compose ps

# Просмотр логов
docker-compose logs -f app
```

### 3. Доступ к приложению

- **Приложение:** http://localhost:3000
- **PostgreSQL:** localhost:5432
- **Пользователь по умолчанию:** admin / admin123

### 4. Остановка

```bash
docker-compose down

# С удалением данных (осторожно!)
docker-compose down -v
```

---

## 🔧 Ручная установка PostgreSQL

### 1. Установка PostgreSQL

#### Ubuntu/Debian

```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
```

#### macOS (Homebrew)

```bash
brew install postgresql@15
brew services start postgresql@15
```

#### Windows

Скачайте установщик с [postgresql.org](https://www.postgresql.org/download/windows/)

---

### 2. Создание базы данных

```bash
# Вход в PostgreSQL
sudo -u postgres psql

# Создание БД и пользователя
CREATE DATABASE my_ring_app;
CREATE USER myringapp WITH PASSWORD 'mypassword123';
GRANT ALL PRIVILEGES ON DATABASE my_ring_app TO myringapp;
\q
```

---

### 3. Применение миграции

```bash
# Применение схемы
psql -U postgres -d my_ring_app -f postgres_migration.sql

# Проверка таблиц
psql -U postgres -d my_ring_app -c "\dt"
```

---

### 4. Запуск приложения

```bash
# Установка переменных окружения
export DB_TYPE=postgresql
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=my_ring_app
export DB_USER=myringapp
export DB_PASSWORD=mypassword123

# Запуск
lein run

# Или сборка JAR
lein uberjar
java -jar target/uberjar/my-ring-app-1.0.0-SNAPSHOT-standalone.jar
```

---

## 📊 Миграция данных из SQLite

### Скрипт миграции

```bash
# Экспорт данных из SQLite
sqlite3 igra.db ".dump" > sqlite_backup.sql

# Конвертация и импорт в PostgreSQL
# (требуется ручная обработка из-за различий в синтаксисе)
```

### Python-скрипт для миграции

```python
#!/usr/bin/env python3
import sqlite3
import psycopg2

# Подключение к SQLite
sqlite_conn = sqlite3.connect('igra.db')
sqlite_cursor = sqlite_conn.cursor()

# Подключение к PostgreSQL
pg_conn = psycopg2.connect(
    host="localhost",
    database="my_ring_app",
    user="postgres",
    password="postgres"
)
pg_cursor = pg_conn.cursor()

# Миграция таблицы Работник
sqlite_cursor.execute("SELECT * FROM Работник")
for row in sqlite_cursor.fetchall():
    pg_cursor.execute("""
        INSERT INTO Работник 
        (фамилия, имя, отчество, дата_приема, цех_id, система_оплаты_id, 
         категория_работника_id, разряд_id, режим_работы_id, оклад_id, почасовая_ставка_id)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """, row[1:])  # Пропускаем ID

pg_conn.commit()
pg_cursor.close()
pg_conn.close()
sqlite_conn.close()
```

---

## ⚙️ Конфигурация приложения

### Переменные окружения

| Переменная | По умолчанию | Описание |
|------------|--------------|----------|
| `DB_TYPE` | `sqlite` | Тип БД: `sqlite` или `postgresql` |
| `DB_HOST` | `localhost` | Хост PostgreSQL |
| `DB_PORT` | `5432` | Порт PostgreSQL |
| `DB_NAME` | `my_ring_app` | Имя базы данных |
| `DB_USER` | `postgres` | Пользователь БД |
| `DB_PASSWORD` | `postgres` | Пароль БД |
| `DATABASE_URL` | — | Полный URL подключения (альтернатива) |
| `PORT` | `3000` | Порт веб-сервера |
| `ENV` | `development` | Окружение (development/production) |

### Примеры подключения

#### SQLite (по умолчанию)

```bash
# Никаких дополнительных настроек не требуется
lein run
```

#### PostgreSQL (разные способы)

```bash
# Способ 1: Через переменные
export DB_TYPE=postgresql
export DB_HOST=localhost
export DB_NAME=my_ring_app
export DB_USER=postgres
export DB_PASSWORD=secret
lein run

# Способ 2: Через DATABASE_URL
export DATABASE_URL="postgresql://postgres:secret@localhost:5432/my_ring_app"
lein run

# Способ 3: Для Docker Compose
# Переменные определены в docker-compose.yml
docker-compose up
```

---

## 🔒 Безопасность

### Рекомендации для продакшена

1. **Смените пароль по умолчанию:**
   ```sql
   ALTER USER postgres WITH PASSWORD 'strong_password_here';
   ```

2. **Настройте pg_hba.conf:**
   ```
   # Только локальные подключения
   host my_ring_app myringapp 127.0.0.1/32 md5
   ```

3. **Включите SSL:**
   ```bash
   # postgresql.conf
   ssl = on
   ssl_cert_file = 'server.crt'
   ssl_key_file = 'server.key'
   ```

4. **Регулярные бэкапы:**
   ```bash
   pg_dump -U postgres my_ring_app > backup_$(date +%Y%m%d).sql
   ```

---

## 📊 Мониторинг

### Проверка подключения

```bash
# PostgreSQL
psql -U postgres -d my_ring_app -c "SELECT version();"

# Приложение
curl http://localhost:3000/api/dashboard/stats
```

### Логи приложения

```bash
# Docker
docker-compose logs -f app

# Systemd
journalctl -u my-ring-app -f
```

### Логи PostgreSQL

```bash
# Docker
docker-compose logs -f postgres

# Systemd
journalctl -u postgresql -f
```

---

## ❓ Troubleshooting

### Ошибка: "Connection refused"

```bash
# Проверьте, запущен ли PostgreSQL
sudo systemctl status postgresql

# Проверьте порт
netstat -tlnp | grep 5432
```

### Ошибка: "Database does not exist"

```bash
# Создайте базу данных
createdb -U postgres my_ring_app

# Примените миграцию
psql -U postgres -d my_ring_app -f postgres_migration.sql
```

### Ошибка: "Password authentication failed"

```bash
# Сбросьте пароль
psql -U postgres
ALTER USER postgres WITH PASSWORD 'new_password';
\q
```

---

## 📚 Дополнительные ресурсы

- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Docker Compose Reference](https://docs.docker.com/compose/)
- [Clojure Java JDBC](https://github.com/clojure/java.jdbc)

---

*Документация актуальна для версии 1.0.0-SNAPSHOT*

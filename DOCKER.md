# 🐳 Docker развёртывание

**Версия:** 1.0  
**Дата:** 24 марта 2026 г.

---

## 📋 Содержание

1. [Быстрый старт](#быстрый-старт)
2. [Конфигурация](#конфигурация)
3. [Переменные окружения](#переменные-окружения)
4. [Развёртывание с Nginx](#развёртывание-с-nginx)
5. [Мониторинг](#мониторинг)
6. [Бэкапы](#бэкапы)

---

## 🚀 Быстрый старт

### 1. Сборка и запуск

```bash
# Сборка и запуск приложения
docker-compose up -d --build

# Проверка статуса
docker-compose ps

# Просмотр логов
docker-compose logs -f app
```

### 2. Доступ к приложению

- **Приложение:** http://localhost:3000
- **Health Check:** http://localhost:3000/api/health
- **Метрики:** http://localhost:3000/api/metrics

### 3. Остановка

```bash
# Остановка
docker-compose down

# Остановка с удалением томов (осторожно!)
docker-compose down -v
```

---

## ⚙️ Конфигурация

### Переменные окружения

Создайте файл `.env` в корне проекта:

```bash
# Порт приложения
APP_PORT=3000

# Окружение (development/production)
ENV=production

# Тип БД (sqlite/postgresql)
DB_TYPE=sqlite

# SMTP для email уведомлений
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=your-email@gmail.com
SMTP_PASS=your-app-password
SMTP_TLS=true
SMTP_FROM=noreply@example.com

# Порт Nginx (если используется)
NGINX_PORT=80
```

### Основные переменные

| Переменная | По умолчанию | Описание |
|------------|--------------|----------|
| `APP_PORT` | 3000 | Порт приложения |
| `ENV` | production | Окружение |
| `DB_TYPE` | sqlite | Тип БД (sqlite/postgresql) |
| `SMTP_HOST` | smtp.gmail.com | SMTP сервер |
| `SMTP_PORT` | 587 | SMTP порт |
| `SMTP_USER` | — | SMTP пользователь |
| `SMTP_PASS` | — | SMTP пароль |
| `NGINX_PORT` | 80 | Порт Nginx |

---

## 🌐 Развёртывание с Nginx

### 1. Запуск с профилем Nginx

```bash
docker-compose --profile with-nginx up -d
```

### 2. Проверка

```bash
# Проверка статуса
docker-compose ps

# Логи Nginx
docker-compose logs nginx
```

### 3. Настройка SSL (опционально)

Для HTTPS используйте [Certbot](https://certbot.eff.org/):

```bash
# Установка SSL сертификата
docker run --rm -it \
  -v /etc/letsencrypt:/etc/letsencrypt \
  -v /var/lib/letsencrypt:/var/lib/letsencrypt \
  certbot/certbot certonly \
  --standalone \
  -d your-domain.com
```

---

## 📊 Мониторинг

### Health Check

```bash
# Проверка работоспособности
curl http://localhost:3000/api/health

# Проверка готовности
curl http://localhost:3000/api/ready

# Проверка что приложение живо
curl http://localhost:3000/api/live
```

### Prometheus метрики

```bash
# Получение метрик
curl http://localhost:3000/api/metrics

# Пример вывода:
# HELP app_uptime_seconds Время работы приложения (секунды)
# TYPE app_uptime_seconds counter
# app_uptime_seconds 3600.5
# app_memory_percent 45
```

### Статистика приложения

```bash
# Расширенная статистика
curl http://localhost:3000/api/stats
```

### Логи

```bash
# Логи приложения
docker-compose logs -f app

# Логи Nginx (если используется)
docker-compose logs -f nginx

# Логи за последние 100 строк
docker-compose logs --tail=100 app
```

---

## 💾 Бэкапы

### Бэкап базы данных

```bash
# Копирование файла БД из контейнера
docker cp my-ring-app:/app/igra.db ./backup-$(date +%Y%m%d).db

# Или из тома
docker run --rm \
  -v my-ring-app_sqlite-data:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/sqlite-backup-$(date +%Y%m%d).tar.gz /data
```

### Бэкап логов

```bash
docker run --rm \
  -v my-ring-app_app-logs:/logs \
  -v $(pwd):/backup \
  alpine tar czf /backup/logs-$(date +%Y%m%d).tar.gz /logs
```

### Восстановление из бэкапа

```bash
# Восстановление БД
docker cp ./backup.db my-ring-app:/app/igra.db

# Перезапуск приложения
docker-compose restart app
```

---

## 🔧 Управление контейнерами

### Основные команды

```bash
# Перезапуск приложения
docker-compose restart app

# Остановка и запуск
docker-compose stop app
docker-compose start app

# Удаление контейнера
docker-compose rm -f app

# Пересборка образа
docker-compose build --no-cache app
```

### Масштабирование

```bash
# Запуск нескольких реплик (если нужно)
docker-compose up -d --scale app=3
```

---

## 🐛 Troubleshooting

### Приложение не запускается

```bash
# Проверка логов
docker-compose logs app

# Проверка health check
docker-compose exec app wget -q -O - http://localhost:3000/api/health
```

### Проблемы с базой данных

```bash
# Проверка файла БД
docker-compose exec app ls -la /app/igra.db

# Проверка прав доступа
docker-compose exec app ls -la /app/logs
```

### Проблемы с памятью

```bash
# Проверка использования памяти
docker stats my-ring-app

# Увеличение лимита памяти в docker-compose.yml
# deploy.resources.limits.memory: 2G
```

---

## 📚 Дополнительные ресурсы

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Reference](https://docs.docker.com/compose/)
- [Nginx Documentation](https://nginx.org/en/docs/)

---

*Документация актуальна для версии 1.9.0-SNAPSHOT*

# Dockerfile для my-ring-app
# Система управления персоналом на Clojure/Ring

# ==============================================================================
# Этап 1: Сборка приложения
# ==============================================================================
FROM clojure:temurin-17-tools-deps as build

WORKDIR /app

# Копирование конфигурации зависимостей
COPY project.clj .

# Загрузка зависимостей (кэшируется)
RUN lein deps

# Копирование исходного кода
COPY src ./src
COPY resources ./resources
COPY test ./test

# Сборка uberjar
RUN lein uberjar

# ==============================================================================
# Этап 2: Рантайм образ
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine

# Установка зависимостей рантайма
RUN apk add --no-cache sqlite bash

# Создание пользователя для безопасности
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Копирование uberjar из build этапа
COPY --from=build /app/target/uberjar/my-ring-app-*.jar app.jar

# Копирование базы данных (если есть)
COPY igra.db . 2>/dev/null || true

# Создание директорий для логов
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

# Переключение на не-root пользователя
USER appuser

# Порт приложения
EXPOSE 3000

# Переменные окружения по умолчанию
ENV PORT=3000
ENV ENV=production
ENV DB_TYPE=sqlite

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:3000/api/health || exit 1

# Запуск приложения
CMD ["java", "-jar", "app.jar"]

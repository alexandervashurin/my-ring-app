# Dockerfile для my-ring-app
# Система управления персоналом на Clojure/Ring

FROM eclipse-temurin:17-jdk-alpine

# Установка зависимостей
RUN apk add --no-cache sqlite

# Рабочая директория
WORKDIR /app

# Копирование проекта
COPY project.clj .
COPY src ./src
COPY resources ./resources
COPY test ./test

# Загрузка зависимостей и сборка
RUN apk add --no-cache leiningen && \
    lein deps && \
    lein uberjar

# Порт приложения
EXPOSE 3000

# Переменная окружения для типа БД
ENV DB_TYPE=sqlite
ENV PORT=3000

# Запуск приложения
CMD ["java", "-jar", "target/uberjar/my-ring-app-1.0.0-SNAPSHOT-standalone.jar"]

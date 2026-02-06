(ns my-ring-app.config)

;; Подключение к БД
(def db-spec
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname "igra.db"})

;; Конфигурация приложения
(def app-config
  {:port (Integer/parseInt (or (System/getenv "PORT") "3000"))
   :env (or (System/getenv "ENV") "development")})
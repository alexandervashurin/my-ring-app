(ns my-ring-app.config
  (:require [clojure.string :as str]))

;; ======================================================================
;; Конфигурация базы данных
;; ======================================================================

(defn- get-db-type
  "Определение типа БД из переменных окружения"
  []
  (let [db-type (str/lower-case (or (System/getenv "DB_TYPE") "sqlite"))]
    (if (#{"postgres" "postgresql" "pg"} db-type)
      :postgresql
      :sqlite)))

(defn- get-db-spec []
  "Конфигурация подключения к БД"
  (let [db-type (get-db-type)]
    (if (= db-type :postgresql)
      ;; PostgreSQL конфигурация
      {:classname "org.postgresql.Driver"
       :subprotocol "postgresql"
       :subname (str "//"
                     (or (System/getenv "DB_HOST") "localhost")
                     ":"
                     (or (System/getenv "DB_PORT") "5432")
                     "/"
                     (or (System/getenv "DB_NAME") "my_ring_app"))
       :user (or (System/getenv "DB_USER") "postgres")
       :password (or (System/getenv "DB_PASSWORD") "postgres")
       :connection-uri (or (System/getenv "DATABASE_URL"))}
      ;; SQLite конфигурация (по умолчанию)
      {:classname "org.sqlite.JDBC"
       :subprotocol "sqlite"
       :subname "igra.db"})))

;; Подключение к БД
(def db-spec (get-db-spec))

;; ======================================================================
;; Конфигурация приложения
;; ======================================================================

(def app-config
  {:port (Integer/parseInt (or (System/getenv "PORT") "3000"))
   :env (or (System/getenv "ENV") "development")
   :db-type (get-db-type)})
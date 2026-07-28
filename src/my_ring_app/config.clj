(ns my-ring-app.config
  (:require [clojure.string :as str]
            [hikari-cp.core :as hikari]
            [my-ring-app.logger :as logger]))

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

(defn- get-db-spec
  "Конфигурация подключения к БД (без пула — для совместимости)"
  []
  (let [db-type (get-db-type)]
    (if (= db-type :postgresql)
      (let [user (System/getenv "DB_USER")
            password (System/getenv "DB_PASSWORD")]
        (when (or (str/blank? user) (str/blank? password))
          (throw (IllegalStateException. "DB_USER и DB_PASSWORD обязательны для PostgreSQL")))
        {:classname "org.postgresql.Driver"
         :subprotocol "postgresql"
         :subname (str "//"
                       (or (System/getenv "DB_HOST") "localhost")
                       ":"
                       (or (System/getenv "DB_PORT") "5432")
                       "/"
                       (or (System/getenv "DB_NAME") "my_ring_app"))
         :user user
         :password password
         :connection-uri (or (System/getenv "DATABASE_URL"))})
      {:classname "org.sqlite.JDBC"
       :subprotocol "sqlite"
       :subname "igra.db"})))

(defn- make-pooled-db-spec
  "Создание DataSource с HikariCP connection pool"
  [db-type raw-spec]
  (try
    (let [datasource (if (= db-type :postgresql)
                       (hikari/make-datasource
                        {:adapter "postgresql"
                         :username (:user raw-spec)
                         :password (:password raw-spec)
                         :jdbc-url (or (:connection-uri raw-spec)
                                       (str "jdbc:postgresql:"
                                            (:subname raw-spec)))
                         :pool-name "hr-app-pool"
                         :maximum-pool-size (or (when-let [v (System/getenv "DB_POOL_MAX")]
                                                 (Integer/parseInt v)) 10)
                         :minimum-idle (or (when-let [v (System/getenv "DB_POOL_MIN")]
                                             (Integer/parseInt v)) 2)
                         :connection-timeout 30000
                         :idle-timeout 600000
                         :max-lifetime 1800000})
                       (hikari/make-datasource
                        {:jdbc-url (str "jdbc:sqlite:" (:subname raw-spec))
                         :pool-name "hr-app-pool"
                         :maximum-pool-size 1
                         :minimum-idle 1}))]
      (logger/log-info (format "HikariCP pool создан (adapter=%s, max-pool=%s)"
                               (name db-type)
                               (if (= db-type :postgresql)
                                 (or (System/getenv "DB_POOL_MAX") "10")
                                 "1")))
      {:datasource datasource})
    (catch Exception e
      (logger/log-error e "Ошибка создания HikariCP pool, используется прямое подключение")
      raw-spec)))

;; Подключение к БД (с пулом)
(def db-spec (let [db-type (get-db-type)
                   raw (get-db-spec)]
               (make-pooled-db-spec db-type raw)))

;; ======================================================================
;; Конфигурация приложения
;; ======================================================================

(def app-config
  {:port (Integer/parseInt (or (System/getenv "PORT") "3000"))
   :env (or (System/getenv "ENV") "development")
   :db-type (get-db-type)})
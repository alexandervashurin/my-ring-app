(ns my-ring-app.logger
  (:require [clojure.tools.logging :as log]
            [java.time :as time]))

;; Логгер для аудита действий
(def audit-logger (log/logger "my-ring-app.audit"))

(defn log-audit
  "Логирование действий пользователей (аудит)"
  [action entity-type entity-id & [details]]
  (let [timestamp (time/local-date-time (time/clock))
        log-message (format "[%s] ACTION: %s | ENTITY: %s | ID: %s | DETAILS: %s"
                           timestamp
                           action
                           entity-type
                           entity-id
                           (or details "-"))]
    (log/info audit-logger log-message)))

(defn log-request
  "Логирование входящих запросов"
  [request]
  (let [method (-> request :request-method name)
        uri (:uri request)
        ip (get-in request [:headers "x-forwarded-for"] (:remote-addr request "unknown"))]
    (log/info (format "REQUEST: %s %s | IP: %s" method uri ip))))

(defn log-response
  "Логирование исходящих ответов"
  [status uri]
  (log/info (format "RESPONSE: %s %s" status uri)))

(defn log-error
  "Логирование ошибок"
  [error message & [context]]
  (let [error-message (format "ERROR: %s | MESSAGE: %s | CONTEXT: %s"
                             (.getMessage error)
                             message
                             (or context "-"))]
    (log/error error-message)))

(defn log-info
  "Логирование информационных сообщений"
  [message]
  (log/info message))

(defn log-debug
  "Логирование отладочной информации"
  [message]
  (log/debug message))

(defn log-warn
  "Логирование предупреждений"
  [message]
  (log/warn message))

(defn log-sql
  "Логирование SQL-запросов"
  [query params]
  (log/debug (format "SQL: %s | PARAMS: %s" query (pr-str params))))
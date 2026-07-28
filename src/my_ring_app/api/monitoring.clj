(ns my-ring-app.api.monitoring
  "REST API для мониторинга и метрик"
  (:require [ring.util.response :as resp]
            [my-ring-app.model :as model]
            [my-ring-app.migration :as migration]
            [my-ring-app.cache :as cache]
            [my-ring-app.logger :as logger]
            [java-time :as time]))

(declare get-app-stats)

;; Время запуска приложения
(def ^:private app-start-time (System/currentTimeMillis))

;; ======================================================================
;; Вспомогательные функции
;; ======================================================================

(defn- format-uptime
  "Форматирование uptime"
  [seconds]
  (let [days (int (/ seconds 86400))
        hours (int (mod (/ seconds 3600) 24))
        minutes (int (mod (/ seconds 60) 60))]
    (str days "d " hours "h " minutes "m")))

(defn- get-memory-info
  "Получение информации о памяти"
  []
  (let [runtime (Runtime/getRuntime)
        total-mem (.totalMemory runtime)
        free-mem (.freeMemory runtime)
        used-mem (- total-mem free-mem)
        max-mem (.maxMemory runtime)]
    {:used (int (/ used-mem 1048576))  ; MB
     :total (int (/ total-mem 1048576))
     :free (int (/ free-mem 1048576))
     :max (int (/ max-mem 1048576))
     :percent (int (* 100 (/ (double used-mem) (double total-mem))))}))

(defn- get-db-stats
  "Статистика базы данных"
  ([] (get-db-stats nil))
  ([org-id]
   (try
     (let [stats (model/get-dashboard-stats org-id)]
       {:tables (count (model/get-tables))
        :workers (:total-workers stats)
        :shops (:total-shops stats)
        :status "connected"})
     (catch Exception e
       {:status "disconnected"
        :error "Database connection failed"}))))

;; ======================================================================
;; Health Check
;; ======================================================================

(defn health-check
  "GET /api/health — проверка работоспособности"
  [request]
  (try
    (let [db-stats (get-db-stats)
          memory (get-memory-info)
          status (if (= (:status db-stats) "connected") "healthy" "degraded")]
      (logger/log-info (format "API: GET /api/health — статус: %s" status))
      (-> (resp/response
           {:status status
            :timestamp (str (time/instant))
            :database db-stats
            :memory {:used-mb (:used memory)
                     :total-mb (:total memory)
                     :percent (:percent memory)}
            :uptime (format-uptime (/ (- (System/currentTimeMillis) app-start-time) 1000))})
          (resp/content-type "application/json; charset=utf-8")))
    (catch Exception e
      (logger/log-error e "API: Ошибка health check")
      (-> (resp/response
           {:status "unhealthy"
            :timestamp (str (time/instant))
            :error "Internal server error"})
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn ready-check
  "GET /api/ready — проверка готовности к работе"
  [request]
  (try
    (let [db-stats (get-db-stats)]
      (if (= (:status db-stats) "connected")
        (do
          (logger/log-info "API: GET /api/ready — готов")
          (-> (resp/response {:ready true :timestamp (str (time/instant))})
              (resp/content-type "application/json; charset=utf-8")))
        (do
          (logger/log-warn "API: GET /api/ready — не готов (БД недоступна)")
          (-> (resp/response {:ready false :timestamp (str (time/instant)) :reason "Database unavailable"})
              (resp/status 503)
              (resp/content-type "application/json; charset=utf-8")))))
    (catch Exception e
      (logger/log-error e "API: Ошибка ready check")
      (-> (resp/response {:ready false :timestamp (str (time/instant)) :error "Internal server error"})
          (resp/status 503)
          (resp/content-type "application/json; charset=utf-8")))))

(defn live-check
  "GET /api/live — проверка что приложение живо"
  [request]
  (logger/log-debug "API: GET /api/live")
  (-> (resp/response {:alive true :timestamp (str (time/instant))})
      (resp/content-type "application/json; charset=utf-8")))

;; ======================================================================
;; Prometheus Metrics
;; ======================================================================

(defn- format-prometheus-metric
  "Форматирование метрики Prometheus"
  [name value help & [labels]]
  (str (when help (str "# HELP " name " " help "\n"))
       "# TYPE " name " gauge\n"
       name (when labels (str "{" labels "}")) " " value "\n"))

(defn prometheus-metrics
  "GET /api/metrics — метрики в формате Prometheus"
  [request]
  (try
    (let [memory (get-memory-info)
          uptime (/ (- (System/currentTimeMillis) app-start-time) 1000)
          db-stats (get-db-stats)
          app-stats (get-app-stats)
          metrics (str
                   "# HELP app_uptime_seconds Время работы приложения (секунды)\n"
                   "# TYPE app_uptime_seconds counter\n"
                   "app_uptime_seconds " uptime "\n"
                   
                   "# HELP app_memory_used_bytes Использовано памяти (байты)\n"
                   "# TYPE app_memory_used_bytes gauge\n"
                   "app_memory_used_bytes " (* (:used memory) 1048576) "\n"
                   
                   "# HELP app_memory_total_bytes Всего памяти (байты)\n"
                   "# TYPE app_memory_total_bytes gauge\n"
                   "app_memory_total_bytes " (* (:total memory) 1048576) "\n"
                   
                   "# HELP app_memory_percent Процент использования памяти\n"
                   "# TYPE app_memory_percent gauge\n"
                   "app_memory_percent " (:percent memory) "\n"
                   
                   "# HELP app_workers_count Количество работников\n"
                   "# TYPE app_workers_count gauge\n"
                   "app_workers_count " (:workers db-stats 0) "\n"
                   
                   "# HELP app_shops_count Количество цехов\n"
                   "# TYPE app_shops_count gauge\n"
                   "app_shops_count " (:shops db-stats 0) "\n"
                   
                   "# HELP app_tables_count Количество таблиц в БД\n"
                   "# TYPE app_tables_count gauge\n"
                   "app_tables_count " (:tables db-stats 0) "\n"
                   
                   "# HELP app_info Информация о приложении\n"
                   "# TYPE app_info gauge\n"
                   "app_info{version=\"" (:version app-stats) "\",clojure=\"" (:clojure-version app-stats) "\",env=\"" (:environment app-stats) "\"} 1\n")]
      (logger/log-debug "API: GET /api/metrics")
      (-> (resp/response metrics)
          (resp/header "Content-Type" "text/plain; version=0.0.4; charset=utf-8")))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении метрик Prometheus")
      (-> (resp/response "# ERROR: Metrics unavailable\n")
          (resp/status 500)
          (resp/header "Content-Type" "text/plain; charset=utf-8")))))

;; ======================================================================
;; Статистика приложения
;; ======================================================================

(defn- get-app-stats
  "Статистика приложения"
  []
  {:version "1.8.0-SNAPSHOT"
   :clojure-version (str (clojure-version))
   :uptime-seconds (- (System/currentTimeMillis) app-start-time)
   :environment (or (System/getenv "ENV") "development")})

(defn app-statistics
  "GET /api/stats — расширенная статистика приложения"
  [request]
  (try
    (let [memory (get-memory-info)
          db-stats (get-db-stats)
          app-stats (get-app-stats)
          [current-year current-month] (model/current-year-month)
          salary-data (->> (model/get-salary-with-details)
                           (filter #(and (= (:год %) current-year)
                                         (= (:месяц %) current-month))))
          total-payroll (reduce + (map #(or (:общая_зарплата %) 0) salary-data))
          worker-count (:workers db-stats)
          avg-salary (if (pos? worker-count) (/ total-payroll worker-count) 0)]
      (logger/log-info "API: GET /api/stats")
      (-> (resp/response
           {:application app-stats
            :database db-stats
            :memory {:used-mb (:used memory)
                     :total-mb (:total memory)
                     :free-mb (:free memory)
                     :max-mb (:max memory)
                     :percent (:percent memory)}
            :uptime {:seconds (int (/ (- (System/currentTimeMillis) app-start-time) 1000))
                     :formatted (format-uptime (/ (- (System/currentTimeMillis) app-start-time) 1000))}
                          :business {:total-workers (:workers db-stats 0)
                        :total-shops (:shops db-stats 0)
                        :total-payroll total-payroll
                        :avg-salary (int avg-salary)}
                          :cache (cache/cache-status)})
          (resp/content-type "application/json; charset=utf-8")))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении статистики")
      (-> (resp/response {:error "Internal server error"})
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

;; ======================================================================
;; Cache Refresh
;; ======================================================================

(defn refresh-cache
  "POST /api/cache/refresh — принудительное обновление кэша справочников (admin only)"
  [request]
  (try
    (cache/refresh!)
    (logger/log-info "API: POST /api/cache/refresh — кэш обновлён")
    (-> (resp/response {:message "Кэш справочников обновлён"
                        :status (cache/cache-status)})
        (resp/content-type "application/json; charset=utf-8"))
    (catch Exception e
      (logger/log-error e "API: Ошибка обновления кэша")
      (-> (resp/response {:error "Internal server error"})
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn migration-status
  "GET /api/migrations — статус миграций базы данных"
  [request]
  (try
    (let [status (migration/migration-status)
          applied (count (filter :applied status))
          pending (count (filter #(not (:applied %)) status))]
      (logger/log-info "API: GET /api/migrations")
      (-> (resp/response {:total (count status)
                          :applied applied
                          :pending pending
                          :migrations status})
          (resp/content-type "application/json; charset=utf-8")))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении статуса миграций")
      (-> (resp/response {:error "Internal server error"})
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))
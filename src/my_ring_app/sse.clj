(ns my-ring-app.sse
  "Dashboard polling — оптимизированный эндпоинт для периодического
   обновления данных дашборда. Возвращает только изменившиеся метрики."
  (:require [my-ring-app.logger :as logger]
            [my-ring-app.model :as model]
            [my-ring-app.cache :as cache]
            [ring.util.response :as resp]))

;; ======================================================================
;; Кэш последнего состояния дашборда для diff (по организациям)
;; ======================================================================

(defonce ^:private last-stats (atom {}))

(defn- compute-stats [org-id]
  (let [stats (model/get-dashboard-stats org-id)
        cache-status (cache/cache-status)
        now (System/currentTimeMillis)]
    {:total_workers (:total-workers stats 0)
     :total_shops (:total-shops stats 0)
     :avg_salary (int (or (:avg-salary stats) 0))
     :total_payroll (int (or (:total-payroll stats) 0))
     :cache_age_ms (:age-ms cache-status)
     :timestamp now}))

;; ======================================================================
;; Polling Handler
;; ======================================================================

(defn dashboard-poll
  "GET /api/dashboard/poll — быстрый эндпоинт для polling дашборда.
   Возвращает текущие метрики в рамках организации пользователя.
   Клиент опрашивает каждые 5-10 сек."
  [request]
  (try
    (let [org-id (:org-id request)
          current (compute-stats org-id)
          previous (get @last-stats org-id)
          changed? (or (nil? previous)
                       (not= (:total_workers current) (:total_workers previous))
                       (not= (:total_payroll current) (:total_payroll previous))
                       (not= (:avg_salary current) (:avg_salary previous)))]
      (swap! last-stats assoc org-id current)
      (logger/log-info (format "API: GET /api/dashboard/poll (org: %s)" (str org-id)))
      (-> (resp/response {:stats current
                          :changed changed?})
          (resp/content-type "application/json; charset=utf-8")))
    (catch Exception e
      (logger/log-error e "API: Ошибка polling дашборда")
      (-> (resp/response {:error "Internal server error"})
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

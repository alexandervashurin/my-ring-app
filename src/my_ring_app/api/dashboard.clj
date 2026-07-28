(ns my-ring-app.api.dashboard
  "REST API для дашборда и аналитики"
  (:require [ring.util.response :as resp]
            [my-ring-app.model :as model]
            [my-ring-app.logger :as logger]
            [my-ring-app.util :as util]))

;; ======================================================================
;; Вспомогательные функции
;; ======================================================================

(def ^:private success-response util/success-response)
(def ^:private error-response util/error-response)

;; ======================================================================
;; API endpoints
;; ======================================================================

(defn get-dashboard
  "GET /api/dashboard — получение данных дашборда"
  [request]
  (try
    (let [org-id (:org-id request)
          dashboard-data (model/get-dashboard-data org-id)]
      (logger/log-info (format "API: GET /api/dashboard (org: %s)" (str org-id)))
      (-> (resp/response (success-response dashboard-data "Данные дашборда получены"))
          (resp/content-type "application/json; charset=utf-8")))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении данных дашборда")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn get-dashboard-stats
  "GET /api/dashboard/stats — получение основной статистики"
  [request]
  (try
    (let [org-id (:org-id request)
          stats (model/get-dashboard-stats org-id)]
      (logger/log-info (format "API: GET /api/dashboard/stats (org: %s)" (str org-id)))
      (-> (resp/response (success-response stats "Статистика получена"))
          (resp/content-type "application/json; charset=utf-8")))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении статистики")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn get-workers-by-shop
  "GET /api/analytics/workers-by-shop — распределение по цехам"
  [request]
  (try
    (let [org-id (:org-id request)
          data (model/get-workers-by-shop org-id)]
      (logger/log-info (format "API: GET /api/analytics/workers-by-shop (org: %s)" (str org-id)))
      (-> (resp/response (success-response data "Данные получены"))
          (resp/content-type "application/json; charset=utf-8")))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении распределения по цехам")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn get-workers-by-category
  "GET /api/analytics/workers-by-category — распределение по категориям"
  [request]
  (try
    (let [org-id (:org-id request)
          data (model/get-workers-by-category org-id)]
      (logger/log-info (format "API: GET /api/analytics/workers-by-category (org: %s)" (str org-id)))
      (-> (resp/response (success-response data "Данные получены"))
          (resp/content-type "application/json; charset=utf-8")))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении распределения по категориям")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn get-salary-distribution
  "GET /api/analytics/salary-distribution — распределение по зарплате"
  [request]
  (try
    (let [org-id (:org-id request)
          data (model/get-salary-distribution org-id)]
      (logger/log-info (format "API: GET /api/analytics/salary-distribution (org: %s)" (str org-id)))
      (-> (resp/response (success-response data "Данные получены"))
          (resp/content-type "application/json; charset=utf-8")))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении распределения по зарплате")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

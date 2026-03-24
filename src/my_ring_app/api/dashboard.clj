(ns my-ring-app.api.dashboard
  "REST API для дашборда и аналитики"
  (:require [compojure.core :refer [defroutes GET]]
            [ring.util.response :as resp]
            [my-ring-app.model :as model]
            [my-ring-app.logger :as logger]))

;; ======================================================================
;; Вспомогательные функции
;; ======================================================================

(defn- success-response
  "Стандартный ответ об успехе"
  ([data]
   {:success true
    :data data
    :message "Данные получены"})
  ([data message]
   {:success true
    :data data
    :message message}))

(defn- error-response
  "Стандартный ответ об ошибке"
  [code message]
  {:success false
   :error {:code code
           :message message}})

;; ======================================================================
;; API endpoints
;; ======================================================================

(defn get-dashboard
  "GET /api/dashboard — получение данных дашборда"
  [request]
  (try
    (let [dashboard-data (model/get-dashboard-data)]
      (logger/log-info "API: GET /api/dashboard")
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
    (let [stats (model/get-dashboard-stats)]
      (logger/log-info "API: GET /api/dashboard/stats")
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
    (let [data (model/get-workers-by-shop)]
      (logger/log-info "API: GET /api/analytics/workers-by-shop")
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
    (let [data (model/get-workers-by-category)]
      (logger/log-info "API: GET /api/analytics/workers-by-category")
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
    (let [data (model/get-salary-distribution)]
      (logger/log-info "API: GET /api/analytics/salary-distribution")
      (-> (resp/response (success-response data "Данные получены"))
          (resp/content-type "application/json; charset=utf-8")))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении распределения по зарплате")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

;; ======================================================================
;; Маршруты API
;; ======================================================================

(defroutes api-routes
  (GET "/api/dashboard" [] get-dashboard)
  (GET "/api/dashboard/stats" [] get-dashboard-stats)
  (GET "/api/analytics/workers-by-shop" [] get-workers-by-shop)
  (GET "/api/analytics/workers-by-category" [] get-workers-by-category)
  (GET "/api/analytics/salary-distribution" [] get-salary-distribution))

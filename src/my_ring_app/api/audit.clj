(ns my-ring-app.api.audit
  "REST API для просмотра аудита изменений"
  (:require [compojure.core :refer [defroutes GET]]
            [ring.util.response :as resp]
            [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [my-ring-app.model :as model]
            [my-ring-app.config :refer [db-spec]]
            [my-ring-app.logger :as logger]))

;; ======================================================================
;; Вспомогательные функции
;; ======================================================================

(defn- parse-int [s default]
  "Безопасное преобразование строки в число"
  (try
    (if (or (nil? s) (str/blank? s))
      default
      (Integer/parseInt (str/trim s)))
    (catch NumberFormatException e
      default)))

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

(defn get-audit-log-api
  "GET /api/audit — получение записей аудита с пагинацией"
  [request]
  (try
    (let [query-params (:params request)
          page (parse-int (:page query-params) 1)
          per-page (parse-int (:per_page query-params) 50)
          entity-type (:entity_type query-params)
          action (:action query-params)
          offset (* (- page 1) per-page)
          records (model/get-audit-log per-page offset entity-type action)
          total (model/get-audit-log-count entity-type action)
          total-pages (int (Math/ceil (/ total (double per-page))))]
      (logger/log-info (format "API: GET /api/audit (страница: %d, найдено: %d из %d)"
                               page (count records) total))
      (-> (resp/response (success-response
                          {:records records
                           :pagination {:page page
                                        :per_page per-page
                                        :total total
                                        :total_pages total-pages
                                        :has_next (< page total-pages)
                                        :has_prev (> page 1)}}
                          (str "Получено " (count records) " записей аудита")))
          (resp/content-type "application/json; charset=utf-8")))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении записей аудита")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn get-audit-by-entity-api
  "GET /api/audit/:entity-type/:entity-id — история сущности"
  [request]
  (try
    (let [route-params (:route-params request)
          entity-type (:entity-type route-params)
          entity-id (parse-int (:entity-id route-params) 0)]
      (if (or (nil? entity-type) (zero? entity-id))
        (-> (resp/response (error-response "INVALID_PARAMS" "Некорректные параметры"))
            (resp/status 400)
            (resp/content-type "application/json; charset=utf-8"))
        (let [records (model/get-audit-by-entity entity-type entity-id)]
          (logger/log-info (format "API: GET /api/audit/%s/%d (найдено: %d)"
                                   entity-type entity-id (count records)))
          (-> (resp/response (success-response
                              records
                              (str "История " entity-type " ID=" entity-id)))
              (resp/content-type "application/json; charset=utf-8")))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении истории сущности")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn get-audit-by-user-api
  "GET /api/audit/user/:username — действия пользователя"
  [request]
  (try
    (let [route-params (:route-params request)
          username (:username route-params)
          limit (parse-int (:limit (:params request)) 100)]
      (if (or (nil? username) (str/blank? username))
        (-> (resp/response (error-response "INVALID_PARAMS" "Имя пользователя обязательно"))
            (resp/status 400)
            (resp/content-type "application/json; charset=utf-8"))
        (let [records (model/get-audit-by-user username limit)]
          (logger/log-info (format "API: GET /api/audit/user/%s (найдено: %d)"
                                   username (count records)))
          (-> (resp/response (success-response
                              records
                              (str "Действия пользователя " username)))
              (resp/content-type "application/json; charset=utf-8")))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении действий пользователя")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn get-audit-stats-api
  "GET /api/audit/stats — статистика аудита"
  [request]
  (try
    (let [total (model/get-audit-log-count)
          by-action (jdbc/query db-spec ["SELECT action, COUNT(*) as count
                                          FROM Аудит_изменений
                                          GROUP BY action"])
          by-entity (jdbc/query db-spec ["SELECT entity_type, COUNT(*) as count
                                          FROM Аудит_изменений
                                          GROUP BY entity_type
                                          ORDER BY count DESC"])
          recent (model/get-audit-log 10 0)]
      (logger/log-info "API: GET /api/audit/stats")
      (-> (resp/response (success-response
                          {:total total
                           :by-action by-action
                           :by-entity by-entity
                           :recent (take 10 recent)}
                          "Статистика аудита получена"))
          (resp/content-type "application/json; charset=utf-8")))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении статистики аудита")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

;; ======================================================================
;; Маршруты API
;; ======================================================================

(defroutes api-routes
  (GET "/api/audit" [] get-audit-log-api)
  (GET "/api/audit/stats" [] get-audit-stats-api)
  (GET "/api/audit/:entity-type/:entity-id" [] get-audit-by-entity-api)
  (GET "/api/audit/user/:username" [] get-audit-by-user-api))

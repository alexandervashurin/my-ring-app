(ns my-ring-app.api.salary
  "REST API для зарплаты и учёта рабочего времени"
  (:require [compojure.core :refer [defroutes GET POST PUT]]
            [ring.util.response :as resp]
            [clojure.string :as str]
            [my-ring-app.model :as model]
            [my-ring-app.validation :as validation]
            [my-ring-app.logger :as logger]
            [my-ring-app.util :as util]))

;; ======================================================================
;; Вспомогательные функции
;; ======================================================================

(def ^:private parse-int util/parse-int)
(def ^:private validate-id util/validate-id)
(def ^:private success-response util/success-response)
(def ^:private error-response util/error-response)

;; ======================================================================
;; API endpoints - Зарплата
;; ======================================================================

(defn get-worker-salary
  "GET /api/salary/:worker-id — зарплата работника"
  [request]
  (try
    (let [worker-id (-> request :route-params :worker-id validate-id)
          year (parse-int (get-in request [:params :year]) 2025)
          month (parse-int (get-in request [:params :month]) 10)
          validation-error (util/validate-year-month year month)]
      (cond
        (nil? worker-id)
        (-> (resp/response (error-response "INVALID_ID" "Некорректный идентификатор работника"))
            (resp/status 400)
            (resp/content-type "application/json; charset=utf-8"))

        validation-error
        (-> (resp/response (error-response (:error validation-error) (:message validation-error)))
            (resp/status 400)
            (resp/content-type "application/json; charset=utf-8"))

        :else
        (let [salary-info (model/get-worker-salary worker-id year month)
              salary-history (model/get-worker-salary-history worker-id)]
          (if salary-info
            (do
              (logger/log-info (format "API: GET /api/salary/%d — зарплата за %d-%02d" worker-id year month))
              (-> (resp/response (success-response
                                  {:current salary-info
                                   :history salary-history}
                                  (str "Зарплата работника за " month "/" year)))
                  (resp/content-type "application/json; charset=utf-8")))
            (-> (resp/response (error-response "NOT_FOUND" "Данные о зарплате не найдены"))
                (resp/status 404)
                (resp/content-type "application/json; charset=utf-8"))))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении зарплаты работника")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

;; ======================================================================
;; API endpoints - Учёт рабочего времени
;; ======================================================================

(defn get-worker-work-time
  "GET /api/work-time/:worker-id — учёт времени работника"
  [request]
  (try
    (let [worker-id (-> request :route-params :worker-id validate-id)]
      (if (nil? worker-id)
        (-> (resp/response (error-response "INVALID_ID" "Некорректный идентификатор работника"))
            (resp/status 400)
            (resp/content-type "application/json; charset=utf-8"))
        (let [work-time-records (model/get-worker-work-time worker-id)]
          (logger/log-info (format "API: GET /api/work-time/%d — найдено %d записей" worker-id (count work-time-records)))
          (-> (resp/response (success-response
                              work-time-records
                              (str "Получено " (count work-time-records) " записей учёта времени")))
              (resp/content-type "application/json; charset=utf-8")))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении учёта времени")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn update-work-time
  "PUT /api/work-time/:id — обновление учёта времени"
  [request]
  (try
    (let [work-time-id (-> request :route-params :id validate-id)
          data (:params request)]
      (if (nil? work-time-id)
        (-> (resp/response (error-response "INVALID_ID" "Некорректный идентификатор"))
            (resp/status 400)
            (resp/content-type "application/json; charset=utf-8"))
        (let [validation-result (validation/validate-work-time data)]
          (if (:valid? validation-result)
            (let [update-data {:год (Integer/parseInt (:год data))
                               :месяц (Integer/parseInt (:месяц data))
                               :всего_часов_за_месяц_по_плану (Integer/parseInt (:всего_часов_за_месяц_по_плану data))
                               :всего_часов_в_месяц_по_факту (Integer/parseInt (:всего_часов_в_месяц_по_факту data))
                               :количество_отработанных_дней (when (seq (:количество_отработанных_дней data)) (Integer/parseInt (:количество_отработанных_дней data)))
                               :количество_рабочих_часов_в_день (when (seq (:количество_рабочих_часов_в_день data)) (Integer/parseInt (:количество_рабочих_часов_в_день data)))
                               :всего_отработанных_часов (when (seq (:всего_отработанных_часов data)) (Integer/parseInt (:всего_отработанных_часов data)))
                               :сколько_должны_отработать (when (seq (:сколько_должны_отработать data)) (Integer/parseInt (:сколько_должны_отработать data)))
                               :больничные_дни (Integer/parseInt (or (:больничные_дни data) "0"))
                               :командировочные_дни (Integer/parseInt (or (:командировочные_дни data) "0"))}
                  result (model/update-record "Учет_рабочего_времени" work-time-id update-data)]
              (if (:success result)
                (do
                  (logger/log-audit "UPDATE" "WorkTime" work-time-id "Обновлен учёт времени (API)")
                  (logger/log-info (format "API: PUT /api/work-time/%d — обновлено" work-time-id))
                  (-> (resp/response (success-response nil "Учёт времени обновлён"))
                      (resp/content-type "application/json; charset=utf-8")))
                (-> (resp/response (error-response "UPDATE_ERROR" (:message result)))
                    (resp/status 500)
                    (resp/content-type "application/json; charset=utf-8"))))
            (-> (resp/response (error-response "VALIDATION_ERROR" "Ошибка валидации данных" (:errors validation-result)))
                (resp/status 400)
                (resp/content-type "application/json; charset=utf-8"))))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при обновлении учёта времени")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

;; ======================================================================
;; Маршруты API
;; ======================================================================

(defroutes api-routes
  (GET "/api/salary/:worker-id" [] get-worker-salary)
  (GET "/api/work-time/:worker-id" [] get-worker-work-time)
  (PUT "/api/work-time/:id" [] update-work-time))

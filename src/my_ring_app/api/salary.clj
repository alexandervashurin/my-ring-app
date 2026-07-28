(ns my-ring-app.api.salary
  "REST API для зарплаты и учёта рабочего времени"
  (:require [ring.util.response :as resp]
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
          [current-year current-month] (model/current-year-month)
          year (parse-int (get-in request [:params :year]) current-year)
          month (parse-int (get-in request [:params :month]) current-month)
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
        (let [org-id (:org-id request)
              salary-info (model/get-worker-salary worker-id year month org-id)
              salary-history (model/get-worker-salary-history worker-id org-id)]
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
        (let [org-id (:org-id request)
              work-time-records (model/get-worker-work-time worker-id org-id)]
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
            (let [update-data (util/parse-work-time-params data)
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

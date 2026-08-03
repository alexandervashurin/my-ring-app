(ns my-ring-app.api.salary
  "REST API для зарплаты и учёта рабочего времени"
  (:require [my-ring-app.model :as model]
            [my-ring-app.validation :as validation]
            [my-ring-app.logger :as logger]
            [my-ring-app.util :as util]))

(def ^:private parse-int util/parse-int)
(def ^:private validate-id util/validate-id)

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
        (util/json-error 400 "INVALID_ID" "Некорректный идентификатор работника")

        validation-error
        (util/json-error 400 (:error validation-error) (:message validation-error))

        :else
        (let [org-id (:org-id request)
              salary-info (model/get-worker-salary worker-id year month org-id)
              salary-history (model/get-worker-salary-history worker-id org-id)]
          (if salary-info
            (do
              (logger/log-info (format "API: GET /api/salary/%d — зарплата за %d-%02d" worker-id year month))
              (util/json-ok {:current salary-info
                             :history salary-history}
                            (str "Зарплата работника за " month "/" year)))
            (util/json-error 404 "NOT_FOUND" "Данные о зарплате не найдены")))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении зарплаты работника")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

(defn get-worker-work-time
  "GET /api/work-time/:worker-id — учёт времени работника"
  [request]
  (try
    (let [worker-id (-> request :route-params :worker-id validate-id)]
      (if (nil? worker-id)
        (util/json-error 400 "INVALID_ID" "Некорректный идентификатор работника")
        (let [org-id (:org-id request)
              work-time-records (model/get-worker-work-time worker-id org-id)]
          (logger/log-info (format "API: GET /api/work-time/%d — найдено %d записей" worker-id (count work-time-records)))
          (util/json-ok work-time-records
                        (str "Получено " (count work-time-records) " записей учёта времени")))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении учёта времени")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

(defn update-work-time
  "PUT /api/work-time/:id — обновление учёта времени (только своей организации)"
  [request]
  (try
    (let [work-time-id (-> request :route-params :id validate-id)
          org-id (:org-id request)
          data (:params request)]
      (if (nil? work-time-id)
        (util/json-error 400 "INVALID_ID" "Некорректный идентификатор")
        (if-not (model/get-work-time-by-id (str work-time-id) org-id)
          (util/json-error 404 "NOT_FOUND" "Запись учёта времени не найдена")
          (let [validation-result (validation/validate-work-time data)]
            (if (:valid? validation-result)
              (let [update-data (util/parse-work-time-params data)
                    result (model/update-record "Учет_рабочего_времени" work-time-id update-data)]
                (if (:success result)
                  (do
                    (logger/log-audit "UPDATE" "WorkTime" work-time-id "Обновлен учёт времени (API)")
                    (logger/log-info (format "API: PUT /api/work-time/%d — обновлено" work-time-id))
                    (util/json-ok nil "Учёт времени обновлён"))
                  (util/json-error 500 "UPDATE_ERROR" (:message result))))
              (util/json-error-details 400 "VALIDATION_ERROR" "Ошибка валидации данных" (:errors validation-result)))))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при обновлении учёта времени")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

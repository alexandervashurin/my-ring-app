(ns my-ring-app.api.reports
  "REST API для генерации PDF отчётов"
  (:require [my-ring-app.model :as model]
            [my-ring-app.pdf-reports :as pdf]
            [my-ring-app.logger :as logger]
            [my-ring-app.util :as util]))

(def ^:private validate-id util/validate-id)
(def ^:private parse-int util/parse-int)

(defn export-worker-pdf
  "GET /api/reports/worker/:id/pdf — экспорт отчёта по работнику в PDF"
  [request]
  (let [temp-file (java.io.File/createTempFile "worker_" ".pdf")]
    (try
      (let [worker-id (-> request :route-params :id)]
        (if-not (validate-id worker-id)
          (do (.delete temp-file)
              (util/json-error 400 "INVALID_ID" "Неверный формат ID работника"))
          (if-let [result (pdf/generate-worker-pdf worker-id (.getAbsolutePath temp-file))]
            (if (:success result)
              (do
                (logger/log-info (format "API: GET /api/reports/worker/%s/pdf — сгенерирован PDF" worker-id))
                (.deleteOnExit temp-file)
                (util/file-download temp-file "application/pdf" (str "worker_" worker-id ".pdf")))
              (do (.delete temp-file)
                  (util/json-error 404 "NOT_FOUND" (:message result))))
            (do (.delete temp-file)
                (util/json-error 500 "GENERATION_ERROR" "Ошибка при генерации PDF")))))
      (catch Exception e
        (.delete temp-file)
        (logger/log-error e "API: Ошибка при экспорте работника в PDF")
        (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера")))))

(defn export-workers-list-pdf
  "GET /api/reports/workers/pdf — экспорт списка работников в PDF"
  [request]
  (let [temp-file (java.io.File/createTempFile "workers_" ".pdf")]
    (try
      (if-let [result (pdf/generate-workers-list-pdf (.getAbsolutePath temp-file))]
        (if (:success result)
          (do
            (logger/log-info "API: GET /api/reports/workers/pdf — сгенерирован PDF списка работников")
            (.deleteOnExit temp-file)
            (util/file-download temp-file "application/pdf" "workers_list.pdf"))
          (do (.delete temp-file)
              (util/json-error 500 "GENERATION_ERROR" (:message result))))
        (do (.delete temp-file)
            (util/json-error 500 "GENERATION_ERROR" "Ошибка при генерации PDF")))
      (catch Exception e
        (.delete temp-file)
        (logger/log-error e "API: Ошибка при экспорте списка работников в PDF")
        (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера")))))

(defn export-salary-report-pdf
  "GET /api/reports/salary/pdf — экспорт отчёта по зарплате в PDF"
  [request]
  (let [query-params (:params request)
        [current-year current-month] (model/current-year-month)
        year (parse-int (:year query-params) current-year)
        month (parse-int (:month query-params) current-month)
        temp-file (java.io.File/createTempFile "salary_" ".pdf")]
    (try
      (if-let [result (pdf/generate-salary-report-pdf (.getAbsolutePath temp-file) year month)]
        (if (:success result)
          (do
            (logger/log-info (format "API: GET /api/reports/salary/pdf — сгенерирован PDF за %d-%d" year month))
            (.deleteOnExit temp-file)
            (util/file-download temp-file "application/pdf" (str "salary_report_" year "_" month ".pdf")))
          (do (.delete temp-file)
              (util/json-error 500 "GENERATION_ERROR" (:message result))))
        (do (.delete temp-file)
            (util/json-error 500 "GENERATION_ERROR" "Ошибка при генерации PDF")))
      (catch Exception e
        (.delete temp-file)
        (logger/log-error e "API: Ошибка при экспорте отчёта по зарплате в PDF")
        (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера")))))

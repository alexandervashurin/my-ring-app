(ns my-ring-app.api.reports
  "REST API для генерации PDF отчётов"
  (:require [compojure.core :refer [defroutes GET]]
            [ring.util.response :as resp]
            [clojure.string :as str]
            [my-ring-app.model :as model]
            [my-ring-app.pdf-reports :as pdf]
            [my-ring-app.logger :as logger]
            [clojure.java.io :as io]))

;; ======================================================================
;; Вспомогательные функции
;; ======================================================================

(defn- validate-id [id]
  (and id (re-matches #"\d+" (str id))))

(defn- parse-int [s default]
  "Безопасное преобразование строки в число"
  (try
    (if (or (nil? s) (str/blank? s))
      default
      (Integer/parseInt (str/trim s)))
    (catch NumberFormatException e
      default)))

(defn- error-response
  "Стандартный ответ об ошибке"
  [code message]
  {:success false
   :error {:code code
           :message message}})

;; ======================================================================
;; API endpoints
;; ======================================================================

(defn export-worker-pdf
  "GET /api/reports/worker/:id/pdf — экспорт отчёта по работнику в PDF"
  [request]
  (let [temp-file (java.io.File/createTempFile "worker_" ".pdf")]
    (try
      (let [worker-id (-> request :route-params :id)]
        (if-not (validate-id worker-id)
          (do (.delete temp-file)
              (-> (resp/response (error-response "INVALID_ID" "Неверный формат ID работника"))
                  (resp/status 400)
                  (resp/content-type "application/json; charset=utf-8")))
          (if-let [result (pdf/generate-worker-pdf worker-id (.getAbsolutePath temp-file))]
            (if (:success result)
              (do
                (logger/log-info (format "API: GET /api/reports/worker/%s/pdf — сгенерирован PDF" worker-id))
                (.deleteOnExit temp-file)
                (-> (resp/response temp-file)
                    (resp/header "Content-Type" "application/pdf")
                    (resp/header "Content-Disposition" (str "attachment; filename=\"worker_" worker-id ".pdf\""))))
              (do (.delete temp-file)
                  (-> (resp/response (error-response "NOT_FOUND" (:message result)))
                      (resp/status 404)
                      (resp/content-type "application/json; charset=utf-8"))))
            (do (.delete temp-file)
                (-> (resp/response (error-response "GENERATION_ERROR" "Ошибка при генерации PDF"))
                    (resp/status 500)
                    (resp/content-type "application/json; charset=utf-8"))))))
      (catch Exception e
        (.delete temp-file)
        (logger/log-error e "API: Ошибка при экспорте работника в PDF")
        (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
            (resp/status 500)
            (resp/content-type "application/json; charset=utf-8"))))))

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
            (-> (resp/response temp-file)
                (resp/header "Content-Type" "application/pdf")
                (resp/header "Content-Disposition" "attachment; filename=\"workers_list.pdf\"")))
          (do (.delete temp-file)
              (-> (resp/response (error-response "GENERATION_ERROR" (:message result)))
                  (resp/status 500)
                  (resp/content-type "application/json; charset=utf-8"))))
        (do (.delete temp-file)
            (-> (resp/response (error-response "GENERATION_ERROR" "Ошибка при генерации PDF"))
                (resp/status 500)
                (resp/content-type "application/json; charset=utf-8"))))
      (catch Exception e
        (.delete temp-file)
        (logger/log-error e "API: Ошибка при экспорте списка работников в PDF")
        (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
            (resp/status 500)
            (resp/content-type "application/json; charset=utf-8"))))))

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
            (-> (resp/response temp-file)
                (resp/header "Content-Type" "application/pdf")
                (resp/header "Content-Disposition" (str "attachment; filename=\"salary_report_" year "_" month ".pdf\""))))
          (do (.delete temp-file)
              (-> (resp/response (error-response "GENERATION_ERROR" (:message result)))
                  (resp/status 500)
                  (resp/content-type "application/json; charset=utf-8"))))
        (do (.delete temp-file)
            (-> (resp/response (error-response "GENERATION_ERROR" "Ошибка при генерации PDF"))
                (resp/status 500)
                (resp/content-type "application/json; charset=utf-8"))))
      (catch Exception e
        (.delete temp-file)
        (logger/log-error e "API: Ошибка при экспорте отчёта по зарплате в PDF")
        (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
            (resp/status 500)
            (resp/content-type "application/json; charset=utf-8"))))))

;; ======================================================================
;; Маршруты API
;; ======================================================================

(defroutes api-routes
  (GET "/api/reports/worker/:id/pdf" [] export-worker-pdf)
  (GET "/api/reports/workers/pdf" [] export-workers-list-pdf)
  (GET "/api/reports/salary/pdf" [] export-salary-report-pdf))

(ns my-ring-app.api.notifications
  "REST API для email уведомлений"
  (:require [ring.util.response :as resp]
            [clojure.string :as str]
            [my-ring-app.email :as email]
            [my-ring-app.model :as model]
            [my-ring-app.logger :as logger]
            [my-ring-app.util :as util]))

;; ======================================================================
;; Вспомогательные функции
;; ======================================================================

(def ^:private parse-int util/parse-int)
(def ^:private success-response util/success-response)
(def ^:private error-response util/error-response)

(defn- parse-recipients
  "Парсинг и валидация списка получателей email.
   Возвращает вектор валидных email-адресов."
  [recipients-str]
  (->> (str/split (or recipients-str "") #",")
       (map str/trim)
       (filter seq)
       (filter util/validate-email)
       vec))

;; ======================================================================
;; API endpoints
;; ======================================================================

(defn test-email-api
  "GET /api/notifications/test — тестирование email подключения"
  [request]
  (try
    (let [result (email/test-email-connection)]
      (logger/log-info "API: GET /api/notifications/test")
      (-> (resp/response (success-response result "Тест email подключения"))
          (resp/content-type "application/json; charset=utf-8")))
    (catch Exception e
      (logger/log-error e "API: Ошибка при тестировании email")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn notify-new-worker-api
  "POST /api/notifications/new-worker — уведомление о новом работнике"
  [request]
  (try
    (let [params (:params request)
          worker-id (parse-int (:worker_id params) 0)
          recipients (parse-recipients (:recipients params))]
      (cond
        (zero? worker-id)
        (-> (resp/response (error-response "INVALID_ID" "ID работника обязателен"))
            (resp/status 400)
            (resp/content-type "application/json; charset=utf-8"))

        (empty? recipients)
        (-> (resp/response (error-response "INVALID_RECIPIENTS" "Укажите хотя бы один валидный email-адрес"))
            (resp/status 400)
            (resp/content-type "application/json; charset=utf-8"))

        :else
        (let [worker (model/get-record-by-id "Работник" (str worker-id))]
          (if worker
            (let [result (email/notify-new-worker worker recipients)]
              (logger/log-info (format "API: POST /api/notifications/new-worker — отправлено %d писем" (:sent result)))
              (-> (resp/response (success-response result (str "Отправлено " (:sent result) " писем")))
                  (resp/content-type "application/json; charset=utf-8")))
            (-> (resp/response (error-response "NOT_FOUND" "Работник не найден"))
                (resp/status 404)
                (resp/content-type "application/json; charset=utf-8"))))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при отправке уведомления о новом работнике")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn notify-birthday-api
  "POST /api/notifications/birthday — уведомление о дне рождения"
  [request]
  (try
    (let [params (:params request)
          worker-id (parse-int (:worker_id params) 0)
          age (parse-int (:age params) 0)
          recipients (parse-recipients (:recipients params))]
      (cond
        (or (zero? worker-id) (zero? age))
        (-> (resp/response (error-response "INVALID_PARAMS" "ID работника и возраст обязательны"))
            (resp/status 400)
            (resp/content-type "application/json; charset=utf-8"))

        (empty? recipients)
        (-> (resp/response (error-response "INVALID_RECIPIENTS" "Укажите хотя бы один валидный email-адрес"))
            (resp/status 400)
            (resp/content-type "application/json; charset=utf-8"))

        :else
        (let [worker (model/get-record-by-id "Работник" (str worker-id))]
          (if worker
            (let [result (email/notify-birthday worker age recipients)]
              (logger/log-info (format "API: POST /api/notifications/birthday — отправлено %d писем" (:sent result)))
              (-> (resp/response (success-response result (str "Отправлено " (:sent result) " писем")))
                  (resp/content-type "application/json; charset=utf-8")))
            (-> (resp/response (error-response "NOT_FOUND" "Работник не найден"))
                (resp/status 404)
                (resp/content-type "application/json; charset=utf-8"))))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при отправке уведомления о дне рождения")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn notify-anniversary-api
  "POST /api/notifications/anniversary — уведомление о годовщине работы"
  [request]
  (try
    (let [params (:params request)
          worker-id (parse-int (:worker_id params) 0)
          years (parse-int (:years params) 0)
          recipients (parse-recipients (:recipients params))]
      (cond
        (or (zero? worker-id) (zero? years))
        (-> (resp/response (error-response "INVALID_PARAMS" "ID работника и годы обязательны"))
            (resp/status 400)
            (resp/content-type "application/json; charset=utf-8"))

        (empty? recipients)
        (-> (resp/response (error-response "INVALID_RECIPIENTS" "Укажите хотя бы один валидный email-адрес"))
            (resp/status 400)
            (resp/content-type "application/json; charset=utf-8"))

        :else
        (let [worker (model/get-record-by-id "Работник" (str worker-id))]
          (if worker
            (let [result (email/notify-work-anniversary worker years recipients)]
              (logger/log-info (format "API: POST /api/notifications/anniversary — отправлено %d писем" (:sent result)))
              (-> (resp/response (success-response result (str "Отправлено " (:sent result) " писем")))
                  (resp/content-type "application/json; charset=utf-8")))
            (-> (resp/response (error-response "NOT_FOUND" "Работник не найден"))
                (resp/status 404)
                (resp/content-type "application/json; charset=utf-8"))))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при отправке уведомления о годовщине")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

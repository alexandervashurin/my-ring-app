(ns my-ring-app.api.notifications
  "REST API для email уведомлений"
  (:require [clojure.string :as str]
            [my-ring-app.email :as email]
            [my-ring-app.model :as model]
            [my-ring-app.logger :as logger]
            [my-ring-app.util :as util]))

(def ^:private parse-int util/parse-int)

(defn- parse-recipients
  "Парсинг и валидация списка получателей email.
   Возвращает вектор валидных email-адресов."
  [recipients-str]
  (->> (str/split (or recipients-str "") #",")
       (map str/trim)
       (filter seq)
       (filter util/validate-email)
       vec))

(defn test-email-api
  "GET /api/notifications/test — тестирование email подключения"
  [request]
  (try
    (let [result (email/test-email-connection)]
      (logger/log-info "API: GET /api/notifications/test")
      (util/json-ok result "Тест email подключения"))
    (catch Exception e
      (logger/log-error e "API: Ошибка при тестировании email")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

(defn notify-new-worker-api
  "POST /api/notifications/new-worker — уведомление о новом работнике"
  [request]
  (try
    (let [params (:params request)
          worker-id (parse-int (:worker_id params) 0)
          recipients (parse-recipients (:recipients params))]
      (cond
        (zero? worker-id)
        (util/json-error 400 "INVALID_ID" "ID работника обязателен")

        (empty? recipients)
        (util/json-error 400 "INVALID_RECIPIENTS" "Укажите хотя бы один валидный email-адрес")

        :else
        (let [org-id (:org-id request)
              worker (model/get-record-by-id "Работник" (str worker-id) org-id)]
          (if worker
            (let [result (email/notify-new-worker worker recipients)]
              (logger/log-info (format "API: POST /api/notifications/new-worker — отправлено %d писем" (:sent result)))
              (util/json-ok result (str "Отправлено " (:sent result) " писем")))
            (util/json-error 404 "NOT_FOUND" "Работник не найден")))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при отправке уведомления о новом работнике")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

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
        (util/json-error 400 "INVALID_PARAMS" "ID работника и возраст обязательны")

        (empty? recipients)
        (util/json-error 400 "INVALID_RECIPIENTS" "Укажите хотя бы один валидный email-адрес")

        :else
        (let [org-id (:org-id request)
              worker (model/get-record-by-id "Работник" (str worker-id) org-id)]
          (if worker
            (let [result (email/notify-birthday worker age recipients)]
              (logger/log-info (format "API: POST /api/notifications/birthday — отправлено %d писем" (:sent result)))
              (util/json-ok result (str "Отправлено " (:sent result) " писем")))
            (util/json-error 404 "NOT_FOUND" "Работник не найден")))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при отправке уведомления о дне рождения")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

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
        (util/json-error 400 "INVALID_PARAMS" "ID работника и годы обязательны")

        (empty? recipients)
        (util/json-error 400 "INVALID_RECIPIENTS" "Укажите хотя бы один валидный email-адрес")

        :else
        (let [org-id (:org-id request)
              worker (model/get-record-by-id "Работник" (str worker-id) org-id)]
          (if worker
            (let [result (email/notify-work-anniversary worker years recipients)]
              (logger/log-info (format "API: POST /api/notifications/anniversary — отправлено %d писем" (:sent result)))
              (util/json-ok result (str "Отправлено " (:sent result) " писем")))
            (util/json-error 404 "NOT_FOUND" "Работник не найден")))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при отправке уведомления о годовщине")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

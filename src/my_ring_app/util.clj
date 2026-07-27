(ns my-ring-app.util
  "Общие вспомогательные функции"
  (:require [clojure.string :as str]))

(defn parse-int
  "Безопасное преобразование строки в число"
  [s default]
  (try
    (if (or (nil? s) (str/blank? s))
      default
      (Integer/parseInt (str/trim (str s))))
    (catch NumberFormatException _
      default)))

(defn validate-id
  "Валидация и преобразование ID в число. Возвращает nil при некорректном ID"
  [id]
  (try
    (when id
      (let [cleaned (str/trim (str/replace (str id) #"[^0-9]" ""))]
        (when (seq cleaned)
          (Integer/parseInt cleaned))))
    (catch Exception _ nil)))

(defn success-response
  "Стандартный JSON-ответ об успехе"
  ([data]
   {:success true
    :data data
    :message "Операция выполнена успешно"})
  ([data message]
   {:success true
    :data data
    :message message}))

(defn error-response
  "Стандартный JSON-ответ об ошибке"
  ([code message]
   {:success false
    :error {:code code
            :message message}})
  ([code message details]
   {:success false
    :error {:code code
            :message message
            :details details}}))

(defn validate-email
  "Проверка формата email-адреса"
  [email]
  (boolean
   (and (string? email)
        (re-matches #"^[^@\s]+@[^@\s]+\.[^@\s]+$" (str/trim email)))))

(defn validate-year-month
  "Валидация года и месяца. Возвращает nil или карту с ошибкой"
  [year month]
  (cond
    (or (< year 2000) (> year 2100))
    {:error "INVALID_PARAMS" :message "Год должен быть от 2000 до 2100"}

    (or (< month 1) (> month 12))
    {:error "INVALID_PARAMS" :message "Месяц должен быть от 1 до 12"}

    :else nil))

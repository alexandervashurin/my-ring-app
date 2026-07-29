(ns my-ring-app.util
  "Общие вспомогательные функции"
  (:require [clojure.string :as str]
            [ring.util.response :as resp]))

(defn ^Integer parse-int
  "Безопасное преобразование строки в число"
  [s default]
  (try
    (if (nil? s)
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

(defn parse-worker-params
  "Парсинг параметров работника из request params"
  [params]
  {:фамилия (:фамилия params)
   :имя (:имя params)
   :отчество (:отчество params)
   :дата_приема (:дата_приема params)
   :цех_id (parse-int (:цех_id params) nil)
   :система_оплаты_id (parse-int (:система_оплаты_id params) nil)
   :категория_работника_id (parse-int (:категория_работника_id params) nil)
   :разряд_id (parse-int (:разряд_id params) nil)
   :режим_работы_id (parse-int (:режим_работы_id params) nil)
   :оклад_id (when (:оклад_id params) (parse-int (:оклад_id params) nil))
   :почасовая_ставка_id (when (:почасовая_ставка_id params) (parse-int (:почасовая_ставка_id params) nil))})

(defn parse-work-time-params
  "Парсинг параметров учета рабочего времени из request params"
  [params]
  {:год (parse-int (:год params) nil)
   :месяц (parse-int (:месяц params) nil)
   :всего_часов_за_месяц_по_плану (parse-int (:всего_часов_за_месяц_по_плану params) nil)
   :всего_часов_в_месяц_по_факту (parse-int (:всего_часов_в_месяц_по_факту params) nil)
   :количество_отработанных_дней (when (:количество_отработанных_дней params) (parse-int (:количество_отработанных_дней params) nil))
   :количество_рабочих_часов_в_день (when (:количество_рабочих_часов_в_день params) (parse-int (:количество_рабочих_часов_в_день params) nil))
   :всего_отработанных_часов (when (:всего_отработанных_часов params) (parse-int (:всего_отработанных_часов params) nil))
   :сколько_должны_отработать (when (:сколько_должны_отработать params) (parse-int (:сколько_должны_отработать params) nil))
   :больничные_дни (parse-int (:больничные_дни params) 0)
   :командировочные_дни (parse-int (:командировочные_дни params) 0)})

(defn json-response
  "Стандартный JSON ответ с Content-Type"
  ([data]
   (-> (resp/response data)
       (resp/content-type "application/json; charset=utf-8")))
  ([data status]
   (-> (resp/response data)
       (resp/status status)
       (resp/content-type "application/json; charset=utf-8"))))

(defn json-ok
  "JSON ответ 200 OK"
  ([data]
   (json-ok data nil))
  ([data message]
   (-> (resp/response (success-response data (or message "Операция выполнена успешно")))
       (resp/content-type "application/json; charset=utf-8"))))

(defn json-created
  "JSON ответ 201 Created"
  [data message]
  (-> (resp/response (success-response data message))
      (resp/status 201)
      (resp/content-type "application/json; charset=utf-8")))

(defn json-error
  "JSON ответ об ошибке с указанным статусом"
  [status code message]
  (-> (resp/response (error-response code message))
      (resp/status status)
      (resp/content-type "application/json; charset=utf-8")))

(defn json-error-details
  "JSON ответ об ошибке с деталями"
  [status code message details]
  (-> (resp/response (error-response code message details))
      (resp/status status)
      (resp/content-type "application/json; charset=utf-8")))

(defn file-download
  "Ответ для скачивания файла"
  [content content-type filename]
  (-> (resp/response content)
      (resp/header "Content-Type" content-type)
      (resp/header "Content-Disposition" (str "attachment; filename=\"" filename "\""))))

(defn pagination-meta
  "Метаданные пагинации для ответа"
  [total page per-page]
  {:total total
   :page page
   :per_page per-page
   :total_pages (int (Math/ceil (/ (double total) (double per-page))))})

(defn validate-year-month
  "Валидация года и месяца. Возвращает nil или карту с ошибкой"
  [year month]
  (cond
    (or (< year 2000) (> year 2100))
    {:error "INVALID_PARAMS" :message "Год должен быть от 2000 до 2100"}

    (or (< month 1) (> month 12))
    {:error "INVALID_PARAMS" :message "Месяц должен быть от 1 до 12"}

    :else nil))

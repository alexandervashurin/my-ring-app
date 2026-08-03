(ns my-ring-app.api.workers
  "REST API для работников"
  (:require [clojure.string :as str]
            [my-ring-app.model :as model]
            [my-ring-app.validation :as validation]
            [my-ring-app.auth :as auth]
            [my-ring-app.tariff :as tariff]
            [my-ring-app.util :as util]
            [my-ring-app.logger :as logger]))

(def ^:private parse-int util/parse-int)
(def ^:private validate-id util/validate-id)
(def ^:private parse-worker-params util/parse-worker-params)

(defn format-worker
  "Форматирование данных работника для API"
  [worker]
  (when worker
    (-> worker
        (select-keys [:id :фамилия :имя :отчество :дата_приема
                      :цех_id :система_оплаты_id :категория_работника_id
                      :разряд_id :режим_работы_id :оклад_id :почасовая_ставка_id])
        (assoc :цех (:цех worker)
               :система_оплаты (:система worker)
               :категория (:категория worker)
               :разряд (:разряд worker)
               :режим (:режим worker)))))

(defn get-workers
  "GET /api/workers — получение списка работников с пагинацией"
  [request]
  (try
    (let [user (:identity request)
          org-id (:org-id request)
          query-params (:params request)
          search (:search query-params)
          page (max 1 (parse-int (:page query-params) 1))
          per-page (max 1 (min 100 (parse-int (:per_page query-params) 20)))
          offset (* (- page 1) per-page)
          workers (if (and search (not (str/blank? search)))
                    (model/search-workers search org-id)
                    (model/get-workers-with-details org-id))
          total (count workers)
          total-pages (int (Math/ceil (/ total (double per-page))))
          paginated-workers (take per-page (drop offset workers))]
      (logger/log-info (format "API: GET /api/workers (поиск: %s, страница: %d, размер: %d, найдено: %d, org: %s)"
                               (or search "-") page per-page (count paginated-workers) (str org-id)))
      (util/json-ok
       {:workers (map format-worker paginated-workers)
        :pagination {:page page
                     :per_page per-page
                     :total total
                     :total_pages total-pages
                     :has_next (< page total-pages)
                     :has_prev (> page 1)}}
       (str "Получено " (count paginated-workers) " из " total " работников")))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении списка работников")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

(defn get-worker-by-id
  "GET /api/workers/:id — получение работника по ID (только своей организации)"
  [request]
  (try
    (let [id (-> request :route-params :id validate-id)
          org-id (:org-id request)]
      (if (nil? id)
        (util/json-error 400 "INVALID_ID" "Некорректный идентификатор")
        (let [worker (model/get-record-by-id "Работник" (str id) org-id)]
          (if worker
            (do
              (logger/log-info (format "API: GET /api/workers/%d (org: %s)" id (str org-id)))
              (util/json-ok (format-worker worker)))
            (util/json-error 404 "NOT_FOUND" "Работник не найден")))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении работника по ID")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

(defn create-worker
  "POST /api/workers — создание работника"
  [request]
  (try
    (let [user (:identity request)
          org-id (:org-id request)
          worker-data (:params request)
          validation-result (validation/validate-worker worker-data)]
      (if (:valid? validation-result)
        (let [limit-check (tariff/check-worker-limit org-id)]
          (if (:allowed limit-check)
            (let [data (parse-worker-params worker-data)
                  result (model/create-record "Работник" data org-id)]
              (if (:success result)
                (do
                  (logger/log-audit "CREATE" "Worker" (:id result)
                                    (format "Создан работник %s %s (API, org: %s)" (:фамилия data) (:имя data) (str org-id)))
                  (logger/log-info (format "API: POST /api/workers — создан работник ID=%s (org: %s)" (str (:id result)) (str org-id)))
                  (util/json-created
                   (format-worker (model/get-record-by-id "Работник" (str (:id result))))
                   "Работник успешно создан"))
                (util/json-error 500 "CREATE_ERROR" (:message result))))
            (util/json-error 403 "LIMIT_EXCEEDED" (:message limit-check))))
        (do
          (logger/log-warn (format "API: Валидация не пройдена: %s" (str/join ", " (:errors validation-result))))
          (util/json-error-details 400 "VALIDATION_ERROR" "Ошибка валидации данных" (:errors validation-result)))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при создании работника")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

(defn update-worker
  "PUT /api/workers/:id — обновление работника (только своей организации)"
  [request]
  (try
    (let [id (-> request :route-params :id validate-id)
          org-id (:org-id request)
          worker-data (:params request)]
      (if (nil? id)
        (util/json-error 400 "INVALID_ID" "Некорректный идентификатор")
        (if-not (model/get-record-by-id "Работник" (str id) org-id)
          (util/json-error 404 "NOT_FOUND" "Работник не найден")
          (let [validation-result (validation/validate-worker worker-data)]
            (if (:valid? validation-result)
              (let [data (parse-worker-params worker-data)
                    result (model/update-record "Работник" id data)]
                (if (:success result)
                  (do
                    (logger/log-audit "UPDATE" "Worker" id
                                      (format "Обновлен работник %s %s (API)" (:фамилия data) (:имя data)))
                    (logger/log-info (format "API: PUT /api/workers/%d — обновлён работник" id))
                    (util/json-ok
                     (format-worker (model/get-record-by-id "Работник" (str id) org-id))
                     "Работник успешно обновлён"))
                  (util/json-error 500 "UPDATE_ERROR" (:message result))))
              (util/json-error-details 400 "VALIDATION_ERROR" "Ошибка валидации данных" (:errors validation-result)))))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при обновлении работника")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

(defn delete-worker
  "DELETE /api/workers/:id — удаление работника"
  [request]
  (try
    (let [id (-> request :route-params :id validate-id)]
      (if (nil? id)
        (util/json-error 400 "INVALID_ID" "Некорректный идентификатор")
        (let [result (model/delete-record "Работник" id)]
          (if (:success result)
            (do
              (logger/log-audit "DELETE" "Worker" id "Работник удалён (API)")
              (logger/log-info (format "API: DELETE /api/workers/%d — удалён работник" id))
              (util/json-ok nil "Работник успешно удалён"))
            (util/json-error 500 "DELETE_ERROR" (:message result))))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при удалении работника")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

(defn search-workers-api
  "GET /api/workers/search — поиск работников"
  [request]
  (try
    (let [org-id (:org-id request)
          query-params (:params request)
          query (:q query-params)]
      (if (or (nil? query) (str/blank? query))
        (util/json-error 400 "MISSING_QUERY" "Параметр поиска 'q' обязателен")
        (let [workers (model/search-workers query org-id)]
          (logger/log-info (format "API: GET /api/workers/search (запрос: %s, найдено: %d, org: %s)" query (count workers) (str org-id)))
          (util/json-ok
           (vec (map format-worker workers))
           (str "Найдено " (count workers) " работников по запросу '" query "'")))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при поиске работников")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

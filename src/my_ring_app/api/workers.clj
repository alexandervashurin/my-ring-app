(ns my-ring-app.api.workers
  "REST API для работников"
  (:require [compojure.core :refer [defroutes GET POST PUT DELETE]]
            [ring.util.response :as resp]
            [clojure.string :as str]
            [my-ring-app.model :as model]
            [my-ring-app.validation :as validation]
            [my-ring-app.auth :as auth]
            [my-ring-app.util :as util]
            [my-ring-app.logger :as logger]))

;; ======================================================================
;; Вспомогательные функции
;; ======================================================================

(def ^:private parse-int util/parse-int)
(def ^:private validate-id util/validate-id)
(def ^:private success-response util/success-response)
(def ^:private error-response util/error-response)
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

;; ======================================================================
;; API endpoints
;; ======================================================================

(defn get-workers
  "GET /api/workers — получение списка работников с пагинацией"
  [request]
  (try
    (let [user (:identity request)
          query-params (:params request)
          search (:search query-params)
          page (max 1 (parse-int (:page query-params) 1))
          per-page (max 1 (min 100 (parse-int (:per_page query-params) 20)))
          offset (* (- page 1) per-page)
          workers (if (and search (not (str/blank? search)))
                    (model/search-workers search)
                    (model/get-workers-with-details))
          total (count workers)
          total-pages (int (Math/ceil (/ total (double per-page))))
          paginated-workers (take per-page (drop offset workers))]
      (logger/log-info (format "API: GET /api/workers (поиск: %s, страница: %d, размер: %d, найдено: %d)"
                               (or search "-") page per-page (count paginated-workers)))
      (-> (resp/response (success-response
                          {:workers (map format-worker paginated-workers)
                           :pagination {:page page
                                        :per_page per-page
                                        :total total
                                        :total_pages total-pages
                                        :has_next (< page total-pages)
                                        :has_prev (> page 1)}}
                          (str "Получено " (count paginated-workers) " из " total " работников")))
          (resp/content-type "application/json; charset=utf-8")))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении списка работников")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn get-worker-by-id
  "GET /api/workers/:id — получение работника по ID"
  [request]
  (try
    (let [id (-> request :route-params :id validate-id)]
      (if (nil? id)
        (-> (resp/response (error-response "INVALID_ID" "Некорректный идентификатор"))
            (resp/status 400)
            (resp/content-type "application/json; charset=utf-8"))
        (let [worker (model/get-record-by-id "Работник" (str id))]
          (if worker
            (do
              (logger/log-info (format "API: GET /api/workers/%d" id))
              (-> (resp/response (success-response (format-worker worker)))
                  (resp/content-type "application/json; charset=utf-8")))
            (-> (resp/response (error-response "NOT_FOUND" "Работник не найден"))
                (resp/status 404)
                (resp/content-type "application/json; charset=utf-8"))))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении работника по ID")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn create-worker
  "POST /api/workers — создание работника"
  [request]
  (try
    (let [user (:identity request)
          worker-data (:params request)
          validation-result (validation/validate-worker worker-data)]
      (if (:valid? validation-result)
        (let [data (parse-worker-params worker-data)
              result (model/create-record "Работник" data)]
          (if (:success result)
            (do
              (logger/log-audit "CREATE" "Worker" (:id result)
                                (format "Создан работник %s %s (API)" (:фамилия data) (:имя data)))
              (logger/log-info (format "API: POST /api/workers — создан работник ID=%s" (str (:id result))))
              (-> (resp/response (success-response
                                  (format-worker (model/get-record-by-id "Работник" (str (:id result))))
                                  "Работник успешно создан"))
                  (resp/status 201)
                  (resp/content-type "application/json; charset=utf-8")))
            (-> (resp/response (error-response "CREATE_ERROR" (:message result)))
                (resp/status 500)
                (resp/content-type "application/json; charset=utf-8"))))
        (do
          (logger/log-warn (format "API: Валидация не пройдена: %s" (str/join ", " (:errors validation-result))))
          (-> (resp/response (error-response "VALIDATION_ERROR" "Ошибка валидации данных" (:errors validation-result)))
              (resp/status 400)
              (resp/content-type "application/json; charset=utf-8")))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при создании работника")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn update-worker
  "PUT /api/workers/:id — обновление работника"
  [request]
  (try
    (let [id (-> request :route-params :id validate-id)
          worker-data (:params request)]
      (if (nil? id)
        (-> (resp/response (error-response "INVALID_ID" "Некорректный идентификатор"))
            (resp/status 400)
            (resp/content-type "application/json; charset=utf-8"))
            (let [validation-result (validation/validate-worker worker-data)]
          (if (:valid? validation-result)
            (let [data (parse-worker-params worker-data)
                  result (model/update-record "Работник" id data)]
              (if (:success result)
                (do
                  (logger/log-audit "UPDATE" "Worker" id
                                    (format "Обновлен работник %s %s (API)" (:фамилия data) (:имя data)))
                  (logger/log-info (format "API: PUT /api/workers/%d — обновлён работник" id))
                  (-> (resp/response (success-response
                                      (format-worker (model/get-record-by-id "Работник" (str id)))
                                      "Работник успешно обновлён"))
                      (resp/content-type "application/json; charset=utf-8")))
                (-> (resp/response (error-response "UPDATE_ERROR" (:message result)))
                    (resp/status 500)
                    (resp/content-type "application/json; charset=utf-8"))))
            (-> (resp/response (error-response "VALIDATION_ERROR" "Ошибка валидации данных" (:errors validation-result)))
                (resp/status 400)
                (resp/content-type "application/json; charset=utf-8"))))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при обновлении работника")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn delete-worker
  "DELETE /api/workers/:id — удаление работника"
  [request]
  (try
    (let [id (-> request :route-params :id validate-id)]
      (if (nil? id)
        (-> (resp/response (error-response "INVALID_ID" "Некорректный идентификатор"))
            (resp/status 400)
            (resp/content-type "application/json; charset=utf-8"))
        (let [result (model/delete-record "Работник" id)]
          (if (:success result)
            (do
              (logger/log-audit "DELETE" "Worker" id "Работник удалён (API)")
              (logger/log-info (format "API: DELETE /api/workers/%d — удалён работник" id))
              (-> (resp/response (success-response nil "Работник успешно удалён"))
                  (resp/content-type "application/json; charset=utf-8")))
            (-> (resp/response (error-response "DELETE_ERROR" (:message result)))
                (resp/status 500)
                (resp/content-type "application/json; charset=utf-8"))))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при удалении работника")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn search-workers-api
  "GET /api/workers/search — поиск работников"
  [request]
  (try
    (let [query-params (:params request)
          query (:q query-params)]
      (if (or (nil? query) (str/blank? query))
        (-> (resp/response (error-response "MISSING_QUERY" "Параметр поиска 'q' обязателен"))
            (resp/status 400)
            (resp/content-type "application/json; charset=utf-8"))
        (let [workers (model/search-workers query)]
          (logger/log-info (format "API: GET /api/workers/search (запрос: %s, найдено: %d)" query (count workers)))
          (-> (resp/response (success-response
                               (vec (map format-worker workers))
                              (str "Найдено " (count workers) " работников по запросу '" query "'")))
              (resp/content-type "application/json; charset=utf-8")))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при поиске работников")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

;; ======================================================================
;; Маршруты API
;; ======================================================================

(defroutes api-routes
  ;; Работники
  (GET "/api/workers" [] get-workers)
  (GET "/api/workers/search" [] search-workers-api)
  (GET "/api/workers/:id" [] get-worker-by-id)
  (POST "/api/workers" [] create-worker)
  (PUT "/api/workers/:id" [] update-worker)
  (DELETE "/api/workers/:id" [] delete-worker))

(ns my-ring-app.controllers
  (:require [ring.util.response :as resp]
            [clojure.string :as str]
            [my-ring-app.model :as model]
            [my-ring-app.views.layout :as layout]
            [my-ring-app.views.home :as home]
            [my-ring-app.views.workers :as workers]
            [my-ring-app.views.salary :as salary]
            [my-ring-app.views.work-time :as work-time]
            [my-ring-app.views.tables :as tables]
            [my-ring-app.views.dashboard :as dashboard]
            [my-ring-app.validation :as validation]
            [my-ring-app.logger :as logger]
            [my-ring-app.util :as util]
            [my-ring-app.auth :as auth]
            [my-ring-app.tariff :as tariff]
            [my-ring-app.cache :as cache]
            [my-ring-app.config :refer [url]]))

;; ======================================================================
;; Вспомогательные функции
;; ======================================================================

(def ^:private parse-int util/parse-int)
(def ^:private validate-id util/validate-id)
(def ^:private parse-worker-params util/parse-worker-params)
(def ^:private parse-work-time-params util/parse-work-time-params)

(defn- bad-request
  "Возвращает ответ с ошибкой 400"
  [message]
  (util/html-response message 400))

(defn- extract-org-id
  "Извлечение organization_id из запроса"
  [request]
  (when (map? request)
    (or (:org-id request)
        (get-in request [:identity :organization_id]))))

(defn- extract-params
  "Извлекает params из request или возвращает как есть, если это уже map"
  [request-or-params]
  (if (map? request-or-params)
    (if (:params request-or-params)
      (:params request-or-params)
      request-or-params)
    request-or-params))

(defn- extract-id-from-request
  "Извлекает id из params request"
  [request]
  (get-in request [:params :id]))

(defn- load-worker-form-data
  "Загрузка всех справочников для формы работника (из кэша)"
  []
  {:цеха (cache/get-shops)
   :системы_оплаты (cache/get-salary-systems)
   :категории (cache/get-worker-categories)
   :разряды (cache/get-qualifications)
   :режимы (cache/get-work-modes)
   :оклады (cache/get-salary-grades)
   :ставки (cache/get-hourly-rates)})

(defn- render-new-worker-error-response
  "Рендер ответа с ошибкой для формы создания работника"
  [form-data errors params]
  (util/html-response
    (workers/render-new-worker-page (:цеха form-data) (:системы_оплаты form-data) (:категории form-data)
                                    (:разряды form-data) (:режимы form-data) (:оклады form-data) (:ставки form-data)
                                    :errors errors
                                    :worker-data params)))

(defn- render-edit-worker-error-response
  "Рендер ответа с ошибкой для формы редактирования работника"
  [worker form-data errors]
  (util/html-response
    (workers/render-edit-worker-page worker (:цеха form-data) (:системы_оплаты form-data)
                                      (:категории form-data) (:разряды form-data)
                                      (:режимы form-data) (:оклады form-data) (:ставки form-data)
                                      :errors errors)))

;; ======================================================================
;; Контроллер главной страницы
;; ======================================================================

(defn home-page []
  (logger/log-info "Открыта главная страница")
  (util/html-response (home/render-home)))

;; ======================================================================
;; Контроллер дашборда
;; ======================================================================

(defn dashboard-page
  ([] (dashboard-page nil))
  ([request]
   (logger/log-info "Открыта страница дашборда")
   (let [org-id (extract-org-id request)
          dashboard-data (model/get-dashboard-data org-id)]
      (util/html-response (dashboard/render-dashboard-page dashboard-data)))))

;; ======================================================================
;; Контроллеры работников
;; ======================================================================

(defn workers-page
  ([] (workers-page nil))
  ([request-or-params]
   (let [params (extract-params request-or-params)
         org-id (extract-org-id request-or-params)
         query (:search params)
         page (max 1 (util/parse-int (:page params) 1))
         per-page (max 1 (util/parse-int (:per_page params) 10))
         result (if (and query (not (str/blank? query)))
                  (model/get-workers-page org-id page per-page query)
                  (model/get-workers-page org-id page per-page))
         workers (:items result)
         total (:total result)
         total-pages (int (Math/ceil (/ (double total) (double per-page))))]
     (logger/log-info (format "Открыт список работников (поиск: %s, найдено: %d, стр: %d/%d, org: %s)"
                               (or query "-") total page total-pages (str org-id)))
      (util/html-response (workers/render-workers-page workers query page total-pages total)))))

(defn new-worker-form
  ([] (new-worker-form nil))
  ([request-or-params]
   (let [params (extract-params request-or-params)
         form-data (load-worker-form-data)
          errors (when-let [err-str (:errors params)]
                   (str/split err-str #","))]
     (logger/log-info "Открыта форма создания работника")
     (util/html-response
       (workers/render-new-worker-page (:цеха form-data) (:системы_оплаты form-data) (:категории form-data)
                                       (:разряды form-data) (:режимы form-data) (:оклады form-data) (:ставки form-data)
                                       :errors errors
                                       :worker-data params)))))

(defn edit-worker-form
  ([id] (edit-worker-form id nil))
  ([id-or-request request-or-params]
   (let [id (if (number? id-or-request) id-or-request
              (extract-id-from-request id-or-request))
         params (extract-params (or request-or-params id-or-request))
         org-id (extract-org-id id-or-request)]
      (let [worker-id (validate-id id)]
        (if (nil? worker-id)
          (bad-request "Некорректный идентификатор работника")
          (do
            (logger/log-info (format "Открыта форма редактирования работника ID=%s (org: %s)" worker-id (str org-id)))
            (let [worker (model/get-record-by-id "Работник" (str worker-id) org-id)
                  form-data (load-worker-form-data)
           errors (when-let [err-str (:errors params)]
                            (str/split err-str #","))]
              (if worker
                 (util/html-response
                   (workers/render-edit-worker-page worker (:цеха form-data) (:системы_оплаты form-data)
                                                     (:категории form-data) (:разряды form-data)
                                                     (:режимы form-data) (:оклады form-data) (:ставки form-data)
                                                     :errors errors))
                 (util/html-response "Работник не найден" 404)))))))))

(defn- handle-create-exception [params e]
  (logger/log-error ^Throwable e "Критическая ошибка при создании работника")
  (render-new-worker-error-response (load-worker-form-data) ["Внутренняя ошибка при создании работника"] params))

(defn- handle-create-result [params org-id result]
  (if (:success result)
    (do
      (logger/log-audit "CREATE" "Worker" (:id result)
                        (format "Создан работник %s %s (org: %s)" (:фамилия params) (:имя params) (str org-id)))
      (logger/log-info (format "Работник успешно создан, ID=%s (org: %s)" (:id result) (str org-id)))
      (resp/redirect (url "/workers")))
    (do
      (logger/log-error ^Throwable (Exception. (:message result)) "Ошибка при создании работника")
      (render-new-worker-error-response (load-worker-form-data) [(:message result)] params))))

(defn create-worker
  ([] (create-worker nil))
  ([request-or-params]
   (let [params (extract-params request-or-params)]
     (logger/log-info "Попытка создания работника")
     (let [org-id (extract-org-id request-or-params)
           limit-check (tariff/check-worker-limit org-id)]
       (if (not (:allowed limit-check))
         (do
           (logger/log-warn (format "Лимит работников превышен: %s" (:message limit-check)))
           (render-new-worker-error-response (load-worker-form-data) [(:message limit-check)] params))
         (let [validation-result (validation/validate-worker params)]
           (if (not (:valid? validation-result))
             (do
               (logger/log-warn (format "Валидация не пройдена: %s" (str/join ", " (:errors validation-result))))
               (render-new-worker-error-response (load-worker-form-data) (:errors validation-result) params))
             (let [data (parse-worker-params params)]
               (try
                 (let [result (model/create-record "Работник" data org-id)]
                   (handle-create-result params org-id result))
                 (catch Exception e
                   (handle-create-exception params e)))))))))))

(defn update-worker
  ([id] (update-worker id nil))
  ([id-or-request request-or-params]
   (let [id (if (number? id-or-request) id-or-request
              (extract-id-from-request id-or-request))
         params (extract-params (or request-or-params id-or-request))
         org-id (extract-org-id id-or-request)]
     (let [worker-id (validate-id id)]
       (if (nil? worker-id)
         (bad-request "Некорректный идентификатор работника")
         (if-not (model/get-record-by-id "Работник" (str worker-id) org-id)
           (resp/redirect (url "/workers"))
           (do
             (logger/log-info (format "Попытка обновления работника ID=%s (org: %s)" worker-id (str org-id)))
             (let [validation-result (validation/validate-worker params)]
               (if (:valid? validation-result)
                 (let [data (parse-worker-params params)
                       result (model/update-record "Работник" worker-id data)]
                   (if (:success result)
                     (do
                       (logger/log-audit "UPDATE" "Worker" worker-id
                                         (format "Обновлен работник %s %s" (:фамилия params) (:имя params)))
                       (logger/log-info (format "Работник успешно обновлен, ID=%s" worker-id))
                       (resp/redirect (url "/workers")))
                     (do
                       (logger/log-error ^Throwable (Exception. (:message result)) "Ошибка при обновлении работника")
                       (render-edit-worker-error-response (model/get-record-by-id "Работник" (str worker-id) org-id)
                                                          (load-worker-form-data) [(:message result)]))))
                 (do
                   (logger/log-warn (format "Валидация не пройдена: %s" (str/join ", " (:errors validation-result))))
                   (render-edit-worker-error-response (merge (model/get-record-by-id "Работник" (str worker-id) org-id) params)
                                                      (load-worker-form-data) (:errors validation-result))))))))))))

(defn delete-worker
  ([] (delete-worker nil))
  ([id-or-request]
   (let [id (if (number? id-or-request) id-or-request
              (extract-id-from-request id-or-request))]
     (let [worker-id (validate-id id)]
       (if (nil? worker-id)
         (bad-request "Некорректный идентификатор работника")
         (do
           (logger/log-info (format "Попытка удаления работника ID=%s" worker-id))
           (let [result (model/delete-record "Работник" worker-id)]
             (if (:success result)
               (do
                 (logger/log-audit "DELETE" "Worker" worker-id "Работник удален")
                 (logger/log-info (format "Работник успешно удален, ID=%s" worker-id))
                  (resp/redirect (url "/workers")))
                (do
                  (logger/log-error ^Throwable (Exception. (:message result)) "Ошибка при удалении работника")
                  (resp/redirect (url "/workers")))))))))))

;; ======================================================================
;; Контроллеры зарплаты
;; ======================================================================

(defn worker-salary-page
  ([] (worker-salary-page nil))
  ([id-or-request]
   (let [id (if (number? id-or-request) id-or-request
              (extract-id-from-request id-or-request))
         org-id (extract-org-id id-or-request)]
     (let [worker-id (validate-id id)]
       (if (nil? worker-id)
         (bad-request "Некорректный идентификатор работника")
         (do
            (logger/log-info (format "Открыта страница зарплаты работника ID=%s (org: %s)" worker-id (str org-id)))
             (let [worker (model/get-record-by-id "Работник" (str worker-id) org-id)
                   [current-year current-month] (model/current-year-month)
                   requested-year (parse-int (get-in id-or-request [:params :year]) nil)
                   requested-month (parse-int (get-in id-or-request [:params :month]) nil)
                   period-error (when (and requested-year requested-month)
                                  (util/validate-year-month requested-year requested-month))
                   [selected-year selected-month] (if period-error
                                                    [current-year current-month]
                                                    [(or requested-year current-year)
                                                     (or requested-month current-month)])
                   salary-info (model/get-worker-salary worker-id selected-year selected-month org-id)
                   salary-history (model/get-worker-salary-history worker-id org-id)]
               (if worker
                 (util/html-response (salary/render-salary-page worker salary-info salary-history
                                                                selected-year selected-month))
                  (resp/redirect (url "/workers"))))))))))

;; ======================================================================
;; Контроллеры учета времени
;; ======================================================================

(defn worker-work-time-page
  ([] (worker-work-time-page nil))
  ([id-or-request]
   (let [id (if (number? id-or-request) id-or-request
              (extract-id-from-request id-or-request))
         org-id (extract-org-id id-or-request)]
     (let [worker-id (validate-id id)]
       (if (nil? worker-id)
         (bad-request "Некорректный идентификатор работника")
         (do
           (logger/log-info (format "Открыта страница учета времени работника ID=%s (org: %s)" worker-id (str org-id)))
            (let [worker (model/get-record-by-id "Работник" (str worker-id) org-id)
                  work-time-records (model/get-worker-work-time worker-id org-id)]
              (if worker
                (util/html-response (work-time/render-work-time-page worker work-time-records))
                 (resp/redirect (url "/workers"))))))))))

(defn edit-work-time-form
  ([] (edit-work-time-form nil))
  ([id-or-request]
   (let [id (if (number? id-or-request) id-or-request
              (extract-id-from-request id-or-request))
         org-id (extract-org-id id-or-request)]
      (let [work-time-id (validate-id id)]
        (if (nil? work-time-id)
          (bad-request "Некорректный идентификатор записи")
          (do
            (logger/log-info (format "Открыта форма редактирования учета времени ID=%s (org: %s)" work-time-id (str org-id)))
            (let [work-time-record (model/get-work-time-by-id (str work-time-id) org-id)
                  worker (when work-time-record
                           (model/get-record-by-id "Работник" (:работник_id work-time-record) org-id))]
               (if (and work-time-record worker)
                 (util/html-response (work-time/render-edit-work-time-form work-time-record worker))
                  (resp/redirect (url "/workers"))))))))))

(defn update-work-time
  ([] (update-work-time nil nil))
  ([id-or-request request-or-params]
   (let [id (if (number? id-or-request) id-or-request
              (extract-id-from-request id-or-request))
         params (extract-params (or request-or-params id-or-request))
         org-id (extract-org-id id-or-request)]
     (let [work-time-id (validate-id id)]
       (if (nil? work-time-id)
         (bad-request "Некорректный идентификатор записи")
         (if-not (model/get-work-time-by-id (str work-time-id) org-id)
           (resp/redirect (url "/workers"))
           (do
             (logger/log-info (format "Попытка обновления учета времени ID=%s (org: %s)" work-time-id (str org-id)))
             (let [validation-result (validation/validate-work-time params)]
               (if (:valid? validation-result)
                 (let [data (parse-work-time-params params)
                       result (model/update-record "Учет_рабочего_времени" work-time-id data)]
                   (if (:success result)
                     (do
                       (let [work-time-record (model/get-work-time-by-id (str work-time-id) org-id)
                             worker-id (:работник_id work-time-record)]
                         (logger/log-audit "UPDATE" "WorkTime" work-time-id
                                           (format "Обновлен учет времени для работника ID=%s" (str worker-id)))
                         (logger/log-info (format "Учет времени успешно обновлен, ID=%s" work-time-id))
                         (resp/redirect (url (str "/workers/" (or worker-id "?error=unknown") "/work-time")))))
                     (do
                       (logger/log-error ^Throwable (Exception. (:message result)) "Ошибка при обновлении учета времени")
                       (let [work-time-record (model/get-work-time-by-id (str work-time-id) org-id)
                             worker (model/get-record-by-id "Работник" (:работник_id work-time-record) org-id)]
                         (util/html-response (work-time/render-edit-work-time-form work-time-record worker :errors [(:message result)]))))))
                 (do
                   (logger/log-warn (format "Валидация учета времени не пройдена: %s" (str/join ", " (:errors validation-result))))
                   (let [work-time-record (merge (model/get-work-time-by-id (str work-time-id) org-id) params)
                         worker (model/get-record-by-id "Работник" (:работник_id work-time-record) org-id)]
                      (util/html-response (work-time/render-edit-work-time-form work-time-record worker :errors (:errors validation-result))))))))))))))

;; ======================================================================
;; Контроллеры БД
;; ======================================================================

(defn all-tables-page
  ([] (all-tables-page nil))
  ([request]
    (logger/log-info "Открыта страница просмотра всех таблиц")
    (let [tables (model/get-tables)
          tables-data (mapv (fn [table-name]
                              {:table table-name
                               :rows (model/get-table-data table-name 200)
                               :total (model/count-table-rows table-name)})
                            tables)]
      (util/html-response (tables/render-all-tables-page tables-data)))))

;; ======================================================================
;; Контроллер ошибок
;; ======================================================================

(defn not-found-page [request]
  (logger/log-warn "Страница не найдена")
  (let [lang (get-in request [:session :lang] "ru")
        user (get-in request [:session :user])
        content (layout/wrap-html
                  "<div class='alert alert-danger'><h2>404</h2><p>Страница не найдена</p><a href='/' class='btn btn-primary'>На главную</a></div>"
                  "Не найдено"
                  nil
                  user
                  lang)]
    (util/html-response content 404)))

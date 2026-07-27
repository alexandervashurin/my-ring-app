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
            [my-ring-app.logger :as logger]))

;; ======================================================================
;; Вспомогательные функции
;; ======================================================================

(defn- parse-int [s default]
  "Безопасное преобразование строки в число"
  (try
    (if (or (nil? s) (str/blank? s))
      default
      (Integer/parseInt (str/trim s)))
    (catch NumberFormatException e
      default)))

(defn- clean-id [id]
  "Очистка ID от лишних символов, оставляем только цифры"
  (when id
    (let [cleaned (str/trim (str/replace (str id) #"[^0-9]" ""))]
      (if (seq cleaned) cleaned nil))))

(defn- validate-id [id]
  "Валидация и преобразование ID в число"
  (try
    (let [cleaned (clean-id id)]
      (if (seq cleaned)
        (Integer/parseInt cleaned)
        (throw (Exception. "Некорректный ID"))))
    (catch Exception e
      (logger/log-error e (format "Некорректный ID: '%s'" (str id)))
      nil)))

(defn- bad-request [message]
  "Возвращает ответ с ошибкой 400"
  (-> (resp/response message)
      (resp/status 400)
      (resp/content-type "text/html; charset=utf-8")))

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

;; ======================================================================
;; Контроллер главной страницы
;; ======================================================================

(defn home-page []
  (logger/log-info "Открыта главная страница")
  (-> (resp/response (home/render-home))
      (resp/content-type "text/html; charset=utf-8")))

;; ======================================================================
;; Контроллер дашборда
;; ======================================================================

(defn dashboard-page
  ([] (dashboard-page nil))
  ([request]
   (logger/log-info "Открыта страница дашборда")
   (let [dashboard-data (model/get-dashboard-data)]
     (-> (resp/response (dashboard/render-dashboard-page dashboard-data))
         (resp/content-type "text/html; charset=utf-8")))))

;; ======================================================================
;; Контроллеры работников
;; ======================================================================

(defn workers-page
  ([] (workers-page nil))
  ([request-or-params]
   (let [params (extract-params request-or-params)
         query (:search params)
         workers (if (and query (not (str/blank? query)))
                   (model/search-workers query)
                   (model/get-workers-with-details))]
     (logger/log-info (format "Открыт список работников (поиск: %s, найдено: %d)"
                              (or query "-") (count workers)))
     (-> (resp/response (workers/render-workers-page workers query))
         (resp/content-type "text/html; charset=utf-8")))))

(defn new-worker-form
  ([] (new-worker-form nil))
  ([request-or-params]
   (let [params (extract-params request-or-params)]
     (logger/log-info "Открыта форма создания работника")
     (let [цеха (model/get-spravochnik "Цех")
           системы_оплаты (model/get-spravochnik "Система_оплаты")
           категории (model/get-spravochnik "Категория_работника")
           разряды (model/get-spravochnik "Разряд")
           режимы (model/get-spravochnik "Режим_работы")
           оклады (model/get-spravochnik "Оклад")
           ставки (model/get-spravochnik "Почасовые_ставки")
           errors (when-let [err-str (:errors params)]
                    (clojure.string/split err-str #","))]
       (-> (resp/response (workers/render-new-worker-page цеха системы_оплаты категории разряды режимы оклады ставки
                                                          :errors errors
                                                          :worker-data params))
           (resp/content-type "text/html; charset=utf-8"))))))

(defn edit-worker-form
  ([id] (edit-worker-form id nil))
  ([id-or-request request-or-params]
   (let [id (if (number? id-or-request) id-or-request
              (extract-id-from-request id-or-request))
         params (extract-params (or request-or-params id-or-request))]
     (let [worker-id (validate-id id)]
       (if (nil? worker-id)
         (bad-request "Некорректный идентификатор работника")
         (do
           (logger/log-info (format "Открыта форма редактирования работника ID=%s" worker-id))
           (let [worker (model/get-record-by-id "Работник" (str worker-id))
                 цеха (model/get-spravochnik "Цех")
                 системы_оплаты (model/get-spravochnik "Система_оплаты")
                 категории (model/get-spravochnik "Категория_работника")
                 разряды (model/get-spravochnik "Разряд")
                 режимы (model/get-spravochnik "Режим_работы")
                 оклады (model/get-spravochnik "Оклад")
                 ставки (model/get-spravochnik "Почасовые_ставки")
                 errors (when-let [err-str (:errors params)]
                          (clojure.string/split err-str #","))]
             (if worker
               (-> (resp/response (workers/render-edit-worker-page worker цеха системы_оплаты категории разряды режимы оклады ставки
                                                                    :errors errors))
                   (resp/content-type "text/html; charset=utf-8"))
               (-> (resp/response "Работник не найден")
                   (resp/status 404)
                   (resp/content-type "text/html; charset=utf-8"))))))))))

(defn create-worker
  ([] (create-worker nil))
  ([request-or-params]
   (let [params (extract-params request-or-params)]
     (logger/log-info "Попытка создания работника")
     (let [validation-result (validation/validate-worker params)]
       (if (:valid? validation-result)
         (try
           (let [data {:фамилия (:фамилия params)
                       :имя (:имя params)
                       :отчество (:отчество params)
                       :дата_приема (:дата_приема params)
                       :цех_id (parse-int (:цех_id params) 0)
                       :система_оплаты_id (parse-int (:система_оплаты_id params) 0)
                       :категория_работника_id (parse-int (:категория_работника_id params) 0)
                       :разряд_id (parse-int (:разряд_id params) 0)
                       :режим_работы_id (parse-int (:режим_работы_id params) 0)
                       :оклад_id (when (seq (:оклад_id params)) (parse-int (:оклад_id params) nil))
                       :почасовая_ставка_id (when (seq (:почасовая_ставка_id params)) (parse-int (:почасовая_ставка_id params) nil))}
                 result (model/create-record "Работник" data)]
             (if (:success result)
               (do
                 (logger/log-audit "CREATE" "Worker" (:id result)
                                   (format "Создан работник %s %s" (:фамилия params) (:имя params)))
                 (logger/log-info (format "Работник успешно создан, ID=%s" (:id result)))
                 (resp/redirect "/workers"))
               (do
                 (logger/log-error ^Throwable (Exception. (:message result)) "Ошибка при создании работника")
                 (let [цеха (model/get-spravochnik "Цех")
                       системы_оплаты (model/get-spravochnik "Система_оплаты")
                       категории (model/get-spravochnik "Категория_работника")
                       разряды (model/get-spravochnik "Разряд")
                       режимы (model/get-spravochnik "Режим_работы")
                       оклады (model/get-spravochnik "Оклад")
                       ставки (model/get-spravochnik "Почасовые_ставки")]
                   (-> (resp/response (workers/render-new-worker-page цеха системы_оплаты категории разряды режимы оклады ставки
                                                                      :errors [(:message result)]
                                                                      :worker-data params))
                       (resp/content-type "text/html; charset=utf-8"))))))
           (catch Exception e
             (logger/log-error ^Throwable e "Критическая ошибка при создании работника")
             (let [цеха (model/get-spravochnik "Цех")
                   системы_оплаты (model/get-spravochnik "Система_оплаты")
                   категории (model/get-spravochnik "Категория_работника")
                   разряды (model/get-spravochnik "Разряд")
                   режимы (model/get-spravochnik "Режим_работы")
                   оклады (model/get-spravochnik "Оклад")
                   ставки (model/get-spravochnik "Почасовые_ставки")]
               (-> (resp/response (workers/render-new-worker-page цеха системы_оплаты категории разряды режимы оклады ставки
                                                                  :errors ["Внутренняя ошибка при создании работника"]
                                                                  :worker-data params))
                   (resp/content-type "text/html; charset=utf-8")))))
         (do
           (logger/log-warn (format "Валидация не пройдена: %s" (clojure.string/join ", " (:errors validation-result))))
           (let [цеха (model/get-spravochnik "Цех")
                 системы_оплаты (model/get-spravochnik "Система_оплаты")
                 категории (model/get-spravochnik "Категория_работника")
                 разряды (model/get-spravochnik "Разряд")
                 режимы (model/get-spravochnik "Режим_работы")
                 оклады (model/get-spravochnik "Оклад")
                 ставки (model/get-spravochnik "Почасовые_ставки")]
             (-> (resp/response (workers/render-new-worker-page цеха системы_оплаты категории разряды режимы оклады ставки
                                                                :errors (:errors validation-result)
                                                                :worker-data params))
                 (resp/content-type "text/html; charset=utf-8")))))))))

(defn update-worker
  ([id] (update-worker id nil))
  ([id-or-request request-or-params]
   (let [id (if (number? id-or-request) id-or-request
              (extract-id-from-request id-or-request))
         params (extract-params (or request-or-params id-or-request))]
     (let [worker-id (validate-id id)]
       (if (nil? worker-id)
         (bad-request "Некорректный идентификатор работника")
         (do
           (logger/log-info (format "Попытка обновления работника ID=%s" worker-id))
           (let [validation-result (validation/validate-worker-update params)]
             (if (:valid? validation-result)
               (let [data {:фамилия (:фамилия params)
                           :имя (:имя params)
                           :отчество (:отчество params)
                           :дата_приема (:дата_приема params)
                           :цех_id (parse-int (:цех_id params) nil)
                           :система_оплаты_id (parse-int (:система_оплаты_id params) nil)
                           :категория_работника_id (parse-int (:категория_работника_id params) nil)
                           :разряд_id (parse-int (:разряд_id params) nil)
                           :режим_работы_id (parse-int (:режим_работы_id params) nil)
                           :оклад_id (when (seq (:оклад_id params)) (parse-int (:оклад_id params) nil))
                           :почасовая_ставка_id (when (seq (:почасовая_ставка_id params)) (parse-int (:почасовая_ставка_id params) nil))}
                     result (model/update-record "Работник" worker-id data)]
                 (if (:success result)
                   (do
                     (logger/log-audit "UPDATE" "Worker" worker-id
                                       (format "Обновлен работник %s %s" (:фамилия params) (:имя params)))
                     (logger/log-info (format "Работник успешно обновлен, ID=%s" worker-id))
                     (resp/redirect "/workers"))
                   (do
                     (logger/log-error ^Throwable (Exception. (:message result)) "Ошибка при обновлении работника")
                     (let [цеха (model/get-spravochnik "Цех")
                           системы_оплаты (model/get-spravochnik "Система_оплаты")
                           категории (model/get-spravochnik "Категория_работника")
                           разряды (model/get-spravochnik "Разряд")
                           режимы (model/get-spravochnik "Режим_работы")
                           оклады (model/get-spravochnik "Оклад")
                           ставки (model/get-spravochnik "Почасовые_ставки")
                           worker (model/get-record-by-id "Работник" (str worker-id))]
                       (-> (resp/response (workers/render-edit-worker-page worker цеха системы_оплаты категории разряды режимы оклады ставки
                                                                            :errors [(:message result)]))
                           (resp/content-type "text/html; charset=utf-8"))))))
               (do
                 (logger/log-warn (format "Валидация не пройдена: %s" (clojure.string/join ", " (:errors validation-result))))
                 (let [цеха (model/get-spravochnik "Цех")
                       системы_оплаты (model/get-spravochnik "Система_оплаты")
                       категории (model/get-spravochnik "Категория_работника")
                       разряды (model/get-spravochnik "Разряд")
                       режимы (model/get-spravochnik "Режим_работы")
                       оклады (model/get-spravochnik "Оклад")
                       ставки (model/get-spravochnik "Почасовые_ставки")
                       worker (merge (model/get-record-by-id "Работник" (str worker-id)) params)]
                   (-> (resp/response (workers/render-edit-worker-page worker цеха системы_оплаты категории разряды режимы оклады ставки
                                                                        :errors (:errors validation-result)))
                       (resp/content-type "text/html; charset=utf-8"))))))))))))

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
                 (resp/redirect "/workers"))
               (do
                 (logger/log-error ^Throwable (Exception. (:message result)) "Ошибка при удалении работника")
                 (resp/redirect "/workers"))))))))))

;; ======================================================================
;; Контроллеры зарплаты
;; ======================================================================

(defn worker-salary-page
  ([] (worker-salary-page nil))
  ([id-or-request]
   (let [id (if (number? id-or-request) id-or-request
              (extract-id-from-request id-or-request))]
     (let [worker-id (validate-id id)]
       (if (nil? worker-id)
         (bad-request "Некорректный идентификатор работника")
         (do
           (logger/log-info (format "Открыта страница зарплаты работника ID=%s" worker-id))
           (let [worker (model/get-record-by-id "Работник" (str worker-id))
                 [current-year current-month] (model/current-year-month)
                 salary-info (model/get-worker-salary worker-id current-year current-month)
                 salary-history (model/get-worker-salary-history worker-id)]
             (if worker
               (-> (resp/response (salary/render-salary-page worker salary-info salary-history))
                   (resp/content-type "text/html; charset=utf-8"))
               (resp/redirect "/workers")))))))))

;; ======================================================================
;; Контроллеры учета времени
;; ======================================================================

(defn worker-work-time-page
  ([] (worker-work-time-page nil))
  ([id-or-request]
   (let [id (if (number? id-or-request) id-or-request
              (extract-id-from-request id-or-request))]
     (let [worker-id (validate-id id)]
       (if (nil? worker-id)
         (bad-request "Некорректный идентификатор работника")
         (do
           (logger/log-info (format "Открыта страница учета времени работника ID=%s" worker-id))
           (let [worker (model/get-record-by-id "Работник" (str worker-id))
                 work-time-records (model/get-worker-work-time worker-id)]
             (if worker
               (-> (resp/response (work-time/render-work-time-page worker work-time-records))
                   (resp/content-type "text/html; charset=utf-8"))
               (resp/redirect "/workers")))))))))

(defn edit-work-time-form
  ([] (edit-work-time-form nil))
  ([id-or-request]
   (let [id (if (number? id-or-request) id-or-request
              (extract-id-from-request id-or-request))]
     (let [work-time-id (validate-id id)]
       (if (nil? work-time-id)
         (bad-request "Некорректный идентификатор записи")
         (do
           (logger/log-info (format "Открыта форма редактирования учета времени ID=%s" work-time-id))
           (let [work-time-record (model/get-work-time-by-id (str work-time-id))
                 worker (when work-time-record
                          (model/get-record-by-id "Работник" (:работник_id work-time-record)))]
             (if (and work-time-record worker)
               (-> (resp/response (work-time/render-edit-work-time-form work-time-record worker))
                   (resp/content-type "text/html; charset=utf-8"))
               (resp/redirect "/workers")))))))))

(defn update-work-time
  ([] (update-work-time nil nil))
  ([id-or-request request-or-params]
   (let [id (if (number? id-or-request) id-or-request
              (extract-id-from-request id-or-request))
         params (extract-params (or request-or-params id-or-request))]
     (let [work-time-id (validate-id id)]
       (if (nil? work-time-id)
         (bad-request "Некорректный идентификатор записи")
         (do
           (logger/log-info (format "Попытка обновления учета времени ID=%s" work-time-id))
           (let [validation-result (validation/validate-work-time params)]
             (if (:valid? validation-result)
               (let [data {:год (parse-int (:год params) nil)
                           :месяц (parse-int (:месяц params) nil)
                           :всего_часов_за_месяц_по_плану (parse-int (:всего_часов_за_месяц_по_плану params) nil)
                           :всего_часов_в_месяц_по_факту (parse-int (:всего_часов_в_месяц_по_факту params) nil)
                           :количество_отработанных_дней (when (seq (:количество_отработанных_дней params)) (parse-int (:количество_отработанных_дней params) nil))
                           :количество_рабочих_часов_в_день (when (seq (:количество_рабочих_часов_в_день params)) (parse-int (:количество_рабочих_часов_в_день params) nil))
                           :всего_отработанных_часов (when (seq (:всего_отработанных_часов params)) (parse-int (:всего_отработанных_часов params) nil))
                           :сколько_должны_отработать (when (seq (:сколько_должны_отработать params)) (parse-int (:сколько_должны_отработать params) nil))
                           :больничные_дни (parse-int (:больничные_дни params) 0)
                           :командировочные_дни (parse-int (:командировочные_дни params) 0)}
                  result (model/update-record "Учет_рабочего_времени" work-time-id data)]
                  (if (:success result)
                    (do
                      (let [work-time-record (model/get-work-time-by-id (str work-time-id))]
                        (logger/log-audit "UPDATE" "WorkTime" work-time-id
                                          (format "Обновлен учет времени для работника ID=%s" (:работник_id work-time-record)))
                        (logger/log-info (format "Учет времени успешно обновлен, ID=%s" work-time-id))
                        (resp/redirect (str "/workers/" (:работник_id work-time-record) "/work-time"))))
                   (do
                     (logger/log-error ^Throwable (Exception. (:message result)) "Ошибка при обновлении учета времени")
                     (let [work-time-record (model/get-work-time-by-id (str work-time-id))
                           worker (model/get-record-by-id "Работник" (:работник_id work-time-record))]
                       (-> (resp/response (work-time/render-edit-work-time-form work-time-record worker :errors [(:message result)]))
                           (resp/content-type "text/html; charset=utf-8"))))))
               (do
                 (logger/log-warn (format "Валидация учета времени не пройдена: %s" (clojure.string/join ", " (:errors validation-result))))
                 (let [work-time-record (merge (model/get-work-time-by-id (str work-time-id)) params)
                       worker (model/get-record-by-id "Работник" (:работник_id work-time-record))]
                   (-> (resp/response (work-time/render-edit-work-time-form work-time-record worker :errors (:errors validation-result)))
                       (resp/content-type "text/html; charset=utf-8"))))))))))))

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
                              :rows (model/get-table-data table-name)})
                           tables)]
     (-> (resp/response (tables/render-all-tables-page tables-data))
         (resp/content-type "text/html; charset=utf-8")))))

;; ======================================================================
;; Контроллер ошибок
;; ======================================================================

(defn not-found-page [request]
  (logger/log-warn "Страница не найдена")
  (-> (resp/response "Страница не найдена")
      (resp/status 404)
      (resp/content-type "text/html; charset=utf-8")))

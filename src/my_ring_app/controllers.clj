(ns my-ring-app.controllers
  (:require [ring.util.response :as resp]
            [clojure.string :as str]
            [my-ring-app.model :as model]
            [my-ring-app.views.layout :as layout]
            [my-ring-app.views.home :as home]
            [my-ring-app.views.workers :as workers]          ; ← для форм работников
            [my-ring-app.views.salary :as salary]            ; ← для страницы зарплаты
            [my-ring-app.views.work-time :as work-time]      ; ← для учета времени
            [my-ring-app.views.tables :as tables]            ; ← для всех таблиц
            [my-ring-app.validation :as validation]
            [my-ring-app.logger :as logger]))

;; ======================================================================
;; Контроллер главной страницы
;; ======================================================================

(defn home-page []
  (logger/log-info "Открыта главная страница")
  (-> (resp/response (home/render-home))
      (resp/content-type "text/html; charset=utf-8")))

;; ======================================================================
;; Контроллеры работников
;; ======================================================================

(defn workers-page [params]
  (let [query (:search params)
        workers (if (and query (not (str/blank? query)))
                  (model/search-workers query)
                  (model/get-workers-with-details))]
    (logger/log-info (format "Открыт список работников (поиск: %s, найдено: %d)"
                             (or query "-") (count workers)))
    (-> (resp/response (workers/render-workers-page workers query))
        (resp/content-type "text/html; charset=utf-8"))))

(defn new-worker-form [params]
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
        (resp/content-type "text/html; charset=utf-8"))))

(defn edit-worker-form [id params]
  (logger/log-info (format "Открыта форма редактирования работника ID=%s" id))
  (let [worker (model/get-record-by-id "Работник" id)
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
          (resp/content-type "text/html; charset=utf-8")))))

(defn create-worker [params]
  (logger/log-info "Попытка создания работника")
  (let [validation-result (validation/validate-worker params)]
    (if (:valid? validation-result)
      ;; Валидация прошла успешно
      (let [data {:фамилия (:фамилия params)
                  :имя (:имя params)
                  :отчество (:отчество params)
                  :дата_приема (:дата_приема params)
                  :цех_id (Integer/parseInt (:цех_id params))
                  :система_оплаты_id (Integer/parseInt (:система_оплаты_id params))
                  :категория_работника_id (Integer/parseInt (:категория_работника_id params))
                  :разряд_id (Integer/parseInt (:разряд_id params))
                  :режим_работы_id (Integer/parseInt (:режим_работы_id params))
                  :оклад_id (when (seq (:оклад_id params)) (Integer/parseInt (:оклад_id params)))
                  :почасовая_ставка_id (when (seq (:почасовая_ставка_id params)) (Integer/parseInt (:почасовая_ставка_id params)))}
            result (model/create-record "Работник" data)]
        (if (:success result)
          (do
            (logger/log-audit "CREATE" "Worker" (:id result)
                              (format "Создан работник %s %s" (:фамилия params) (:имя params)))
            (logger/log-info (format "Работник успешно создан, ID=%s" (:id result)))
            (resp/redirect "/workers"))
          (do
            (logger/log-error (Exception. (:message result)) "Ошибка при создании работника")
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
      ;; Валидация не прошла
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
              (resp/content-type "text/html; charset=utf-8")))))))

(defn update-worker [id params]
  (logger/log-info (format "Попытка обновления работника ID=%s" id))
  (let [validation-result (validation/validate-worker-update params)]
    (if (:valid? validation-result)
      ;; Валидация прошла успешно
      (let [data {:фамилия (:фамилия params)
                  :имя (:имя params)
                  :отчество (:отчество params)
                  :дата_приема (:дата_приема params)
                  :цех_id (Integer/parseInt (:цех_id params))
                  :система_оплаты_id (Integer/parseInt (:система_оплаты_id params))
                  :категория_работника_id (Integer/parseInt (:категория_работника_id params))
                  :разряд_id (Integer/parseInt (:разряд_id params))
                  :режим_работы_id (Integer/parseInt (:режим_работы_id params))
                  :оклад_id (when (seq (:оклад_id params)) (Integer/parseInt (:оклад_id params)))
                  :почасовая_ставка_id (when (seq (:почасовая_ставка_id params)) (Integer/parseInt (:почасовая_ставка_id params)))}
            result (model/update-record "Работник" (Integer/parseInt id) data)]
        (if (:success result)
          (do
            (logger/log-audit "UPDATE" "Worker" id
                              (format "Обновлен работник %s %s" (:фамилия params) (:имя params)))
            (logger/log-info (format "Работник успешно обновлен, ID=%s" id))
            (resp/redirect "/workers"))
          (do
            (logger/log-error (Exception. (:message result)) "Ошибка при обновлении работника")
            (let [цеха (model/get-spravochnik "Цех")
                  системы_оплаты (model/get-spravochnik "Система_оплаты")
                  категории (model/get-spravochnik "Категория_работника")
                  разряды (model/get-spravochnik "Разряд")
                  режимы (model/get-spravochnik "Режим_работы")
                  оклады (model/get-spravochnik "Оклад")
                  ставки (model/get-spravochnik "Почасовые_ставки")
                  worker (model/get-record-by-id "Работник" id)]
              (-> (resp/response (workers/render-edit-worker-page worker цеха системы_оплаты категории разряды режимы оклады ставки
                                                                   :errors [(:message result)]))
                  (resp/content-type "text/html; charset=utf-8"))))))
      ;; Валидация не прошла
      (do
        (logger/log-warn (format "Валидация не пройдена: %s" (clojure.string/join ", " (:errors validation-result))))
        (let [цеха (model/get-spravochnik "Цех")
              системы_оплаты (model/get-spravochnik "Система_оплаты")
              категории (model/get-spravochnik "Категория_работника")
              разряды (model/get-spravochnik "Разряд")
              режимы (model/get-spravochnik "Режим_работы")
              оклады (model/get-spravochnik "Оклад")
              ставки (model/get-spravochnik "Почасовые_ставки")
              worker (merge (model/get-record-by-id "Работник" id) params)]
          (-> (resp/response (workers/render-edit-worker-page worker цеха системы_оплаты категории разряды режимы оклады ставки
                                                               :errors (:errors validation-result)))
              (resp/content-type "text/html; charset=utf-8")))))))

(defn delete-worker [id]
  (logger/log-info (format "Попытка удаления работника ID=%s" id))
  (let [result (model/delete-record "Работник" (Integer/parseInt id))]
    (if (:success result)
      (do
        (logger/log-audit "DELETE" "Worker" id "Работник удален")
        (logger/log-info (format "Работник успешно удален, ID=%s" id))
        (resp/redirect "/workers"))
      (do
        (logger/log-error (Exception. (:message result)) "Ошибка при удалении работника")
        (resp/redirect "/workers")))))

;; ======================================================================
;; Контроллеры зарплаты
;; ======================================================================

(defn worker-salary-page [id]
  (logger/log-info (format "Открыта страница зарплаты работника ID=%s" id))
  (let [worker (model/get-record-by-id "Работник" id)
        salary-info (model/get-worker-salary (Integer/parseInt id) 2025 10)
        salary-history (model/get-worker-salary-history (Integer/parseInt id))]
    (if worker
      (-> (resp/response (salary/render-salary-page worker salary-info salary-history))
          (resp/content-type "text/html; charset=utf-8"))
      (resp/redirect "/workers"))))

;; ======================================================================
;; Контроллеры учета времени
;; ======================================================================

(defn worker-work-time-page [id]
  (logger/log-info (format "Открыта страница учета времени работника ID=%s" id))
  (let [worker (model/get-record-by-id "Работник" id)
        work-time-records (model/get-worker-work-time (Integer/parseInt id))]
    (if worker
      (-> (resp/response (work-time/render-work-time-page worker work-time-records))
          (resp/content-type "text/html; charset=utf-8"))
      (resp/redirect "/workers"))))

(defn edit-work-time-form [id]
  (logger/log-info (format "Открыта форма редактирования учета времени ID=%s" id))
  (let [work-time-record (model/get-work-time-by-id id)
        worker (when work-time-record
                 (model/get-record-by-id "Работник" (:работник_id work-time-record)))]
    (if (and work-time-record worker)
      (-> (resp/response (work-time/render-edit-work-time-form work-time-record worker))
          (resp/content-type "text/html; charset=utf-8"))
      (resp/redirect "/workers"))))

(defn update-work-time [id params]
  (logger/log-info (format "Попытка обновления учета времени ID=%s" id))
  (let [validation-result (validation/validate-work-time params)]
    (if (:valid? validation-result)
      (let [data {:год (Integer/parseInt (:год params))
                  :месяц (Integer/parseInt (:месяц params))
                  :всего_часов_за_месяц_по_плану (Integer/parseInt (:всего_часов_за_месяц_по_плану params))
                  :всего_часов_в_месяц_по_факту (Integer/parseInt (:всего_часов_в_месяц_по_факту params))
                  :количество_отработанных_дней (when (seq (:количество_отработанных_дней params)) (Integer/parseInt (:количество_отработанных_дней params)))
                  :количество_рабочих_часов_в_день (when (seq (:количество_рабочих_часов_в_день params)) (Integer/parseInt (:количество_рабочих_часов_в_день params)))
                  :всего_отработанных_часов (when (seq (:всего_отработанных_часов params)) (Integer/parseInt (:всего_отработанных_часов params)))
                  :сколько_должны_отработать (when (seq (:сколько_должны_отработать params)) (Integer/parseInt (:сколько_должны_отработать params)))
                  :больничные_дни (Integer/parseInt (or (:больничные_дни params) "0"))
                  :командировочные_дни (Integer/parseInt (or (:командировочные_дни params) "0"))}
            result (model/update-record "Учет_рабочего_времени" (Integer/parseInt id) data)]
        (if (:success result)
          (do
            (logger/log-audit "UPDATE" "WorkTime" id
                              (format "Обновлен учет времени для работника ID=%s" (:работник_id (model/get-work-time-by-id id))))
            (logger/log-info (format "Учет времени успешно обновлен, ID=%s" id))
            (resp/redirect (str "/workers/" (:работник_id (model/get-work-time-by-id id)) "/work-time")))
          (do
            (logger/log-error (Exception. (:message result)) "Ошибка при обновлении учета времени")
            (let [work-time-record (model/get-work-time-by-id id)
                  worker (model/get-record-by-id "Работник" (:работник_id work-time-record))]
              (-> (resp/response (work-time/render-edit-work-time-form work-time-record worker :errors [(:message result)]))
                  (resp/content-type "text/html; charset=utf-8"))))))
      ;; Валидация не прошла
      (do
        (logger/log-warn (format "Валидация учета времени не пройдена: %s" (clojure.string/join ", " (:errors validation-result))))
        (let [work-time-record (merge (model/get-work-time-by-id id) params)
              worker (model/get-record-by-id "Работник" (:работник_id work-time-record))]
          (-> (resp/response (work-time/render-edit-work-time-form work-time-record worker :errors (:errors validation-result)))
              (resp/content-type "text/html; charset=utf-8")))))))

;; ======================================================================
;; Контроллеры БД
;; ======================================================================

(defn all-tables-page []
  (logger/log-info "Открыта страница просмотра всех таблиц")
  (let [tables (model/get-tables)
        tables-data (mapv (fn [table]
                            {:table table
                             :rows (model/get-table-data table)})
                          tables)]
    (-> (resp/response (tables/render-all-tables-page tables-data))
        (resp/content-type "text/html; charset=utf-8"))))

;; ======================================================================
;; Контроллер ошибок
;; ======================================================================

(defn not-found-page [request]
  (logger/log-warn "Страница не найдена")
  (-> (resp/response "Страница не найдена")
      (resp/status 404)
      (resp/content-type "text/html; charset=utf-8")))
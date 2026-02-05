(ns my-ring-app.core
  (:require [ring.adapter.jetty :as jetty]
            [ring.util.response :as resp]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [compojure.core :refer [defroutes GET POST DELETE]]
            [compojure.route :as route]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            [my-ring-app.views :as views]
            [my-ring-app.validation :as validation]))

;; Подключение к SQLite БД
(def db-spec
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname "igra.db"})

;; ======================================================================
;; Бизнес-логика (Model)
;; ======================================================================

;; Получение списка таблиц
(defn get-tables []
  (try
    (let [tables (jdbc/query db-spec ["SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name"])]
      (map :name tables))
    (catch Exception e
      (println "Ошибка при получении таблиц:" (.getMessage e))
      [])))

;; Получение данных из таблицы
(defn get-table-data [table-name]
  (try
    (jdbc/query db-spec [(str "SELECT * FROM \"" table-name "\"")])
    (catch Exception e
      (println "Ошибка при запросе к таблице" table-name ":" (.getMessage e))
      [])))

;; Получение одной записи по ID
(defn get-record-by-id [table-name id]
  (try
    (first (jdbc/query db-spec [(str "SELECT * FROM \"" table-name "\" WHERE id = ?") id]))
    (catch Exception e
      (println "Ошибка при получении записи:" (.getMessage e))
      nil)))

;; Создание записи
(defn create-record [table-name data]
  (try
    (jdbc/insert! db-spec table-name data)
    {:success true :message "Запись успешно создана"}
    (catch Exception e
      {:success false :message (str "Ошибка при создании: " (.getMessage e))})))

;; Обновление записи
(defn update-record [table-name id data]
  (try
    (jdbc/update! db-spec table-name data ["id = ?" id])
    {:success true :message "Запись успешно обновлена"}
    (catch Exception e
      {:success false :message (str "Ошибка при обновлении: " (.getMessage e))})))

;; Удаление записи
(defn delete-record [table-name id]
  (try
    (jdbc/delete! db-spec table-name ["id = ?" id])
    {:success true :message "Запись успешно удалена"}
    (catch Exception e
      {:success false :message (str "Ошибка при удалении: " (.getMessage e))})))

;; Получение справочника для выпадающего списка
(defn get-spravochnik [table-name]
  (try
    (jdbc/query db-spec [(str "SELECT * FROM \"" table-name "\"")])
    (catch Exception e
      (println "Ошибка при получении справочника" table-name ":" (.getMessage e))
      [])))

;; Получение расширенных данных работников с именами справочников
(defn get-workers-with-details []
  (try
    (jdbc/query db-spec 
      ["SELECT r.id, r.фамилия, r.имя, r.отчество, r.дата_приема,
               ц.название_цеха as цех,
               с.название_системы as система,
               к.название_категории as категория,
               рз.номер_разряда as разряд,
               рм.название_режима as режим
        FROM Работник r
        LEFT JOIN Цех ц ON r.цех_id = ц.id
        LEFT JOIN Система_оплаты с ON r.система_оплаты_id = с.id
        LEFT JOIN Категория_работника к ON r.категория_работника_id = к.id
        LEFT JOIN Разряд рз ON r.разряд_id = рз.id
        LEFT JOIN Режим_работы рм ON r.режим_работы_id = рм.id
        ORDER BY r.фамилия, r.имя"])
    (catch Exception e
      (println "Ошибка при получении работников:" (.getMessage e))
      [])))

;; Функция поиска работников
(defn search-workers [query]
  "Поиск работников по ФИО (регистронезависимый)"
  (try
    (let [search-term (str "%" query "%")]
      (jdbc/query db-spec 
        ["SELECT r.id, r.фамилия, r.имя, r.отчество, r.дата_приема,
                 ц.название_цеха as цех,
                 с.название_системы as система,
                 к.название_категории as категория,
                 рз.номер_разряда as разряд,
                 рм.название_режима as режим
          FROM Работник r
          LEFT JOIN Цех ц ON r.цех_id = ц.id
          LEFT JOIN Система_оплаты с ON r.система_оплаты_id = с.id
          LEFT JOIN Категория_работника к ON r.категория_работника_id = к.id
          LEFT JOIN Разряд рз ON r.разряд_id = рз.id
          LEFT JOIN Режим_работы рм ON r.режим_работы_id = рм.id
          WHERE LOWER(r.фамилия) LIKE LOWER(?) 
             OR LOWER(r.имя) LIKE LOWER(?) 
             OR LOWER(r.отчество) LIKE LOWER(?)
             OR LOWER(ц.название_цеха) LIKE LOWER(?)
          ORDER BY r.фамилия, r.имя"
         search-term search-term search-term search-term]))
    (catch Exception e
      (println "Ошибка при поиске:" (.getMessage e))
      [])))

;; Получение информации о зарплате работника за период
(defn get-worker-salary [worker-id year month]
  "Получение информации о зарплате работника за период"
  (try
    (first (jdbc/query db-spec 
      ["SELECT r.id, r.фамилия, r.имя, r.отчество, ц.название_цеха,
               с.название_системы, у.год, у.месяц,
               у.всего_отработанных_часов,
               у.больничные_дни, у.командировочные_дни,
               н.общая_зарплата,
               н.зарплата_за_больничные_дни,
               н.зарплата_за_командировочные_дни,
               о.оклад_в_месяц,
               п.ставка_в_час
        FROM Работник r
        LEFT JOIN Цех ц ON r.цех_id = ц.id
        LEFT JOIN Система_оплаты с ON r.система_оплаты_id = с.id
        LEFT JOIN Учет_рабочего_времени у ON r.id = у.работник_id
        LEFT JOIN Начисление_заработной_платы н ON у.id = н.учет_рабочего_времени_id
        LEFT JOIN Оклад о ON r.оклад_id = о.id
        LEFT JOIN Почасовые_ставки п ON r.почасовая_ставка_id = п.id
        WHERE r.id = ? AND у.год = ? AND у.месяц = ?"
       worker-id year month]))
    (catch Exception e
      (println "Ошибка при получении зарплаты:" (.getMessage e))
      nil)))

;; Получение всей истории зарплат работника
(defn get-worker-salary-history [worker-id]
  "Получение всей истории зарплат работника"
  (try
    (jdbc/query db-spec 
      ["SELECT r.фамилия, r.имя, у.год, у.месяц,
               н.общая_зарплата,
               н.зарплата_за_больничные_дни,
               н.зарплата_за_командировочные_дни,
               у.больничные_дни,
               у.командировочные_дни
        FROM Работник r
        LEFT JOIN Учет_рабочего_времени у ON r.id = у.работник_id
        LEFT JOIN Начисление_заработной_платы н ON у.id = н.учет_рабочего_времени_id
        WHERE r.id = ?
        ORDER BY у.год DESC, у.месяц DESC"
       worker-id])
    (catch Exception e
      (println "Ошибка при получении истории зарплаты:" (.getMessage e))
      [])))

;; НОВАЯ ФУНКЦИЯ: Получение учета рабочего времени работника
(defn get-worker-work-time [worker-id]
  "Получение всех записей учета рабочего времени для работника"
  (try
    (jdbc/query db-spec 
      ["SELECT у.id, у.год, у.месяц,
               у.всего_часов_за_месяц_по_плану,
               у.всего_часов_в_месяц_по_факту,
               у.количество_отработанных_дней,
               у.количество_рабочих_часов_в_день,
               у.всего_отработанных_часов,
               у.сколько_должны_отработать,
               у.больничные_дни,
               у.командировочные_дни
        FROM Учет_рабочего_времени у
        WHERE у.работник_id = ?
        ORDER BY у.год DESC, у.месяц DESC"
       worker-id])
    (catch Exception e
      (println "Ошибка при получении учета рабочего времени:" (.getMessage e))
      [])))

;; НОВАЯ ФУНКЦИЯ: Получение одной записи учета рабочего времени
(defn get-work-time-by-id [id]
  "Получение одной записи учета рабочего времени по ID"
  (try
    (first (jdbc/query db-spec 
      ["SELECT * FROM Учет_рабочего_времени WHERE id = ?" id]))
    (catch Exception e
      (println "Ошибка при получении записи учета времени:" (.getMessage e))
      nil)))

;; ======================================================================
;; Контроллеры (Controller)
;; ======================================================================

(defroutes app-routes
  ;; Главная страница
  (GET "/" []
    (-> (resp/response (views/render-home))
        (resp/content-type "text/html; charset=utf-8")))
  
  ;; Список работников с поиском
  (GET "/workers" request
    (let [params (:params request)
          query (:search params)
          workers (if (and query (not (str/blank? query)))
                    (search-workers query)
                    (get-workers-with-details))]
      (-> (resp/response (views/render-workers-page workers query))
          (resp/content-type "text/html; charset=utf-8"))))
  
  ;; Форма создания работника
  (GET "/workers/new" request
    (let [params (:params request)
          цеха (get-spravochnik "Цех")
          системы_оплаты (get-spravochnik "Система_оплаты")
          категории (get-spravochnik "Категория_работника")
          разряды (get-spravochnik "Разряд")
          режимы (get-spravochnik "Режим_работы")
          оклады (get-spravochnik "Оклад")
          ставки (get-spravochnik "Почасовые_ставки")
          errors (when-let [err-str (:errors params)]
                   (clojure.string/split err-str #","))]
      (-> (resp/response (views/render-new-worker-page цеха системы_оплаты категории разряды режимы оклады ставки 
                                                       :errors errors
                                                       :worker-data params))
          (resp/content-type "text/html; charset=utf-8"))))
  
  ;; Форма редактирования работника
  (GET "/workers/:id/edit" [id :as request]
    (let [params (:params request)
          worker (get-record-by-id "Работник" id)
          цеха (get-spravochnik "Цех")
          системы_оплаты (get-spravochnik "Система_оплаты")
          категории (get-spravochnik "Категория_работника")
          разряды (get-spravochnik "Разряд")
          режимы (get-spravochnik "Режим_работы")
          оклады (get-spravochnik "Оклад")
          ставки (get-spravochnik "Почасовые_ставки")
          errors (when-let [err-str (:errors params)]
                   (clojure.string/split err-str #","))]
      (if worker
        (-> (resp/response (views/render-edit-worker-page worker цеха системы_оплаты категории разряды режимы оклады ставки
                                                           :errors errors))
            (resp/content-type "text/html; charset=utf-8"))
        (-> (resp/response "Работник не найден")
            (resp/status 404)
            (resp/content-type "text/html; charset=utf-8")))))
  
  ;; Создание работника - С ВАЛИДАЦИЕЙ
  (POST "/workers/create" request
    (let [params (:params request)
          validation-result (validation/validate-worker params)]
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
              result (create-record "Работник" data)]
          (if (:success result)
            (resp/redirect "/workers")
            (let [цеха (get-spravochnik "Цех")
                  системы_оплаты (get-spravochnik "Система_оплаты")
                  категории (get-spravochnik "Категория_работника")
                  разряды (get-spravochnik "Разряд")
                  режимы (get-spravochnik "Режим_работы")
                  оклады (get-spravochnik "Оклад")
                  ставки (get-spravochnik "Почасовые_ставки")]
              (-> (resp/response (views/render-new-worker-page цеха системы_оплаты категории разряды режимы оклады ставки
                                                               :errors [(:message result)]
                                                               :worker-data params))
                  (resp/content-type "text/html; charset=utf-8")))))
        ;; Валидация не прошла
        (let [цеха (get-spravochnik "Цех")
              системы_оплаты (get-spravochnik "Система_оплаты")
              категории (get-spravochnik "Категория_работника")
              разряды (get-spravochnik "Разряд")
              режимы (get-spravochnik "Режим_работы")
              оклады (get-spravochnik "Оклад")
              ставки (get-spravochnik "Почасовые_ставки")]
          (-> (resp/response (views/render-new-worker-page цеха системы_оплаты категории разряды режимы оклады ставки
                                                           :errors (:errors validation-result)
                                                           :worker-data params))
              (resp/content-type "text/html; charset=utf-8"))))))
  
  ;; Обновление работника - С ВАЛИДАЦИЕЙ
  (POST "/workers/:id/update" [id :as request]
    (let [params (:params request)
          validation-result (validation/validate-worker-update params)]
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
              result (update-record "Работник" (Integer/parseInt id) data)]
          (if (:success result)
            (resp/redirect "/workers")
            (let [цеха (get-spravochnik "Цех")
                  системы_оплаты (get-spravochnik "Система_оплаты")
                  категории (get-spravochnik "Категория_работника")
                  разряды (get-spravochnik "Разряд")
                  режимы (get-spravochnik "Режим_работы")
                  оклады (get-spravochnik "Оклад")
                  ставки (get-spravochnik "Почасовые_ставки")
                  worker (get-record-by-id "Работник" id)]
              (-> (resp/response (views/render-edit-worker-page worker цеха системы_оплаты категории разряды режимы оклады ставки
                                                               :errors [(:message result)]))
                  (resp/content-type "text/html; charset=utf-8")))))
        ;; Валидация не прошла
        (let [цеха (get-spravochnik "Цех")
              системы_оплаты (get-spravochnik "Система_оплаты")
              категории (get-spravochnik "Категория_работника")
              разряды (get-spravochnik "Разряд")
              режимы (get-spravochnik "Режим_работы")
              оклады (get-spravochnik "Оклад")
              ставки (get-spravochnik "Почасовые_ставки")
              worker (merge (get-record-by-id "Работник" id) params)]
          (-> (resp/response (views/render-edit-worker-page worker цеха системы_оплаты категории разряды режимы оклады ставки
                                                           :errors (:errors validation-result)))
              (resp/content-type "text/html; charset=utf-8"))))))
  
  ;; Удаление работника
  (POST "/workers/:id/delete" [id]
    (let [result (delete-record "Работник" (Integer/parseInt id))]
      (resp/redirect "/workers")))
  
  ;; Страница зарплаты работника
  (GET "/workers/:id/salary" [id]
    (let [worker (get-record-by-id "Работник" id)
          salary-info (get-worker-salary (Integer/parseInt id) 2025 10)
          salary-history (get-worker-salary-history (Integer/parseInt id))]
      (if worker
        (-> (resp/response (views/render-salary-page worker salary-info salary-history))
            (resp/content-type "text/html; charset=utf-8"))
        (resp/redirect "/workers"))))
  
  ;; НОВЫЙ РОУТ: Страница учета рабочего времени
  (GET "/workers/:id/work-time" [id]
    (let [worker (get-record-by-id "Работник" id)
          work-time-records (get-worker-work-time (Integer/parseInt id))]
      (if worker
        (-> (resp/response (views/render-work-time-page worker work-time-records))
            (resp/content-type "text/html; charset=utf-8"))
        (resp/redirect "/workers"))))
  
  ;; НОВЫЙ РОУТ: Форма редактирования записи учета времени
  (GET "/work-time/:id/edit" [id]
    (let [work-time-record (get-work-time-by-id id)
          worker (when work-time-record
                   (get-record-by-id "Работник" (:работник_id work-time-record)))]
      (if (and work-time-record worker)
        (-> (resp/response (views/render-edit-work-time-form work-time-record worker))
            (resp/content-type "text/html; charset=utf-8"))
        (resp/redirect "/workers"))))
  
  ;; НОВЫЙ РОУТ: Обновление записи учета времени
  (POST "/work-time/:id/update" [id :as request]
    (let [params (:params request)
          validation-result (validation/validate-work-time params)]
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
              result (update-record "Учет_рабочего_времени" (Integer/parseInt id) data)]
          (if (:success result)
            (resp/redirect (str "/workers/" (:работник_id (get-work-time-by-id id)) "/work-time"))
            (let [work-time-record (get-work-time-by-id id)
                  worker (get-record-by-id "Работник" (:работник_id work-time-record))]
              (-> (resp/response (views/render-edit-work-time-form work-time-record worker :errors [(:message result)]))
                  (resp/content-type "text/html; charset=utf-8")))))
        ;; Валидация не прошла
        (let [work-time-record (merge (get-work-time-by-id id) params)
              worker (get-record-by-id "Работник" (:работник_id work-time-record))]
          (-> (resp/response (views/render-edit-work-time-form work-time-record worker :errors (:errors validation-result)))
              (resp/content-type "text/html; charset=utf-8"))))))
  
  ;; Просмотр всех таблиц
  (GET "/db" []
    (let [tables (get-tables)
          tables-data (mapv (fn [table]
                              {:table table
                               :rows (get-table-data table)})
                            tables)]
      (-> (resp/response (views/render-all-tables-page tables-data))
          (resp/content-type "text/html; charset=utf-8"))))
  
  (route/not-found
    (-> (resp/response "Страница не найдена")
        (resp/status 404)
        (resp/content-type "text/html; charset=utf-8"))))

;; Middleware для обработки параметров
(def app
  (-> app-routes
      wrap-keyword-params
      wrap-params
      (wrap-json-body {:keywords? true})
      wrap-json-response))

(defn -main
  "Точка входа приложения"
  [& args]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (println "Сервер запускается на порту" port)
    (jetty/run-jetty app {:port port})))
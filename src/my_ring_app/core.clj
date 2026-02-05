(ns my-ring-app.core
  (:require [ring.adapter.jetty :as jetty]
            [ring.util.response :as resp]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [compojure.core :refer [defroutes GET POST DELETE]]
            [compojure.route :as route]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]  ;; ДОБАВЛЕНО: импорт для работы со строками
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

;; НОВАЯ ФУНКЦИЯ: Поиск работников
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
          ;; Если есть ошибки из параметров, передаем их
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
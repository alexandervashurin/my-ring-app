(ns my-ring-app.core
  (:require [ring.adapter.jetty :as jetty]
            [ring.util.response :as resp]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [compojure.core :refer [defroutes GET POST DELETE]]
            [compojure.route :as route]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]))

;; Подключение к SQLite БД
(def db-spec
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname "igra.db"})

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

;; Вспомогательная функция для генерации опций выпадающего списка
(defn- option-tag [item value-key label-key selected-value]
  (let [item-id (str (value-key item))
        item-label (str (label-key item))
        selected (if (= item-id (str selected-value)) " selected" "")]
    (str "<option value='" item-id "'" selected ">" item-label "</option>")))

;; Генерация формы для создания/редактирования работника
(defn generate-worker-form [mode worker-data]
  (let [цеха (get-spravochnik "Цех")
        системы_оплаты (get-spravochnik "Система_оплаты")
        категории (get-spravochnik "Категория_работника")
        разряды (get-spravochnik "Разряд")
        режимы (get-spravochnik "Режим_работы")
        оклады (get-spravochnik "Оклад")
        ставки (get-spravochnik "Почасовые_ставки")
        цех-id (:цех_id worker-data)
        система-id (:система_оплаты_id worker-data)
        категория-id (:категория_работника_id worker-data)
        разряд-id (:разряд_id worker-data)
        режим-id (:режим_работы_id worker-data)
        оклад-id (:оклад_id worker-data)
        ставка-id (:почасовая_ставка_id worker-data)]
    
    (str "<div class='form-container'>"
         "<h2>" (if (= mode :create) "➕ Добавить работника" "✏️ Редактировать работника") "</h2>"
         
         "<form method='POST' action='/workers/" 
         (if (= mode :edit) (str (:id worker-data) "/update") "create") 
         "' style='background: white; padding: 25px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1);'>"
         
         ;; Фамилия
         "<div class='form-group'>"
         "<label>Фамилия *</label>"
         "<input type='text' name='фамилия' value='" (or (:фамилия worker-data) "") "' required>"
         "</div>"
         
         ;; Имя
         "<div class='form-group'>"
         "<label>Имя *</label>"
         "<input type='text' name='имя' value='" (or (:имя worker-data) "") "' required>"
         "</div>"
         
         ;; Отчество
         "<div class='form-group'>"
         "<label>Отчество</label>"
         "<input type='text' name='отчество' value='" (or (:отчество worker-data) "") "'>"
         "</div>"
         
         ;; Дата приема
         "<div class='form-group'>"
         "<label>Дата приема *</label>"
         "<input type='date' name='дата_приема' value='" (or (:дата_приема worker-data) "") "' required>"
         "</div>"
         
         ;; Цех
         "<div class='form-group'>"
         "<label>Цех *</label>"
         "<select name='цех_id' required>"
         "<option value=''>Выберите цех</option>"
         (apply str (map #(option-tag % :id :название_цеха цех-id) цеха))
         "</select>"
         "</div>"
         
         ;; Система оплаты
         "<div class='form-group'>"
         "<label>Система оплаты *</label>"
         "<select name='система_оплаты_id' required onchange='togglePaymentFields(this.value)'>"
         "<option value=''>Выберите систему</option>"
         (apply str (map #(option-tag % :id :название_системы система-id) системы_оплаты))
         "</select>"
         "</div>"
         
         ;; Категория работника
         "<div class='form-group'>"
         "<label>Категория работника *</label>"
         "<select name='категория_работника_id' required>"
         "<option value=''>Выберите категорию</option>"
         (apply str (map #(option-tag % :id :название_категории категория-id) категории))
         "</select>"
         "</div>"
         
         ;; Разряд
         "<div class='form-group'>"
         "<label>Разряд *</label>"
         "<select name='разряд_id' required>"
         "<option value=''>Выберите разряд</option>"
         (apply str (map #(option-tag % :id :номер_разряда разряд-id) разряды))
         "</select>"
         "</div>"
         
         ;; Режим работы
         "<div class='form-group'>"
         "<label>Режим работы *</label>"
         "<select name='режим_работы_id' required>"
         "<option value=''>Выберите режим</option>"
         (apply str (map #(option-tag % :id :название_режима режим-id) режимы))
         "</select>"
         "</div>"
         
         ;; Оклад (показывается только при системе оплаты "Оклад")
         "<div class='form-group payment-field' id='oklad-field' style='display: none;'>"
         "<label>Оклад</label>"
         "<select name='оклад_id'>"
         "<option value=''>Выберите оклад</option>"
         (apply str (map #(option-tag % :id :оклад_в_месяц оклад-id) оклады))
         "</select>"
         "</div>"
         
         ;; Почасовая ставка (показывается только при системе оплаты "Почасовая")
         "<div class='form-group payment-field' id='stavka-field' style='display: none;'>"
         "<label>Почасовая ставка</label>"
         "<select name='почасовая_ставка_id'>"
         "<option value=''>Выберите ставку</option>"
         (apply str (map #(option-tag % :id :ставка_в_час ставка-id) ставки))
         "</select>"
         "</div>"
         
         ;; Кнопки
         "<div style='display: flex; gap: 10px; margin-top: 20px;'>"
         "<button type='submit' class='btn btn-primary'>"
         (if (= mode :create) "Создать" "Сохранить")
         "</button>"
         "<a href='/workers' class='btn btn-secondary'>Отмена</a>"
         "</div>"
         
         "</form>"
         "</div>"

         "<script>
           function togglePaymentFields(systemId) {
             const okladField = document.getElementById('oklad-field');
             const stavkaField = document.getElementById('stavka-field');
             
             if (systemId == '1') {
               okladField.style.display = 'block';
               stavkaField.style.display = 'none';
             } else if (systemId == '2') {
               okladField.style.display = 'none';
               stavkaField.style.display = 'block';
             } else {
               okladField.style.display = 'none';
               stavkaField.style.display = 'none';
             }
           }
           
           window.onload = function() {
             const systemSelect = document.querySelector('select[name=\"система_оплаты_id\"]');
             if (systemSelect && systemSelect.value) {
               togglePaymentFields(systemSelect.value);
             }
           };
         </script>")))

;; Генерация таблицы работников
(defn generate-workers-table [workers]
  (str "<div class='table-container'>"
       "<div style='display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;'>"
       "<h2>📋 Список работников</h2>"
       "<a href='/workers/new' class='btn btn-success'>➕ Добавить работника</a>"
       "</div>"
       
       (if (empty? workers)
         "<div class='empty-state'>Нет работников в базе данных</div>"
         (str "<table class='data-table'>"
              "<thead>"
              "<tr>"
              "<th>ID</th>"
              "<th>ФИО</th>"
              "<th>Дата приема</th>"
              "<th>Цех</th>"
              "<th>Система оплаты</th>"
              "<th>Категория</th>"
              "<th>Разряд</th>"
              "<th>Режим работы</th>"
              "<th>Действия</th>"
              "</tr>"
              "</thead>"
              "<tbody>"
              (apply str 
                (map (fn [w]
                       (str "<tr>"
                            "<td>" (:id w) "</td>"
                            "<td><strong>" (:фамилия w) "</strong> " (:имя w) " " (or (:отчество w) "") "</td>"
                            "<td>" (:дата_приема w) "</td>"
                            "<td>" (:цех w) "</td>"
                            "<td>" (:система w) "</td>"
                            "<td>" (:категория w) "</td>"
                            "<td>" (:разряд w) "</td>"
                            "<td>" (:режим w) "</td>"
                            "<td>"
                            "<a href='/workers/" (:id w) "/edit' class='btn btn-sm btn-warning'>✏️</a> "
                            "<form method='POST' action='/workers/" (:id w) "/delete' style='display: inline;' onsubmit='return confirm(\"Удалить работника " (:фамилия w) " " (:имя w) "?\")'>"
                            "<button type='submit' class='btn btn-sm btn-danger'>🗑️</button>"
                            "</form>"
                            "</td>"
                            "</tr>"))
                     workers))
              "</tbody>"
              "</table>"))
       "</div>"))

;; Генерация полной страницы
(defn generate-page [content title]
  (str "<!DOCTYPE html>"
       "<html lang='ru'>"
       "<head>"
       "<meta charset='UTF-8'>"
       "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
       "<title>" title " - Управление БД работников</title>"
       "<style>"
       "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background: #f5f7fa; color: #333; }"
       ".container { max-width: 1400px; margin: 0 auto; }"
       "header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 10px; margin-bottom: 30px; box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3); }"
       "header h1 { margin: 0; font-size: 28px; }"
       "nav { background: white; padding: 15px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }"
       "nav a { display: inline-block; margin: 0 10px; padding: 10px 15px; text-decoration: none; color: #667eea; border-radius: 5px; transition: all 0.3s; }"
       "nav a:hover { background: #f0f4ff; }"
       "nav a.active { background: #667eea; color: white; }"
       ".form-container, .table-container { background: white; padding: 25px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); margin-bottom: 20px; }"
       ".form-group { margin-bottom: 15px; }"
       ".form-group label { display: block; margin-bottom: 5px; font-weight: 600; color: #555; }"
       ".form-group input, .form-group select { width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 4px; font-size: 14px; }"
       ".form-group input:focus, .form-group select:focus { outline: none; border-color: #667eea; box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1); }"
       ".btn { padding: 10px 20px; border: none; border-radius: 5px; cursor: pointer; font-weight: 600; transition: all 0.3s; text-decoration: none; display: inline-block; text-align: center; }"
       ".btn-primary { background: #667eea; color: white; }"
       ".btn-primary:hover { background: #5568d3; transform: translateY(-2px); box-shadow: 0 4px 8px rgba(102, 126, 234, 0.4); }"
       ".btn-secondary { background: #e0e0e0; color: #333; }"
       ".btn-secondary:hover { background: #d0d0d0; }"
       ".btn-success { background: #4CAF50; color: white; padding: 12px 20px; }"
       ".btn-success:hover { background: #45a049; transform: translateY(-2px); box-shadow: 0 4px 8px rgba(76, 175, 80, 0.4); }"
       ".btn-warning { background: #FF9800; color: white; padding: 8px 12px; }"
       ".btn-warning:hover { background: #F57C00; }"
       ".btn-danger { background: #F44336; color: white; padding: 8px 12px; }"
       ".btn-danger:hover { background: #E53935; }"
       ".btn-sm { padding: 6px 10px; font-size: 12px; }"
       ".data-table { width: 100%; border-collapse: collapse; margin-top: 10px; }"
       ".data-table th { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 12px; text-align: left; font-weight: 600; }"
       ".data-table td { padding: 10px; border-bottom: 1px solid #eee; }"
       ".data-table tr:hover { background: #f9f9f9; }"
       ".empty-state { padding: 40px; text-align: center; color: #999; font-size: 16px; background: #f9f9f9; border-radius: 8px; }"
       ".alert { padding: 15px; border-radius: 5px; margin-bottom: 20px; font-weight: 500; }"
       ".alert-success { background: #e8f5e9; color: #2e7d32; border-left: 4px solid #4CAF50; }"
       ".alert-error { background: #ffebee; color: #c62828; border-left: 4px solid #F44336; }"
       ".alert-info { background: #e3f2fd; color: #1565c0; border-left: 4px solid #2196F3; }"
       "@media (max-width: 768px) {"
       "  nav a { display: block; margin: 5px 0; }"
       "  .data-table { font-size: 12px; }"
       "  .data-table th, .data-table td { padding: 8px; }"
       "}"
       "</style>"
       "</head>"
       "<body>"
       "<div class='container'>"
       "<header>"
       "<h1>🏭 Система управления персоналом</h1>"
       "<p style='opacity: 0.9; margin-top: 5px;'>CRUD-приложение для базы данных работников</p>"
       "</header>"
       
       "<nav>"
       "<a href='/'>Главная</a>"
       "<a href='/workers' class='active'>Работники</a>"
       "<a href='/db'>Все таблицы</a>"
       "</nav>"
       
       content
       
       "</div>"
       "</body>"
       "</html>"))

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

;; Роуты приложения
(defroutes app-routes
  ;; Главная страница
  (GET "/" []
    (-> (resp/response 
          (generate-page 
            "<div class='alert alert-info'>Добро пожаловать в систему управления персоналом! Выберите раздел в меню выше.</div>"
            "Главная"))
        (resp/content-type "text/html; charset=utf-8")))
  
  ;; Список работников
  (GET "/workers" []
    (let [workers (get-workers-with-details)]
      (-> (resp/response 
            (generate-page (generate-workers-table workers) "Работники"))
          (resp/content-type "text/html; charset=utf-8"))))
  
  ;; Форма создания работника
  (GET "/workers/new" []
    (-> (resp/response 
          (generate-page (generate-worker-form :create nil) "Новый работник"))
        (resp/content-type "text/html; charset=utf-8")))
  
  ;; Форма редактирования работника
  (GET "/workers/:id/edit" [id]
    (let [worker (get-record-by-id "Работник" id)]
      (if worker
        (-> (resp/response 
              (generate-page (generate-worker-form :edit worker) "Редактировать работника"))
            (resp/content-type "text/html; charset=utf-8"))
        (-> (resp/response "Работник не найден")
            (resp/status 404)
            (resp/content-type "text/html; charset=utf-8")))))
  
  ;; Создание работника - ИСПРАВЛЕНО: правильная обработка параметров
  (POST "/workers/create" request
    (let [params (:params request)
          data {:фамилия (:фамилия params)
                :имя (:имя params)
                :отчество (:отчество params)
                :дата_приема (:дата_приема params)
                :цех_id (Integer/parseInt (:цех_id params))
                :система_оплаты_id (Integer/parseInt (:система_оплаты_id params))
                :категория_работника_id (Integer/parseInt (:категория_работника_id params))
                :разряд_id (Integer/parseInt (:разряд_id params))
                :режим_работы_id (Integer/parseInt (:режим_работы_id params))
                :оклад_id (when (seq (:оклад_id params)) (Integer/parseInt (:оклад_id params)))
                :почасовая_ставка_id (when (seq (:почасовая_ставка_id params)) (Integer/parseInt (:почасовая_ставка_id params)))}]
      (let [result (create-record "Работник" data)]
        (if (:success result)
          (resp/redirect "/workers")
          (-> (resp/response (generate-page 
                               (str "<div class='alert alert-error'>" (:message result) "</div>"
                                    (generate-worker-form :create params))
                               "Ошибка"))
              (resp/content-type "text/html; charset=utf-8"))))))
  
  ;; Обновление работника - ИСПРАВЛЕНО: правильная обработка параметров
  (POST "/workers/:id/update" [id :as request]
    (let [params (:params request)
          data {:фамилия (:фамилия params)
                :имя (:имя params)
                :отчество (:отчество params)
                :дата_приема (:дата_приема params)
                :цех_id (Integer/parseInt (:цех_id params))
                :система_оплаты_id (Integer/parseInt (:система_оплаты_id params))
                :категория_работника_id (Integer/parseInt (:категория_работника_id params))
                :разряд_id (Integer/parseInt (:разряд_id params))
                :режим_работы_id (Integer/parseInt (:режим_работы_id params))
                :оклад_id (when (seq (:оклад_id params)) (Integer/parseInt (:оклад_id params)))
                :почасовая_ставка_id (when (seq (:почасовая_ставка_id params)) (Integer/parseInt (:почасовая_ставка_id params)))}]
      (let [result (update-record "Работник" (Integer/parseInt id) data)]
        (if (:success result)
          (resp/redirect "/workers")
          (-> (resp/response (generate-page 
                               (str "<div class='alert alert-error'>" (:message result) "</div>"
                                    (generate-worker-form :edit (get-record-by-id "Работник" id)))
                               "Ошибка"))
              (resp/content-type "text/html; charset=utf-8"))))))
  
  ;; Удаление работника - ИСПРАВЛЕНО: правильная обработка параметров
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
      (-> (resp/response 
            (generate-page 
              (apply str 
                (map (fn [tbl]
                       (str "<h3>" (:table tbl) "</h3>"
                            "<table border='1' cellpadding='8' cellspacing='0' style='border-collapse: collapse; margin: 10px 0; width: 100%;'>"
                            "<thead style='background: #667eea; color: white;'>"
                            "<tr>" 
                            (apply str 
                              (map (fn [col] 
                                     (str "<th style='padding: 10px;'>" col "</th>")) 
                                   (keys (first (:rows tbl)))))
                            "</tr>"
                            "</thead>"
                            "<tbody>"
                            (apply str 
                              (map (fn [row]
                                     (str "<tr>"
                                          (apply str 
                                            (map (fn [[k v]] 
                                                   (str "<td style='padding: 8px; border: 1px solid #ddd;'>" 
                                                        (if (nil? v) "NULL" (str v)) 
                                                        "</td>"))
                                                 row))
                                          "</tr>"))
                                    (:rows tbl)))
                            "</tbody>"
                            "</table>"))
                     tables-data))
              "Все таблицы"))
          (resp/content-type "text/html; charset=utf-8"))))
  
  (route/not-found
    (-> (resp/response "Страница не найдена")
        (resp/status 404)
        (resp/content-type "text/html; charset=utf-8"))))

;; Middleware для обработки параметров (важен порядок!)
(def app
  (-> app-routes
      wrap-keyword-params  ;; преобразует параметры в ключевые слова
      wrap-params          ;; парсит query string и form params
      (wrap-json-body {:keywords? true})
      wrap-json-response))

(defn -main
  "Точка входа приложения"
  [& args]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (println "Сервер запускается на порту" port)
    (jetty/run-jetty app {:port port})))
(ns my-ring-app.model
  (:require [clojure.java.jdbc :as jdbc]
            [my-ring-app.config :refer [db-spec]]
            [my-ring-app.logger :as logger]))

;; ======================================================================
;; Вспомогательные функции
;; ======================================================================

(defn safe-query [sql params]
  (try
    (logger/log-sql sql params)
    (jdbc/query db-spec [sql params])
    (catch Exception e
      (logger/log-error e (format "Ошибка запроса: %s" sql) {:sql sql :params params})
      [])))

(defn safe-insert [table data]
  (try
    (logger/log-sql (str "INSERT INTO " table) data)
    (jdbc/insert! db-spec table data)
    (catch Exception e
      (logger/log-error e (format "Ошибка вставки в %s" table) {:table table :data data})
      nil)))

(defn safe-update [table data where-clause]
  (try
    (logger/log-sql (str "UPDATE " table " SET ...") where-clause)
    (jdbc/update! db-spec table data where-clause)
    (catch Exception e
      (logger/log-error e (format "Ошибка обновления %s" table) {:table table :data data})
      0)))

(defn safe-delete [table where-clause]
  (try
    (logger/log-sql (str "DELETE FROM " table) where-clause)
    (jdbc/delete! db-spec table where-clause)
    (catch Exception e
      (logger/log-error e (format "Ошибка удаления из %s" table) {:table table :where where-clause})
      0)))

;; ======================================================================
;; Общие функции работы с таблицами
;; ======================================================================

(defn get-tables []
  (try
    (let [tables (jdbc/query db-spec ["SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name"])]
      (logger/log-info (format "Получено %d таблиц из БД" (count tables)))
      (map :name tables))
    (catch Exception e
      (logger/log-error e "Ошибка при получении таблиц")
      [])))

(defn get-table-data [table-name]
  (try
    (let [result (jdbc/query db-spec [(str "SELECT * FROM \"" table-name "\"")])]
      (logger/log-info (format "Получено %d записей из таблицы %s" (count result) table-name))
      result)
    (catch Exception e
      (logger/log-error e (format "Ошибка при запросе к таблице %s" table-name) {:table table-name})
      [])))

(defn get-record-by-id [table-name id]
  (try
    (let [result (first (jdbc/query db-spec [(str "SELECT * FROM \"" table-name "\" WHERE id = ?") id]))]
      (if result
        (logger/log-info (format "Найдена запись ID=%s в таблице %s" id table-name))
        (logger/log-warn (format "Запись ID=%s не найдена в таблице %s" id table-name)))
      result)
    (catch Exception e
      (logger/log-error e (format "Ошибка при получении записи ID=%s из таблицы %s" id table-name)
                        {:table table-name :id id})
      nil)))

(defn create-record [table-name data]
  (try
    (let [result (safe-insert table-name data)]
      (if result
        (do
          (logger/log-info (format "Создана запись в таблице %s" table-name))
          {:success true :message "Запись успешно создана" :id (first result)})
        {:success false :message "Ошибка при создании записи"}))
    (catch Exception e
      (logger/log-error e (format "Ошибка при создании записи в таблице %s" table-name)
                        {:table table-name :data data})
      {:success false :message (str "Ошибка при создании: " (.getMessage e))})))

(defn update-record [table-name id data]
  (try
    (let [result (safe-update table-name data ["id = ?" id])]
      (if (> result 0)
        (do
          (logger/log-info (format "Обновлена запись ID=%s в таблице %s" id table-name))
          {:success true :message "Запись успешно обновлена"})
        (do
          (logger/log-warn (format "Запись ID=%s не найдена для обновления" id))
          {:success false :message "Запись не найдена"})))
    (catch Exception e
      (logger/log-error e (format "Ошибка при обновлении записи ID=%s в таблице %s" id table-name)
                        {:table table-name :id id :data data})
      {:success false :message (str "Ошибка при обновлении: " (.getMessage e))})))

(defn delete-record [table-name id]
  (try
    (let [result (safe-delete table-name ["id = ?" id])]
      (if (> result 0)
        (do
          (logger/log-info (format "Удалена запись ID=%s из таблицы %s" id table-name))
          {:success true :message "Запись успешно удалена"})
        (do
          (logger/log-warn (format "Запись ID=%s не найдена для удаления" id))
          {:success false :message "Запись не найдена"})))
    (catch Exception e
      (logger/log-error e (format "Ошибка при удалении записи ID=%s из таблицы %s" id table-name)
                        {:table table-name :id id})
      {:success false :message (str "Ошибка при удалении: " (.getMessage e))})))

(defn get-spravochnik [table-name]
  (try
    (let [result (jdbc/query db-spec [(str "SELECT * FROM \"" table-name "\"")])]
      (logger/log-info (format "Получен справочник %s (%d записей)" table-name (count result)))
      result)
    (catch Exception e
      (logger/log-error e (format "Ошибка при получении справочника %s" table-name) {:table table-name})
      [])))

;; ======================================================================
;; Модель работников
;; ======================================================================

(defn get-workers-with-details []
  (try
    (let [result (jdbc/query db-spec
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
        ORDER BY r.фамилия, r.имя"])]
      (logger/log-info (format "Получен список работников (%d записей)" (count result)))
      result)
    (catch Exception e
      (logger/log-error e "Ошибка при получении списка работников")
      [])))

(defn search-workers [query]
  (try
    (let [search-term (str "%" query "%")
          result (jdbc/query db-spec
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
                              search-term search-term search-term search-term])]
      (logger/log-info (format "Поиск работников по запросу '%s': найдено %d записей" query (count result)))
      result)
    (catch Exception e
      (logger/log-error e (format "Ошибка при поиске работников по запросу '%s'" query) {:query query})
      [])))

;; ======================================================================
;; Модель зарплаты
;; ======================================================================

(defn get-worker-salary [worker-id year month]
  (try
    (let [result (first (jdbc/query db-spec
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
                                     worker-id year month]))]
      (if result
        (logger/log-info (format "Получена информация о зарплате работника ID=%s за %d-%d" worker-id year month))
        (logger/log-warn (format "Данные о зарплате работника ID=%s за %d-%d не найдены" worker-id year month)))
      result)
    (catch Exception e
      (logger/log-error e (format "Ошибка при получении зарплаты работника ID=%s за %d-%d" worker-id year month)
                        {:worker-id worker-id :year year :month month})
      nil)))

(defn get-worker-salary-history [worker-id]
  (try
    (let [result (jdbc/query db-spec
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
                              worker-id])]
      (logger/log-info (format "Получена история зарплат работника ID=%s (%d записей)" worker-id (count result)))
      result)
    (catch Exception e
      (logger/log-error e (format "Ошибка при получении истории зарплаты работника ID=%s" worker-id)
                        {:worker-id worker-id})
      [])))

;; ======================================================================
;; Модель учета рабочего времени
;; ======================================================================

(defn get-worker-work-time [worker-id]
  (try
    (let [result (jdbc/query db-spec
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
                              worker-id])]
      (logger/log-info (format "Получен учет рабочего времени работника ID=%s (%d записей)" worker-id (count result)))
      result)
    (catch Exception e
      (logger/log-error e (format "Ошибка при получении учета рабочего времени работника ID=%s" worker-id)
                        {:worker-id worker-id})
      [])))

(defn get-work-time-by-id [id]
  (try
    (let [result (first (jdbc/query db-spec
                                    ["SELECT * FROM Учет_рабочего_времени WHERE id = ?" id]))]
      (if result
        (logger/log-info (format "Найдена запись учета времени ID=%s" id))
        (logger/log-warn (format "Запись учета времени ID=%s не найдена" id)))
      result)
    (catch Exception e
      (logger/log-error e (format "Ошибка при получении записи учета времени ID=%s" id) {:id id})
      nil)))

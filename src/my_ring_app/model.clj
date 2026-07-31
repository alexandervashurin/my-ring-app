(ns my-ring-app.model
  (:require [clojure.java.jdbc :as jdbc]
            [my-ring-app.config :refer [db-spec app-config]]
            [my-ring-app.logger :as logger]
            [clojure.data.json :as json]
            [clojure.string :as str])
  (:import [java.time LocalDate]))

;; ======================================================================
;; Безопасность: белый список таблиц для динамических запросов
;; ======================================================================

(def ^:private allowed-tables
  "Белый список таблиц, доступных для динамических запросов"
  #{"Работник" "Цех" "Система_оплаты" "Категория_работника"
    "Разряд" "Режим_работы" "Оклад" "Почасовые_ставки"
    "Учет_рабочего_времени" "Начисление_заработной_платы"
    "Пользователь" "Аудит_изменений" "Организация" "Тарифный_план" "Сессия" "schema_migrations"})

(defn current-year-month
  "Возвращает текущий год и месяц как вектор [год месяц]"
  []
  (let [now (LocalDate/now)]
    [(.getYear now) (.getValue (.getMonth now))]))

(defn- validate-table-name
  "Проверка имени таблицы по белому списку"
  [table-name]
  (when-not (contains? allowed-tables (str table-name))
    (throw (SecurityException.
            (format "Доступ к таблице '%s' запрещён" table-name))))
  table-name)

(defn- sanitize-table-name
  "Очистка имени таблицы от потенциально опасных символов.
   Оставляет только буквы, цифры и подчёркивания."
  [table-name]
  (let [cleaned (str/replace (str table-name) #"[^a-zA-Zа-яА-ЯёЁ0-9_]" "")]
    (when (empty? cleaned)
      (throw (SecurityException. "Имя таблицы не может быть пустым после очистки")))
    cleaned))

;; ======================================================================
;; Вспомогательные функции
;; ======================================================================

(defn log-audit-change
  "Логирование изменений в таблицу аудита"
  [entity-type entity-id action user-info old-values new-values & [details]]
  (try
    (let [audit-data (cond-> {:entity_type entity-type
                              :entity_id entity-id
                              :action action
                              :user_id (:user-id user-info)
                              :username (:username user-info "system")
                              :old_values (when old-values (json/write-str old-values))
                              :new_values (when new-values (json/write-str new-values))
                              :ip_address (:ip-address user-info)
                              :user_agent (:user-agent user-info)
                              :details (or details "-")}
                       (:org-id user-info)
                       (assoc :organization_id (:org-id user-info)))]
      (jdbc/insert! db-spec :Аудит_изменений audit-data)
      (logger/log-info (format "Аудит: %s %s ID=%d пользователем %s"
                                action entity-type entity-id (:username user-info "system"))))
    (catch Exception e
      (logger/log-error e "Ошибка при записи в аудит"
                        {:entity-type entity-type :entity-id entity-id :action action}))))

(defn safe-query [sql params]
  (try
    (logger/log-sql sql params)
    (jdbc/query db-spec (into [sql] params))
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
    (let [[affected _] (jdbc/update! db-spec table data where-clause)]
      (long affected))
    (catch Exception e
      (logger/log-error e (format "Ошибка обновления %s" table) {:table table :data data})
      0)))

(defn safe-delete [table where-clause]
  (try
    (logger/log-sql (str "DELETE FROM " table) where-clause)
    (first (jdbc/delete! db-spec table where-clause))
    (catch Exception e
      (logger/log-error e (format "Ошибка удаления из %s" table) {:table table :where where-clause})
      0)))

;; ======================================================================
;; Общие функции работы с таблицами
;; ======================================================================

(defn get-tables []
  (try
    (let [db-type (:db-type app-config)
          query (if (= db-type :postgresql)
                  ["SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename"]
                  ["SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name"])
          tables (jdbc/query db-spec query)]
      (logger/log-info (format "Получено %d таблиц из БД" (count tables)))
      (if (= db-type :postgresql)
        (map :tablename tables)
        (map :name tables)))
    (catch Exception e
      (logger/log-error e "Ошибка при получении таблиц")
      [])))

(defn get-table-data [table-name]
  (try
    (validate-table-name table-name)
    (let [safe-name (sanitize-table-name table-name)
          result (jdbc/query db-spec [(str "SELECT * FROM \"" safe-name "\"")])]
      (logger/log-info (format "Получено %d записей из таблицы %s" (count result) table-name))
      result)
    (catch Exception e
      (logger/log-error e (format "Ошибка при запросе к таблице %s" table-name) {:table table-name})
      [])))

(defn get-record-by-id [table-name id]
  (try
    (validate-table-name table-name)
    (let [safe-name (sanitize-table-name table-name)
          result (first (jdbc/query db-spec [(str "SELECT * FROM \"" safe-name "\" WHERE id = ?") id]))]
      (if result
        (logger/log-info (format "Найдена запись ID=%s в таблице %s" id table-name))
        (logger/log-warn (format "Запись ID=%s не найдена в таблице %s" id table-name)))
      result)
    (catch Exception e
      (logger/log-error e (format "Ошибка при получении записи ID=%s из таблицы %s" id table-name)
                        {:table table-name :id id})
      nil)))

(defn create-record
  "Создание записи в таблице. Если org-id передан — автоматически добавляет organization_id."
  ([table-name data] (create-record table-name data nil))
  ([table-name data org-id]
   (try
     (validate-table-name table-name)
     (let [final-data (if (and org-id (not (:organization_id data)))
                        (assoc data :organization_id org-id)
                        data)
           result (safe-insert table-name final-data)
           row (first result)
           record-id (when (map? row)
                       (let [v (first (vals row))]
                         (if (number? v) v (str v))))]
       (if result
         (do
           (logger/log-info (format "Создана запись в таблице %s, ID=%s (org: %s)" table-name (str record-id) (str org-id)))
           {:success true :message "Запись успешно создана" :id record-id})
         {:success false :message "Ошибка при создании записи"}))
     (catch Exception e
       (logger/log-error e (format "Ошибка при создании записи в таблице %s" table-name)
                         {:table table-name :data data})
       {:success false :message "Внутренняя ошибка при создании записи"}))))

(defn update-record [table-name id data]
  (try
    (validate-table-name table-name)
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
      {:success false :message "Внутренняя ошибка при обновлении записи"})))

(defn delete-record [table-name id]
  (try
    (validate-table-name table-name)
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
      {:success false :message "Внутренняя ошибка при удалении записи"})))

;; ======================================================================
;; Модель работников
;; ======================================================================

(defn get-workers-with-details
  "Получение списка работников с деталями. Если org-id не nil — фильтрует по организации."
  ([] (get-workers-with-details nil))
  ([org-id]
   (try
     (let [base-query "SELECT r.id, r.фамилия, r.имя, r.отчество, r.дата_приема,
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
        LEFT JOIN Режим_работы рм ON r.режим_работы_id = рм.id"
           query (if org-id
                   (str base-query " WHERE r.organization_id = ? ORDER BY r.фамилия, r.имя")
                   (str base-query " ORDER BY r.фамилия, r.имя"))
           result (jdbc/query db-spec (into [query] (when org-id [org-id])))]
       (logger/log-info (format "Получен список работников (%d записей, org: %s)" (count result) (str org-id)))
       result)
     (catch Exception e
        (logger/log-error e "Ошибка при получении списка работников")
        []))))

(defn search-workers
  "Поиск работников. Если org-id не nil — фильтрует по организации."
  ([query] (search-workers query nil))
  ([query org-id]
   (try
     (when (and query (> (count (str query)) 100))
       (logger/log-warn (format "Поисковый запрос слишком длинный (%d символов)" (count (str query))))
       (throw (IllegalArgumentException. "Поисковый запрос слишком длинный")))
     (let [search-term (str "%" (str query) "%")
           base-query "SELECT r.id, r.фамилия, r.имя, r.отчество, r.дата_приема,
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
               LEFT JOIN Режим_работы рм ON r.режим_работы_id = рм.id"
           where-clause " WHERE (LOWER(r.фамилия) LIKE LOWER(?) 
                  OR LOWER(r.имя) LIKE LOWER(?) 
                  OR LOWER(r.отчество) LIKE LOWER(?)
                  OR LOWER(ц.название_цеха) LIKE LOWER(?))"
           org-clause (when org-id " AND r.organization_id = ?")
           order-clause " ORDER BY r.фамилия, r.имя"
           [full-query params] (if org-id
                                  [(str base-query where-clause org-clause order-clause)
                                   [search-term search-term search-term search-term org-id]]
                                  [(str base-query where-clause order-clause)
                                   [search-term search-term search-term search-term]])
           result (jdbc/query db-spec (into [full-query] params))]
       (logger/log-info (format "Поиск работников по запросу '%s': найдено %d записей (org: %s)"
                                query (count result) (str org-id)))
       result)
     (catch Exception e
       (logger/log-error e (format "Ошибка при поиске работников по запросу '%s'" query) {:query query})
       []))))

;; ======================================================================
;; Модель зарплаты
;; ======================================================================

(defn get-worker-salary
  "Получение зарплаты работника. Если org-id не nil — проверяет принадлежность к организации."
  ([worker-id year month] (get-worker-salary worker-id year month nil))
  ([worker-id year month org-id]
   (try
     (let [base-query "SELECT r.id, r.фамилия, r.имя, r.отчество, ц.название_цеха,
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
        LEFT JOIN Почасовые_ставки п ON r.почасовая_ставка_id = п.id"
           org-condition (when org-id " AND r.organization_id = ?")
           where-clause " WHERE r.id = ? AND у.год = ? AND у.месяц = ?"
           params (if org-id [worker-id year month org-id] [worker-id year month])
           result (first (jdbc/query db-spec
                                     [(str base-query where-clause org-condition) params]))]
       (if result
         (logger/log-info (format "Получена информация о зарплате работника ID=%s за %d-%d" worker-id year month))
         (logger/log-warn (format "Данные о зарплате работника ID=%s за %d-%d не найдены" worker-id year month)))
       result)
     (catch Exception e
       (logger/log-error e (format "Ошибка при получении зарплаты работника ID=%s за %d-%d" worker-id year month)
                         {:worker-id worker-id :year year :month month})
       nil))))

(defn get-worker-salary-history
  "Получение истории зарплат. Если org-id не nil — фильтрует по организации."
  ([worker-id] (get-worker-salary-history worker-id nil))
  ([worker-id org-id]
   (try
     (let [base-query "SELECT r.фамилия, r.имя, у.год, у.месяц,
               н.общая_зарплата,
               н.зарплата_за_больничные_дни,
               н.зарплата_за_командировочные_дни,
               у.больничные_дни,
               у.командировочные_дни
        FROM Работник r
        LEFT JOIN Учет_рабочего_времени у ON r.id = у.работник_id
        LEFT JOIN Начисление_заработной_платы н ON у.id = н.учет_рабочего_времени_id
        WHERE r.id = ?"
           org-condition (when org-id " AND r.organization_id = ?")
           order-clause " ORDER BY у.год DESC, у.месяц DESC"
           params (if org-id [worker-id org-id] [worker-id])
           result (jdbc/query db-spec [(str base-query org-condition order-clause) params])]
       (logger/log-info (format "Получена история зарплат работника ID=%s (%d записей)" worker-id (count result)))
       result)
     (catch Exception e
       (logger/log-error e (format "Ошибка при получении истории зарплаты работника ID=%s" worker-id)
                         {:worker-id worker-id})
       []))))

;; ======================================================================
;; Модель учета рабочего времени
;; ======================================================================

(defn get-worker-work-time
  "Получение учета рабочего времени. Если org-id не nil — фильтрует по организации."
  ([worker-id] (get-worker-work-time worker-id nil))
  ([worker-id org-id]
   (try
     (let [base-query "SELECT у.id, у.год, у.месяц,
               у.всего_часов_за_месяц_по_плану,
               у.всего_часов_в_месяц_по_факту,
               у.количество_отработанных_дней,
               у.количество_рабочих_часов_в_день,
               у.всего_отработанных_часов,
               у.сколько_должны_отработать,
               у.больничные_дни,
               у.командировочные_дни
        FROM Учет_рабочего_времени у
        WHERE у.работник_id = ?"
           org-condition (when org-id " AND у.organization_id = ?")
           order-clause " ORDER BY у.год DESC, у.месяц DESC"
           params (if org-id [worker-id org-id] [worker-id])
           result (jdbc/query db-spec [(str base-query org-condition order-clause) params])]
       (logger/log-info (format "Получен учет рабочего времени работника ID=%s (%d записей)" worker-id (count result)))
       result)
     (catch Exception e
       (logger/log-error e (format "Ошибка при получении учета рабочего времени работника ID=%s" worker-id)
                         {:worker-id worker-id})
       []))))

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

;; ======================================================================
;; Dashboard Analytics
;; ======================================================================

(defn get-dashboard-stats
  "Получение общей статистики для дашборда. Если org-id не nil — фильтрует по организации."
  ([] (get-dashboard-stats nil))
  ([org-id]
   (try
     (let [org-filter (when org-id " WHERE r.organization_id = ?")
           org-params (when org-id [org-id])
           total-workers (first (jdbc/query db-spec (if org-id
                                                      ["SELECT COUNT(*) as count FROM Работник r WHERE r.organization_id = ?" org-id]
                                                      ["SELECT COUNT(*) as count FROM Работник"])))
           total-shops (first (jdbc/query db-spec ["SELECT COUNT(*) as count FROM Цех"]))
           avg-salary (first (jdbc/query db-spec (if org-id
                                                    ["SELECT AVG(н.общая_зарплата) as avg
                                                     FROM Начисление_заработной_платы н
                                                     JOIN Учет_рабочего_времени у ON н.учет_рабочего_времени_id = у.id
                                                     JOIN Работник р ON у.работник_id = р.id
                                                     WHERE р.organization_id = ?" org-id]
                                                    ["SELECT AVG(общая_зарплата) as avg FROM Начисление_заработной_платы"])))
           total-payroll (first (jdbc/query db-spec (if org-id
                                                       ["SELECT SUM(н.общая_зарплата) as total
                                                        FROM Начисление_заработной_платы н
                                                        JOIN Учет_рабочего_времени у ON н.учет_рабочего_времени_id = у.id
                                                        JOIN Работник р ON у.работник_id = р.id
                                                        WHERE р.organization_id = ?" org-id]
                                                       ["SELECT SUM(общая_зарплата) as total FROM Начисление_заработной_платы"])))
           workers-by-payment (jdbc/query db-spec
                                          (if org-id
                                            ["SELECT с.название_системы as name, COUNT(r.id) as count
                                             FROM Работник r
                                             LEFT JOIN Система_оплаты с ON r.система_оплаты_id = с.id
                                             WHERE r.organization_id = ?
                                             GROUP BY с.id, с.название_системы
                                             ORDER BY count DESC" org-id]
                                            ["SELECT с.название_системы as name, COUNT(r.id) as count
                                             FROM Работник r
                                             LEFT JOIN Система_оплаты с ON r.система_оплаты_id = с.id
                                             GROUP BY с.id, с.название_системы
                                             ORDER BY count DESC"]))]
       (logger/log-info (format "Получена статистика для дашборда (org: %s)" (str org-id)))
       {:total-workers (:count total-workers)
        :total-shops (:count total-shops)
        :avg-salary (or (:avg avg-salary) 0)
        :total-payroll (or (:total total-payroll) 0)
        :workers-by-payment workers-by-payment})
     (catch Exception e
       (logger/log-error e "Ошибка при получении статистики дашборда")
       {}))))

(defn get-salary-distribution
  "Распределение работников по уровню зарплаты. Если org-id не nil — фильтрует по организации."
  ([] (get-salary-distribution nil))
  ([org-id]
   (try
     (let [[year month] (current-year-month)
           result (jdbc/query db-spec
                              (if org-id
                                ["SELECT 
                                   SUM(CASE WHEN n.общая_зарплата < 40000 THEN 1 ELSE 0 END) as low,
                                   SUM(CASE WHEN n.общая_зарплата BETWEEN 40000 AND 60000 THEN 1 ELSE 0 END) as medium,
                                   SUM(CASE WHEN n.общая_зарплата BETWEEN 60001 AND 90000 THEN 1 ELSE 0 END) as high,
                                   SUM(CASE WHEN n.общая_зарплата > 90000 THEN 1 ELSE 0 END) as very_high
                                 FROM Начисление_заработной_платы n
                                 JOIN Учет_рабочего_времени у ON n.учет_рабочего_времени_id = у.id
                                 JOIN Работник р ON у.работник_id = р.id
                                 WHERE у.год = ? AND у.месяц = ? AND р.organization_id = ?" year month org-id]
                                ["SELECT 
                                   SUM(CASE WHEN n.общая_зарплата < 40000 THEN 1 ELSE 0 END) as low,
                                   SUM(CASE WHEN n.общая_зарплата BETWEEN 40000 AND 60000 THEN 1 ELSE 0 END) as medium,
                                   SUM(CASE WHEN n.общая_зарплата BETWEEN 60001 AND 90000 THEN 1 ELSE 0 END) as high,
                                   SUM(CASE WHEN n.общая_зарплата > 90000 THEN 1 ELSE 0 END) as very_high
                                 FROM Начисление_заработной_платы n
                                 JOIN Учет_рабочего_времени у ON n.учет_рабочего_времени_id = у.id
                                 WHERE у.год = ? AND у.месяц = ?" year month]))]
       (if-let [row (first result)]
         (do
           (logger/log-info "Получено распределение по зарплате")
           [{:name "Менее 40 000 ₽" :count (or (:low row) 0)}
            {:name "40 000 - 60 000 ₽" :count (or (:medium row) 0)}
            {:name "60 000 - 90 000 ₽" :count (or (:high row) 0)}
            {:name "Более 90 000 ₽" :count (or (:very_high row) 0)}])
         []))
     (catch Exception e
        (logger/log-error e "Ошибка при получении распределения по зарплате")
        []))))

(defn get-attendance-stats
  "Статистика посещаемости. Если org-id не nil — фильтрует по организации."
  ([] (get-attendance-stats nil))
  ([org-id]
   (try
     (let [[year month] (current-year-month)
           result (first (jdbc/query db-spec
                                     (if org-id
                                       ["SELECT 
                                         AVG(у.всего_отработанных_часов) as avg_hours,
                                         AVG(у.больничные_дни) as avg_sick,
                                         AVG(у.командировочные_дни) as avg_business
                                       FROM Учет_рабочего_времени у
                                       JOIN Работник р ON у.работник_id = р.id
                                       WHERE у.год = ? AND у.месяц = ? AND р.organization_id = ?"
                                      year month org-id]
                                       ["SELECT 
                                         AVG(всего_отработанных_часов) as avg_hours,
                                         AVG(больничные_дни) as avg_sick,
                                         AVG(командировочные_дни) as avg_business
                                       FROM Учет_рабочего_времени
                                       WHERE год = ? AND месяц = ?"
                                      year month])))]
       (logger/log-info "Получена статистика посещаемости")
       {:avg-hours (or (:avg_hours result) 0)
        :avg-sick-days (or (:avg_sick result) 0)
        :avg-business-days (or (:avg_business result) 0)})
     (catch Exception e
       (logger/log-error e "Ошибка при получении статистики посещаемости")
       {}))))

(defn get-workers-by-shop
  "Распределение работников по цехам. Если org-id не nil — фильтрует по организации."
  ([] (get-workers-by-shop nil))
  ([org-id]
   (try
     (let [result (jdbc/query db-spec
                              (if org-id
                                ["SELECT ц.название_цеха as name, COUNT(r.id) as count
                                 FROM Цех ц
                                 LEFT JOIN Работник r ON r.цех_id = ц.id AND r.organization_id = ?
                                 GROUP BY ц.id, ц.название_цеха
                                 ORDER BY count DESC" org-id]
                                ["SELECT ц.название_цеха as name, COUNT(r.id) as count
                                 FROM Цех ц
                                 LEFT JOIN Работник r ON r.цех_id = ц.id
                                 GROUP BY ц.id, ц.название_цеха
                                 ORDER BY count DESC"]))]
       (logger/log-info (format "Получено распределение по цехам (%d записей)" (count result)))
       result)
     (catch Exception e
        (logger/log-error e "Ошибка при получении распределения по цехам")
        []))))

(defn get-workers-by-category
  "Распределение работников по категориям. Если org-id не nil — фильтрует по организации."
  ([] (get-workers-by-category nil))
  ([org-id]
   (try
     (let [result (jdbc/query db-spec
                              (if org-id
                                ["SELECT к.название_категории as name, COUNT(r.id) as count
                                 FROM Категория_работника к
                                 LEFT JOIN Работник r ON r.категория_работника_id = к.id AND r.organization_id = ?
                                 GROUP BY к.id, к.название_категории
                                 ORDER BY count DESC" org-id]
                                ["SELECT к.название_категории as name, COUNT(r.id) as count
                                 FROM Категория_работника к
                                 LEFT JOIN Работник r ON r.категория_работника_id = к.id
                                 GROUP BY к.id, к.название_категории
                                 ORDER BY count DESC"]))]
       (logger/log-info (format "Получено распределение по категориям (%d записей)" (count result)))
       result)
     (catch Exception e
        (logger/log-error e "Ошибка при получении распределения по категориям")
        []))))

(defn get-workers-by-rank
  "Распределение работников по разрядам. Если org-id не nil — фильтрует по организации."
  ([] (get-workers-by-rank nil))
  ([org-id]
   (try
     (let [result (jdbc/query db-spec
                              (if org-id
                                ["SELECT рз.номер_разряда as name, COUNT(r.id) as count
                                 FROM Разряд рз
                                 LEFT JOIN Работник r ON r.разряд_id = рз.id AND r.organization_id = ?
                                 GROUP BY рз.id, рз.номер_разряда
                                 ORDER BY рз.номер_разряда" org-id]
                                ["SELECT рз.номер_разряда as name, COUNT(r.id) as count
                                 FROM Разряд рз
                                 LEFT JOIN Работник r ON r.разряд_id = рз.id
                                 GROUP BY рз.id, рз.номер_разряда
                                 ORDER BY рз.номер_разряда"]))]
       (logger/log-info (format "Получено распределение по разрядам (%d записей)" (count result)))
       result)
     (catch Exception e
        (logger/log-error e "Ошибка при получении распределения по разрядам")
        []))))

(defn get-payroll-by-month
  "Фонд оплаты труда по месяцам. Если org-id не nil — фильтрует по организации."
  ([] (get-payroll-by-month nil))
  ([org-id]
   (try
     (let [result (jdbc/query db-spec
                              (if org-id
                                ["SELECT н.год, н.месяц, SUM(н.общая_зарплата) as total
                                 FROM Начисление_заработной_платы н
                                 JOIN Учет_рабочего_времени у ON н.учет_рабочего_времени_id = у.id
                                 JOIN Работник р ON у.работник_id = р.id
                                 WHERE р.organization_id = ?
                                 GROUP BY н.год, н.месяц
                                 ORDER BY н.год DESC, н.месяц DESC
                                 LIMIT 6" org-id]
                                ["SELECT н.год, н.месяц, SUM(н.общая_зарплата) as total
                                 FROM Начисление_заработной_платы н
                                 JOIN Учет_рабочего_времени у ON н.учет_рабочего_времени_id = у.id
                                 GROUP BY н.год, н.месяц
                                 ORDER BY н.год DESC, н.месяц DESC
                                 LIMIT 6"]))]
       (logger/log-info (format "Получен фонд оплаты труда по месяцам (%d записей)" (count result)))
       result)
     (catch Exception e
        (logger/log-error e "Ошибка при получении фонда оплаты труда по месяцам")
        []))))

(defn get-top-workers-by-salary
  "Топ-5 работников по зарплате. Если org-id не nil — фильтрует по организации."
  ([] (get-top-workers-by-salary nil))
  ([org-id]
   (try
     (let [result (jdbc/query db-spec
                              (if org-id
                                 ["SELECT r.фамилия, r.имя, r.отчество, ц.название_цеха,
                                          MAX(н.общая_зарплата) as max_salary
                                 FROM Работник r
                                 LEFT JOIN Цех ц ON r.цех_id = ц.id
                                 LEFT JOIN Учет_рабочего_времени у ON r.id = у.работник_id
                                 LEFT JOIN Начисление_заработной_платы н ON у.id = н.учет_рабочего_времени_id
                                 WHERE r.organization_id = ?
                                 GROUP BY r.id, r.фамилия, r.имя, r.отчество, ц.название_цеха
                                 ORDER BY max_salary DESC
                                 LIMIT 5" org-id]
                                 ["SELECT r.фамилия, r.имя, r.отчество, ц.название_цеха,
                                          MAX(н.общая_зарплата) as max_salary
                                 FROM Работник r
                                 LEFT JOIN Цех ц ON r.цех_id = ц.id
                                 LEFT JOIN Учет_рабочего_времени у ON r.id = у.работник_id
                                 LEFT JOIN Начисление_заработной_платы н ON у.id = н.учет_рабочего_времени_id
                                 GROUP BY r.id, r.фамилия, r.имя, r.отчество, ц.название_цеха
                                 ORDER BY max_salary DESC
                                 LIMIT 5"]))]
       (logger/log-info (format "Получен топ работников по зарплате (%d записей)" (count result)))
       result)
     (catch Exception e
        (logger/log-error e "Ошибка при получении топа работников по зарплате")
        []))))

(defn get-recent-hires
  "Последние принятые работники (топ-5). Если org-id не nil — фильтрует по организации."
  ([] (get-recent-hires nil))
  ([org-id]
   (try
     (let [result (jdbc/query db-spec
                              (if org-id
                                ["SELECT r.id, r.фамилия, r.имя, r.отчество, r.дата_приема,
                                         ц.название_цеха as цех
                                FROM Работник r
                                LEFT JOIN Цех ц ON r.цех_id = ц.id
                                WHERE r.organization_id = ?
                                ORDER BY r.дата_приема DESC
                                LIMIT 5" org-id]
                                ["SELECT r.id, r.фамилия, r.имя, r.отчество, r.дата_приема,
                                         ц.название_цеха as цех
                                FROM Работник r
                                LEFT JOIN Цех ц ON r.цех_id = ц.id
                                ORDER BY r.дата_приема DESC
                                LIMIT 5"]))]
       (logger/log-info (format "Получены последние принятые работники (%d записей)" (count result)))
       result)
     (catch Exception e
        (logger/log-error e "Ошибка при получении последних принятых работников")
        []))))

(defn get-dashboard-data
  "Получение всех данных для дашборда. Если org-id не nil — фильтрует по организации."
  ([] (get-dashboard-data nil))
  ([org-id]
   (logger/log-info (format "Запрос данных для дашборда (org: %s)" (str org-id)))
   {:stats (get-dashboard-stats org-id)
    :by-shop (get-workers-by-shop org-id)
    :by-category (get-workers-by-category org-id)
    :by-rank (get-workers-by-rank org-id)
    :payroll-by-month (get-payroll-by-month org-id)
    :top-workers (get-top-workers-by-salary org-id)
    :recent-hires (get-recent-hires org-id)
    :salary-distribution (get-salary-distribution org-id)
    :attendance (get-attendance-stats org-id)}))

(defn get-salary-with-details
  "Получение всех начислений с именами работников и данными учета времени. Если org-id не nil — фильтрует по организации."
  ([] (get-salary-with-details nil))
  ([org-id]
   (try
     (let [result (jdbc/query db-spec
                              (if org-id
                                ["SELECT н.id, н.учет_рабочего_времени_id,
                 у.год, у.месяц,
                 н.общая_зарплата,
                 н.зарплата_за_больничные_дни,
                 н.зарплата_за_командировочные_дни,
                 р.фамилия, р.имя
          FROM Начисление_заработной_платы н
          JOIN Учет_рабочего_времени у ON н.учет_рабочего_времени_id = у.id
          JOIN Работник р ON у.работник_id = р.id
          WHERE р.organization_id = ?"
                                 org-id]
                                ["SELECT н.id, н.учет_рабочего_времени_id,
                 у.год, у.месяц,
                 н.общая_зарплата,
                 н.зарплата_за_больничные_дни,
                 н.зарплата_за_командировочные_дни,
                 р.фамилия, р.имя
          FROM Начисление_заработной_платы н
          JOIN Учет_рабочего_времени у ON н.учет_рабочего_времени_id = у.id
          JOIN Работник р ON у.работник_id = р.id"]))]
        (logger/log-info (format "Получены начисления с деталями (%d записей)" (count result)))
        result)
      (catch Exception e
        (logger/log-error e "Ошибка при получении начислений с деталями")
        []))))

;; ======================================================================
;; Аудит изменений
;; ======================================================================

(defn get-audit-log
  "Получение записей аудита с пагинацией"
  ([] (get-audit-log 50 0))
  ([limit offset] (get-audit-log limit offset nil nil))
  ([limit offset entity-type action]
   (try
     (let [where-clause (cond
                          (and entity-type action)
                          ["WHERE entity_type = ? AND action = ? ORDER BY created_at DESC LIMIT ? OFFSET ?"
                           entity-type action limit offset]
                          entity-type
                          ["WHERE entity_type = ? ORDER BY created_at DESC LIMIT ? OFFSET ?"
                           entity-type limit offset]
                          action
                          ["WHERE action = ? ORDER BY created_at DESC LIMIT ? OFFSET ?"
                           action limit offset]
                          :else
                          ["ORDER BY created_at DESC LIMIT ? OFFSET ?" limit offset])]
       (let [result (apply jdbc/query db-spec where-clause)]
         (logger/log-info (format "Получено %d записей аудита" (count result)))
         result))
     (catch Exception e
       (logger/log-error e "Ошибка при получении записей аудита")
       []))))

(defn get-audit-log-count
  "Получение количества записей аудита"
  ([] (get-audit-log-count nil nil))
  ([entity-type action]
   (try
     (let [where-clause (cond
                          (and entity-type action)
                          ["SELECT COUNT(*) as count FROM Аудит_изменений WHERE entity_type = ? AND action = ?"
                           entity-type action]
                          entity-type
                          ["SELECT COUNT(*) as count FROM Аудит_изменений WHERE entity_type = ?" entity-type]
                          action
                          ["SELECT COUNT(*) as count FROM Аудит_изменений WHERE action = ?" action]
                          :else
                          ["SELECT COUNT(*) as count FROM Аудит_изменений"])]
       (:count (first (apply jdbc/query db-spec where-clause))))
     (catch Exception e
       (logger/log-error e "Ошибка при подсчёте записей аудита")
       0))))

(defn get-audit-count-by-action
  "Получение количества записей аудита по действиям"
  []
  (try
    (let [result (jdbc/query db-spec ["SELECT action, COUNT(*) as count
                                       FROM Аудит_изменений
                                       GROUP BY action"])]
      result)
    (catch Exception e
      (logger/log-error e "Ошибка при подсчёте записей аудита по действиям")
      [])))

(defn get-audit-count-by-entity
  "Получение количества записей аудита по типам сущностей"
  []
  (try
    (let [result (jdbc/query db-spec ["SELECT entity_type, COUNT(*) as count
                                       FROM Аудит_изменений
                                       GROUP BY entity_type
                                       ORDER BY count DESC"])]
      result)
    (catch Exception e
      (logger/log-error e "Ошибка при подсчёте записей аудита по сущностям")
      [])))

(defn get-audit-by-entity
  "Получение истории изменений конкретной сущности"
  [entity-type entity-id]
  (try
    (let [result (jdbc/query db-spec
                             ["SELECT * FROM Аудит_изменений
                              WHERE entity_type = ? AND entity_id = ?
                              ORDER BY created_at DESC"
                              entity-type entity-id])]
      (logger/log-info (format "Получена история %s ID=%d (%d записей)"
                               entity-type entity-id (count result)))
      result)
    (catch Exception e
      (logger/log-error e "Ошибка при получении истории сущности"
                        {:entity-type entity-type :entity-id entity-id})
      [])))

(defn get-audit-by-user
  "Получение действий пользователя"
  [username limit]
  (try
    (let [result (jdbc/query db-spec
                             ["SELECT * FROM Аудит_изменений
                              WHERE username = ?
                              ORDER BY created_at DESC
                              LIMIT ?"
                              username (or limit 100)])]
      (logger/log-info (format "Получены действия пользователя %s (%d записей)" username (count result)))
      result)
    (catch Exception e
      (logger/log-error e "Ошибка при получении действий пользователя"
                        {:username username})
      [])))

(ns my-ring-app.migration
  "Система миграций базы данных"
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [my-ring-app.config :refer [app-config db-spec]]
            [my-ring-app.logger :as logger]))

(defn- pg?
  "Текущая БД — PostgreSQL?"
  []
  (= (:db-type app-config) :postgresql))

(defn- pg-translate
  "Трансляция SQLite-специфичного SQL в PostgreSQL:
   id INTEGER PRIMARY KEY AUTOINCREMENT -> id BIGSERIAL PRIMARY KEY
   BOOLEAN DEFAULT 1 -> BOOLEAN DEFAULT TRUE
   INSERT OR IGNORE INTO -> INSERT INTO"
  [sql]
  (-> sql
      (str/replace #"(?i)\bINTEGER\s+PRIMARY\s+KEY\s+AUTOINCREMENT\b" "BIGSERIAL PRIMARY KEY")
      (str/replace #"(?i)\bBOOLEAN\s+DEFAULT\s+1\b" "BOOLEAN DEFAULT TRUE")
      (str/replace #"(?i)\bINSERT\s+OR\s+IGNORE\s+INTO\b" "INSERT INTO")))

;; ======================================================================
;; Таблица schema_migrations
;; ======================================================================

(defn- ensure-migrations-table!
  "Создание таблицы schema_migrations если не существует"
  []
  (jdbc/execute! db-spec
                 ["CREATE TABLE IF NOT EXISTS schema_migrations (
                     version TEXT PRIMARY KEY,
                     applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"]))

(defn- get-applied-migrations
  "Получение списка применённых миграций"
  []
  (try
    (let [result (jdbc/query db-spec ["SELECT version FROM schema_migrations ORDER BY version"])]
      (set (map :version result)))
    (catch Exception _
      #{})))

(defn- mark-applied!
  "Отметка миграции как применённой"
  [version]
  (jdbc/insert! db-spec :schema_migrations {:version version}))

(defn- mark-rolled-back!
  "Удаление отметки миграции"
  [version]
  (jdbc/delete! db-spec :schema_migrations ["version = ?" version]))

;; ======================================================================
;; Парсинг SQL файлов миграций
;; ======================================================================

(defn- split-sql-statements
  "Разделение SQL-строки на отдельные операторы по ';'.
   Фильтрует пустые операторы и операторы-комментарии."
  [sql]
  (->> (str/split sql #";")
       (map str/trim)
       (remove str/blank?)
       (remove #(str/starts-with? % "--"))
       vec))

(defn- parse-migration-file
  "Парсинг SQL файла миграции. Возвращает {:up [...] :down [...]}"
  [content]
  (let [lines (str/split-lines content)
        sections (reduce (fn [acc line]
                           (let [trimmed (str/trim line)]
                             (cond
                               (= trimmed "-- +migrate Up")
                               (assoc acc :current :up)

                               (= trimmed "-- +migrate Down")
                               (assoc acc :current :down)

                               (and (:current acc)
                                    (not (str/blank? trimmed))
                                    (not (str/starts-with? trimmed "--")))
                               (update acc (:current acc) conj trimmed)

                               :else acc)))
                         {:up [] :down [] :current nil}
                         lines)]
    {:up (str/join "\n" (:up sections))
     :down (str/join "\n" (:down sections))}))

(defn- column-exists?
  "Проверка существования колонки в таблице.
   SQLite — через PRAGMA table_info, PostgreSQL — через information_schema.columns."
  [table column]
  (try
    (if (pg?)
      (let [table-name (str/replace table "\"" "")
            rows (jdbc/query db-spec
                             ["SELECT 1 FROM information_schema.columns
                               WHERE table_schema = 'public'
                                 AND table_name = ?
                                 AND column_name = ?"
                              table-name column])]
        (seq rows))
      (let [columns (jdbc/query db-spec [(str "PRAGMA table_info(" table ")")])
            col-names (set (map :name columns))]
        (contains? col-names column)))
    (catch Exception _ false)))

(defn- execute-sql!
  "Выполнение SQL-строки, разбитой на отдельные операторы.
   SQLite не поддерживает несколько операторов в одном execute! call.
   Пропускает ALTER TABLE ADD COLUMN, если колонка уже существует.
   Для PostgreSQL транслирует SQLite-специфичный синтаксис."
  [sql]
  (doseq [raw-stmt (split-sql-statements sql)]
    (let [was-ignore? (boolean (re-find #"(?i)\bINSERT\s+OR\s+IGNORE\s+INTO\b" raw-stmt))
          stmt (if (pg?)
                 (let [t (pg-translate raw-stmt)]
                   (if was-ignore?
                     (str t " ON CONFLICT DO NOTHING")
                     t))
                 raw-stmt)
          trimmed (str/trim stmt)
          alter-match (re-find #"(?i)ALTER\s+TABLE\s+(\S+)\s+ADD\s+(COLUMN\s+)?(\S+)" trimmed)]
      (if alter-match
        (let [table (nth alter-match 1)
              column (nth alter-match 3)]
          (if (column-exists? table column)
            (logger/log-info (format "Колонка %s.%s уже существует, пропускаю" table column))
            (jdbc/execute! db-spec [trimmed])))
        (jdbc/execute! db-spec [trimmed])))))

(defn- reset-sequences!
  "PostgreSQL: сброс id-последовательностей до MAX(id)
   после вставок с явными id (сиды)."
  []
  (when (pg?)
    (let [tables (map :tablename
                      (jdbc/query db-spec
                                  ["SELECT tablename FROM pg_tables
                                    WHERE schemaname = 'public'
                                      AND tablename <> 'schema_migrations'"]))]
      (doseq [t tables]
        (try
          (let [tq (str "\"" (str/replace t "\"" "\"\"") "\"")
                sql (str "SELECT setval(pg_get_serial_sequence('" tq "', 'id'), "
                         "COALESCE((SELECT MAX(id) FROM " tq "), 1))")]
            (jdbc/execute! db-spec [sql]))
          (catch Exception e
            (logger/log-warn (format "PG: не удалось сбросить последовательность для %s: %s"
                                     t (.getMessage e)))))))))

(defn- load-migration-files
  "Загрузка всех файлов миграций из resources/migrations/.
   Работает как из файловой системы (dev), так и из JAR (production)."
  []
  (let [dir-url (io/resource "migrations")]
    (when dir-url
      (let [process-file (fn [name content]
                           (let [version (str/replace name #"\.sql$" "")]
                             {:version version
                              :filename name
                              :parsed (parse-migration-file content)}))]
        (if (= "file" (.getProtocol dir-url))
          ;; Development — ресурсы на файловой системе
          (let [files (-> (io/file dir-url)
                          .listFiles
                          (->> (filter #(.endsWith (.getName ^java.io.File %) ".sql"))
                               (sort-by #(.getName ^java.io.File %))))]
            (mapv (fn [^java.io.File f]
                    (process-file (.getName f) (slurp f)))
                  files))
          ;; Production — ресурсы внутри JAR
          (let [jar-path (second (re-find #"jar:file:(.+)!/" (str dir-url)))
                jar-file (java.util.jar.JarFile. jar-path)
                prefix "migrations/"
                entries (seq (java.util.Collections/list (.entries jar-file)))
                sql-entries (->> entries
                                 (filter #(and (.startsWith (.getName %) prefix)
                                              (.endsWith (.getName %) ".sql")))
                                 (sort-by #(.getName %)))]
            (try
              (mapv (fn [^java.util.jar.JarEntry e]
                      (let [name (str/replace-first (.getName e) prefix "")
                            content (slurp (.getInputStream jar-file e))]
                        (process-file name content)))
                    sql-entries)
              (finally
                (.close jar-file)))))))))

;; ======================================================================
;; Выполнение миграций
;; ======================================================================

(defn run-migrations!
  "Выполнение всех неприменённых миграций"
  []
  (logger/log-info "Проверка миграций базы данных...")
  (ensure-migrations-table!)
  (let [applied (get-applied-migrations)
        migrations (load-migration-files)
        pending (filterv #(not (contains? applied (:version %))) migrations)]
    (if (empty? pending)
      (logger/log-info (format "Все миграции применены (всего: %d)" (count migrations)))
      (do
        (logger/log-info (format "Найдено %d неприменённых миграций из %d"
                                 (count pending) (count migrations)))
        (doseq [{:keys [version filename parsed]} pending]
          (logger/log-info (format "Применение миграции: %s" filename))
          (try
            (execute-sql! (:up parsed))
            (mark-applied! version)
            (logger/log-info (format "Миграция %s применена успешно" version))
            (catch Exception e
              (logger/log-error e (format "Ошибка при применении миграции %s" version))
              (throw e))))
        (reset-sequences!)
        (logger/log-info (format "Все миграции применены успешно"))))))

(defn rollback-migration!
  "Откат последней применённой миграции"
  []
  (ensure-migrations-table!)
  (let [applied (get-applied-migrations)
        migrations (load-migration-files)
        last-applied (->> migrations
                          (filter #(contains? applied (:version %)))
                          last)]
    (if last-applied
      (do
        (logger/log-info (format "Откат миграции: %s" (:filename last-applied)))
        (try
          (execute-sql! (:down (:parsed last-applied)))
          (mark-rolled-back! (:version last-applied))
          (logger/log-info (format "Миграция %s откачена" (:version last-applied)))
          (catch Exception e
            (logger/log-error e (format "Ошибка при откате миграции %s" (:version last-applied)))
            (throw e))))
      (logger/log-info "Нет миграций для отката"))))

(defn migration-status
  "Получение статуса миграций"
  []
  (ensure-migrations-table!)
  (let [applied (get-applied-migrations)
        migrations (load-migration-files)]
    (mapv (fn [m]
            {:version (:version m)
             :filename (:filename m)
             :applied (contains? applied (:version m))})
          migrations)))

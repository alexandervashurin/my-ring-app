(ns my-ring-app.session-audit
  (:require [clojure.java.jdbc :as jdbc]
            [my-ring-app.config :refer [app-config db-spec]]
            [my-ring-app.logger :as logger]
            [my-ring-app.util :as util]))

(defn- pg?
  "Текущая БД — PostgreSQL?"
  []
  (= (:db-type app-config) :postgresql))

(defn- date-day-sql
  "Выражение приведения login_time к дате (день)"
  []
  (if (pg?) "CAST(login_time AS DATE)" "DATE(login_time)"))

(defn- days-ago-sql
  "Выражение сдвига на N дней назад (параметр — строка вида '-7')"
  []
  (if (pg?)
    "(NOW() + (? || ' days')::interval)"
    "DATE('now', ? || ' days')"))

(defn log-login!
  [user-id username ip-address user-agent org-id success & [fail-reason]]
  (try
    (let [result (jdbc/insert! db-spec :Сессия
                               {:user_id user-id
                                :username username
                                :ip_address ip-address
                                :user_agent user-agent
                                :organization_id org-id
                                :success (boolean success)
                                :fail_reason fail-reason
                                :login_time (util/now-timestamp)})
          session-id (util/extract-id result)]
      (logger/log-info (format "Сессия %d: %s %s (org: %s, ip: %s)"
                                session-id (if success "LOGIN" "LOGIN_FAILED")
                                username (str org-id) (or ip-address "-")))
      session-id)
    (catch Exception e
      (logger/log-error e "Ошибка при логировании входа" {:username username})
      nil)))

(defn log-logout!
  [session-id]
  (when session-id
    (try
      (jdbc/update! db-spec :Сессия
                    {:logout_time (util/now-timestamp)}
                    ["id = ?" session-id])
      (catch Exception e
        (logger/log-error e "Ошибка при логировании выхода" {:session-id session-id})))))

(defn get-user-sessions
  [user-id & [limit]]
  (try
    (jdbc/query db-spec
                ["SELECT * FROM \"Сессия\" WHERE user_id = ? ORDER BY login_time DESC LIMIT ?"
                 user-id (or limit 50)])
    (catch Exception e
      (logger/log-error e "Ошибка при получении сессий пользователя" {:user-id user-id})
      [])))

(defn get-recent-sessions
  [& [limit org-id]]
  (try
    (jdbc/query db-spec
                (if org-id
                  ["SELECT * FROM \"Сессия\" WHERE organization_id = ? ORDER BY login_time DESC LIMIT ?"
                   org-id (or limit 50)]
                  ["SELECT * FROM \"Сессия\" ORDER BY login_time DESC LIMIT ?" (or limit 50)]))
    (catch Exception e
      (logger/log-error e "Ошибка при получении последних сессий")
      [])))

(defn get-failed-logins
  [& [limit org-id]]
  (try
    (jdbc/query db-spec
                (if org-id
                  ["SELECT * FROM \"Сессия\" WHERE success = false AND organization_id = ? ORDER BY login_time DESC LIMIT ?"
                   org-id (or limit 20)]
                  ["SELECT * FROM \"Сессия\" WHERE success = false ORDER BY login_time DESC LIMIT ?" (or limit 20)]))
    (catch Exception e
      (logger/log-error e "Ошибка при получении неудачных попыток входа")
      [])))

(defn get-active-sessions
  [& [org-id]]
  (try
    (jdbc/query db-spec
                (if org-id
                  ["SELECT * FROM \"Сессия\" WHERE logout_time IS NULL AND organization_id = ? ORDER BY login_time DESC"
                   org-id]
                  ["SELECT * FROM \"Сессия\" WHERE logout_time IS NULL ORDER BY login_time DESC"]))
    (catch Exception e
      (logger/log-error e "Ошибка при получении активных сессий")
      [])))

(defn get-session-count-by-day
  [& [days org-id]]
  (try
    (jdbc/query db-spec
                (if org-id
                  [(str "SELECT " (date-day-sql) " as day, COUNT(*) as count, SUM(CASE WHEN success THEN 1 ELSE 0 END) as success_count
                    FROM \"Сессия\"
                    WHERE login_time >= " (days-ago-sql) " AND organization_id = ?
                    GROUP BY day ORDER BY day DESC")
                   (str (- (or days 7))) org-id]
                  [(str "SELECT " (date-day-sql) " as day, COUNT(*) as count, SUM(CASE WHEN success THEN 1 ELSE 0 END) as success_count
                    FROM \"Сессия\"
                    WHERE login_time >= " (days-ago-sql) "
                    GROUP BY day ORDER BY day DESC")
                   (str (- (or days 7)))]))
    (catch Exception e
      (logger/log-error e "Ошибка при получении статистики сессий")
      [])))

(defn- format-session
  [row]
  (when row
    {:id (:id row)
     :user_id (:user_id row)
     :username (:username row)
     :login_time (str (:login_time row))
     :logout_time (str (:logout_time row))
     :ip_address (:ip_address row)
     :user_agent (:user_agent row)
     :success (if (pg?)
                (boolean (:success row))
                (pos? (:success row 0)))
     :fail_reason (:fail_reason row)
     :organization_id (:organization_id row)
      :duration (when (and (:login_time row) (:logout_time row))
                  (let [start (java.time.LocalDateTime/parse (.replace (str (:login_time row)) " " "T"))
                        end (java.time.LocalDateTime/parse (.replace (str (:logout_time row)) " " "T"))
                        diff (java.time.Duration/between start end)]
                    (str (.toMinutes diff) " мин")))}))

(defn format-sessions
  [rows]
  (mapv format-session rows))

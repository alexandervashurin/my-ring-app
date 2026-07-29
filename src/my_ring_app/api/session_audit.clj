(ns my-ring-app.api.session-audit
  (:require [my-ring-app.session-audit :as session-audit]
            [my-ring-app.auth :as auth]
            [my-ring-app.logger :as logger]
            [my-ring-app.util :as util]))

(defn get-sessions-api
  [request]
  (try
    (let [user (:identity request)
          org-id (auth/get-org-id request)
          limit (util/parse-int (get-in request [:params :limit]) 50)
          is-admin (= "admin" (:role user))
          sessions (if is-admin
                     (session-audit/get-recent-sessions limit org-id)
                     (session-audit/get-user-sessions (:id user) limit))]
      (logger/log-info (format "API: GET /api/sessions (user: %s, found: %d)" (:username user) (count sessions)))
      (util/json-ok (session-audit/format-sessions sessions)))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении сессий")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

(defn get-failed-logins-api
  [request]
  (try
    (let [org-id (auth/get-org-id request)
          limit (util/parse-int (get-in request [:params :limit]) 20)
          sessions (session-audit/get-failed-logins limit org-id)]
      (logger/log-info (format "API: GET /api/sessions/failed (found: %d)" (count sessions)))
      (util/json-ok (session-audit/format-sessions sessions)))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении неудачных попыток входа")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

(defn get-active-sessions-api
  [request]
  (try
    (let [org-id (auth/get-org-id request)
          sessions (session-audit/get-active-sessions org-id)]
      (logger/log-info (format "API: GET /api/sessions/active (found: %d)" (count sessions)))
      (util/json-ok (session-audit/format-sessions sessions)))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении активных сессий")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

(defn get-session-stats-api
  [request]
  (try
    (let [org-id (auth/get-org-id request)
          days (util/parse-int (get-in request [:params :days]) 7)
          stats (session-audit/get-session-count-by-day days org-id)]
      (logger/log-info (format "API: GET /api/sessions/stats (days: %d)" days))
      (util/json-ok stats))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении статистики сессий")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

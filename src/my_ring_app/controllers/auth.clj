(ns my-ring-app.controllers.auth
  "Контроллеры аутентификации"
  (:require [ring.util.response :as resp]
            [my-ring-app.auth :as auth]
            [my-ring-app.session-audit :as session-audit]
            [my-ring-app.views.layout :as layout]
            [my-ring-app.views.auth :as auth-views]
            [my-ring-app.logger :as logger]
            [clojure.string :as str]))

;; ======================================================================
;; Защита от brute-force атак
;; ======================================================================

(def ^:private max-failed-attempts 5)
(def ^:private lockout-duration-ms (* 15 60 1000))

(def ^:private failed-logins
  "Атом для хранения неудачных попыток входа {username {:count N :last-attempt timestamp}}"
  (atom {}))

(defn- record-failed-login!
  "Запись неудачной попытки входа"
  [username]
  (swap! failed-logins update username
         (fn [entry]
           (let [now (System/currentTimeMillis)
                 entry (or entry {:count 0 :last-attempt 0})]
             (if (> (- now (:last-attempt entry)) lockout-duration-ms)
               {:count 1 :last-attempt now}
               {:count (inc (:count entry)) :last-attempt now})))))

(defn- clear-failed-logins!
  "Очистка счётчика неудачных попыток при успешном входе"
  [username]
  (swap! failed-logins dissoc username))

(defn- is-locked-out?
  "Проверка, заблокирован ли аккаунт"
  [username]
  (let [entry (get @failed-logins username)]
    (when entry
      (and (>= (:count entry) max-failed-attempts)
           (< (- (System/currentTimeMillis) (:last-attempt entry)) lockout-duration-ms)))))

(defn- safe-redirect-url
  "Проверка URL для безопасного редиректа (только внутренние ссылки)"
  [url]
  (when url
    (let [url (str/trim url)]
      (when (and (str/starts-with? url "/")
                 (not (str/starts-with? url "//"))
                 (not (str/includes? url ":")))
        url))))

(defn login-page
  "Страница входа"
  [request]
  (let [error (:error (:query-params request))
        redirect-url (:redirect-url (:session request))]
    (logger/log-info "Открыта страница входа")
    (-> (resp/response (auth-views/render-login-page error redirect-url))
        (resp/content-type "text/html; charset=utf-8"))))

(defn login-submit
  "Обработка формы входа"
  [request]
  (let [username (str/trim (:username (:params request)))
        password (:password (:params request))
        redirect-url (or (safe-redirect-url (:redirect-url (:session request))) "/")
        ip-address (get-in request [:headers "x-forwarded-for"] (:remote-addr request "unknown"))
        user-agent (get-in request [:headers "user-agent"])]
    (logger/log-info (format "Попытка входа пользователя: %s" username))

    (cond
      ;; Пустые данные
      (or (empty? username) (empty? password))
      (do
        (logger/log-warn (format "Попытка входа с пустыми данными: %s" username))
        (-> (resp/redirect (str "/login?error=empty"))
            (resp/status 302)))

      ;; Блокировка при множественных неудачных попытках
      (is-locked-out? username)
      (do
        (logger/log-warn (format "Аккаунт %s заблокирован из-за множественных неудачных попыток" username))
        (-> (resp/redirect (str "/login?error=locked"))
            (resp/status 302)))

      ;; Аутентификация
      :else
      (if-let [user (auth/authenticate username password {:ip-address ip-address :user-agent user-agent})]
        ;; Успешный вход
        (do
          (clear-failed-logins! username)
          (logger/log-info (format "Пользователь %s успешно вошёл в систему" username))
          (-> (resp/redirect redirect-url)
              (resp/status 302)
              (assoc :session {:user user
                               :redirect-url nil})))
        ;; Ошибка аутентификации
        (do
          (record-failed-login! username)
          (logger/log-warn (format "Неудачная попытка входа: %s" username))
          (-> (resp/redirect (str "/login?error=invalid"))
              (resp/status 302)))))))

(defn logout
  "Выход из системы"
  [request]
  (let [user (:identity request)
        session-id (:session_id user)]
    (when user
      (session-audit/log-logout! session-id)
      (logger/log-audit "LOGOUT" "User" (:id user)
                        (format "Пользователь %s вышел из системы" (:username user)))
      (logger/log-info (format "Пользователь %s вышел из системы" (:username user))))
    (-> (resp/redirect "/")
        (resp/status 302)
        (assoc :session nil))))

(defn profile-page
  "Страница профиля пользователя"
  [request]
  (let [user (:identity request)]
    (if user
      (do
        (logger/log-info (format "Открыт профиль пользователя %s" (:username user)))
        (-> (resp/response (auth-views/render-profile-page user))
            (resp/content-type "text/html; charset=utf-8")))
      (-> (resp/redirect "/login")
          (resp/status 302)))))

(defn change-password
  "Смена пароля пользователя"
  [request]
  (let [user (:identity request)
        current-password (:current-password (:params request))
        new-password (:new-password (:params request))
        confirm-password (:confirm-password (:params request))]
    (if (not user)
      (-> (resp/redirect "/login")
          (resp/status 302))
      (do
        (logger/log-info (format "Попытка смены пароля для %s" (:username user)))

        ;; Проверка текущего пароля
        (if-let [auth-user (auth/authenticate (:username user) current-password)]
          ;; Проверка совпадения новых паролей
          (if (= new-password confirm-password)
            ;; Проверка длины пароля
            (if (>= (count new-password) 6)
              ;; Смена пароля
              (let [result (auth/update-user! (:id user) {:password new-password})]
                (if (:success result)
                  (do
                    (logger/log-info (format "Пароль изменён для %s" (:username user)))
                    (-> (resp/redirect "/profile?success=password_changed")
                        (resp/status 302)))
                  (-> (resp/redirect "/profile?error=change_failed")
                      (resp/status 302))))
              (-> (resp/redirect "/profile?error=short_password")
                  (resp/status 302)))
            (-> (resp/redirect "/profile?error=passwords_mismatch")
                (resp/status 302)))
          (-> (resp/redirect "/profile?error=wrong_password")
              (resp/status 302)))))))

(defn sessions-page
  "Страница истории сессий"
  [request]
  (let [user (:identity request)
        org-id (auth/get-org-id request)
        is-admin (= "admin" (:role user))
        sessions (if is-admin
                   (session-audit/get-recent-sessions 100 org-id)
                   (session-audit/get-user-sessions (:id user) 100))
        active-sessions (when is-admin (session-audit/get-active-sessions org-id))
        failed-logins (session-audit/get-failed-logins 20 org-id)]
    (logger/log-info (format "Открыта страница истории сессий (user: %s)" (:username user)))
    (-> (resp/response (auth-views/render-sessions-page user sessions active-sessions failed-logins))
        (resp/content-type "text/html; charset=utf-8"))))

(defn access-denied
  "Страница доступа запрещён"
  [request]
  (-> (resp/response (auth-views/render-access-denied (:identity request)))
      (resp/status 403)
      (resp/content-type "text/html; charset=utf-8")))

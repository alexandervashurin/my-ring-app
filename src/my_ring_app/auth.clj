(ns my-ring-app.auth
  "Модуль аутентификации и авторизации пользователей"
  (:require [clojure.java.jdbc :as jdbc]
            [my-ring-app.config :as config :refer [db-spec]]
            [my-ring-app.logger :as logger]
            [buddy.hashers :as hashers]
            [ring.util.response :as resp]
            [clojure.string :as str]
            [java-time :as time]))

;; ======================================================================
;; Константы и роли
;; ======================================================================

(def roles
  "Доступные роли пользователей"
  {:admin "Администратор — полный доступ"
   :manager "Менеджер — CRUD работников, зарплата, учёт времени"
   :viewer "Наблюдатель — только просмотр"
   :hr "HR — CRUD работников, без зарплаты"})

(defn get-role-permissions
  "Получение прав для роли"
  [role]
  (case role
    "admin" {:workers [:read :write :delete]
             :salary [:read :write]
             :work-time [:read :write]
             :users [:read :write]
             :dashboard [:read]
             :tables [:read]}
    "manager" {:workers [:read :write :delete]
               :salary [:read :write]
               :work-time [:read :write]
               :dashboard [:read]
               :tables [:read]}
    "hr" {:workers [:read :write :delete]
          :work-time [:read :write]
          :dashboard [:read]
          :tables [:read]}
    "viewer" {:workers [:read]
              :salary [:read]
              :work-time [:read]
              :dashboard [:read]
              :tables [:read]}
    {}))

;; ======================================================================
;; Функции работы с пользователями
;; ======================================================================

(defn get-user-by-username
  "Получение пользователя по имени пользователя"
  [username]
  (try
    (let [result (jdbc/query db-spec
                             ["SELECT * FROM Пользователь WHERE username = ? AND is_active = 1"
                              username])]
      (first result))
    (catch Exception e
      (logger/log-error e "Ошибка при получении пользователя" {:username username})
      nil)))

(defn get-user-by-id
  "Получение пользователя по ID"
  [id]
  (try
    (let [result (jdbc/query db-spec
                             ["SELECT * FROM Пользователь WHERE id = ? AND is_active = 1"
                              id])]
      (first result))
    (catch Exception e
      (logger/log-error e "Ошибка при получении пользователя по ID" {:id id})
      nil)))

(defn create-user
  "Создание нового пользователя"
  [username email password role]
  (try
    (let [password-hash (hashers/encrypt password)
          result (jdbc/insert! db-spec :Пользователь
                               {:username username
                                :email email
                                :password_hash password-hash
                                :role role
                                :is_active 1})]
      (logger/log-audit "CREATE" "User" (first result)
                        (format "Создан пользователь %s (роль: %s)" username role))
      {:success true :id (first result) :message "Пользователь создан"})
    (catch Exception e
      (logger/log-error e "Ошибка при создании пользователя"
                        {:username username :email email})
      {:success false :message "Внутренняя ошибка при создании пользователя"})))

(defn update-user!
  "Обновление данных пользователя.
   Разрешены только безопасные поля (whitelist) для защиты от mass assignment."
  [id data]
  (try
    (let [allowed-keys #{:password :email}
          safe-data (select-keys data allowed-keys)
          update-data (cond-> safe-data
                        (:password safe-data)
                        (assoc :password_hash (hashers/encrypt (:password safe-data))))
          clean-data (dissoc update-data :password)
          result (jdbc/update! db-spec :Пользователь clean-data ["id = ?" id])]
      (if (pos? result)
        (do
          (logger/log-audit "UPDATE" "User" id "Данные пользователя обновлены")
          {:success true :message "Пользователь обновлён"})
        {:success false :message "Пользователь не найден"}))
    (catch Exception e
      (logger/log-error e "Ошибка при обновлении пользователя" {:id id})
      {:success false :message "Внутренняя ошибка при обновлении пользователя"})))

(defn deactivate-user!
  "Деактивация пользователя (мягкое удаление)"
  [id]
  (try
    (let [result (jdbc/update! db-spec :Пользователь
                               {:is_active 0
                                :updated_at (str (time/local-date-time))}
                               ["id = ?" id])]
      (when (pos? result)
        (logger/log-audit "DELETE" "User" id "Пользователь деактивирован"))
      {:success true :message "Пользователь деактивирован"})
    (catch Exception e
      (logger/log-error e "Ошибка при деактивации пользователя" {:id id})
      {:success false :message "Внутренняя ошибка при деактивации пользователя"})))

(defn get-all-users
  "Получение списка всех активных пользователей"
  []
  (try
    (jdbc/query db-spec ["SELECT id, username, email, role, is_active, created_at, last_login
                          FROM Пользователь WHERE is_active = 1 ORDER BY username"])
    (catch Exception e
      (logger/log-error e "Ошибка при получении списка пользователей")
      [])))

;; ======================================================================
;; Аутентификация
;; ======================================================================

(defn authenticate
  "Проверка учётных данных пользователя
   Возвращает пользователя при успехе, nil при ошибке"
  [username password]
  (try
    (let [user (get-user-by-username username)]
      (if (and user
               (hashers/check password (:password_hash user)))
        (do
          ;; Обновляем last_login
          (jdbc/update! db-spec :Пользователь
                        {:last_login (str (time/local-date-time))}
                        ["id = ?" (:id user)])
          (logger/log-audit "LOGIN" "User" (:id user)
                            (format "Пользователь %s вошёл в систему" username))
          (logger/log-info (format "Успешная аутентификация: %s" username))
          {:id (:id user)
           :username (:username user)
           :email (:email user)
           :role (:role user)
           :permissions (get-role-permissions (:role user))})
        (do
          (when user
            (logger/log-warn (format "Неверный пароль для пользователя: %s" username)))
          nil)))
    (catch Exception e
      (logger/log-error e "Ошибка при аутентификации" {:username username})
      nil)))

;; ======================================================================
;; Middleware для сессионной аутентификации
;; ======================================================================

(defn wrap-authentication
  "Middleware для добавления пользователя из сессии в запрос.
   Переходит к БД для проверки is_active, чтобы деактивированные пользователи
   теряли сессию немедленно."
  [handler]
  (fn [request]
    (let [session (:session request {})
          session-user (:user session)
          user (when session-user
                 (get-user-by-id (:id session-user)))]
      (handler (assoc request :identity user :current-user user)))))

(defn authenticated?
  "Проверка, аутентифицирован ли пользователь"
  [request]
  (boolean (:identity request)))

;; ======================================================================
;; Авторизация
;; ======================================================================

(defn has-permission?
  "Проверка наличия права у пользователя"
  [user resource action]
  (let [role (:role user)
        permissions (get-role-permissions role)
        resource-permissions (get permissions resource)]
    (boolean (some #{action} resource-permissions))))

(defn require-authentication
  "Middleware требующий аутентификации"
  [handler]
  (fn [request]
    (if (authenticated? request)
      (handler request)
      (-> (resp/redirect "/login")
          (resp/status 302)))))

(defn require-role
  "Middleware требующий определённой роли"
  [handler & allowed-roles]
  (let [allowed (set allowed-roles)]
    (fn [request]
      (let [user (:identity request)]
        (if (and user (contains? allowed (:role user)))
          (handler request)
          (-> (resp/response "Доступ запрещён: недостаточно прав")
              (resp/status 403)
              (resp/content-type "text/html; charset=utf-8")))))))

(defn require-permission
  "Middleware требующий определённого права"
  [handler resource action]
  (fn [request]
    (let [user (:identity request)]
      (if (and user (has-permission? user resource action))
        (handler request)
        (-> (resp/response "Доступ запрещён: недостаточно прав")
            (resp/status 403)
            (resp/content-type "text/html; charset=utf-8"))))))

;; ======================================================================
;; Вспомогательные функции
;; ======================================================================

(defn validate-user-data
  "Валидация данных пользователя"
  [data]
  (let [errors (atom [])]
    ;; Проверка username
    (when (or (empty? (:username data))
              (str/blank? (:username data)))
      (swap! errors conj "Имя пользователя обязательно"))
    (when (and (:username data)
               (< (count (:username data)) 3))
      (swap! errors conj "Имя пользователя должно быть не менее 3 символов"))
    (when (and (:username data)
               (not (re-matches #"^[a-zA-Z0-9_]+$" (:username data))))
      (swap! errors conj "Имя пользователя может содержать только буквы, цифры и подчёркивание"))

    ;; Проверка email
    (when (or (empty? (:email data))
              (str/blank? (:email data)))
      (swap! errors conj "Email обязателен"))
    (when (and (:email data)
               (not (re-matches #"^[^@\s]+@[^@\s]+\.[^@\s]+$" (:email data))))
      (swap! errors conj "Неверный формат email"))

    ;; Проверка пароля
    (when (and (:password data)
               (< (count (:password data)) 6))
      (swap! errors conj "Пароль должен быть не менее 6 символов"))

    ;; Проверка роли
    (when (and (:role data)
               (not (#{"admin" "manager" "viewer" "hr"} (:role data))))
      (swap! errors conj "Неверная роль"))

    (if (empty? @errors)
      {:valid? true}
      {:valid? false :errors @errors})))

;; ======================================================================
;; Инициализация БД
;; ======================================================================

(defn init-db!
  "Инициализация таблицы пользователей (создание админа по умолчанию)"
  []
  (try
    ;; Проверяем, есть ли пользователи
    (let [users (jdbc/query db-spec ["SELECT COUNT(*) as count FROM Пользователь"])]
      (when (or (empty? users)
                (zero? (:count (first users))))
        ;; Создаём админа по умолчанию с безопасным паролем
        (let [admin-password (or (System/getenv "ADMIN_PASSWORD")
                                  (when (= "production" (:env config/app-config))
                                    (throw (IllegalStateException. "ADMIN_PASSWORD env var is required in production")))
                                  "changeme!")
              admin-email (or (System/getenv "ADMIN_EMAIL") "admin@example.com")
              result (create-user "admin" admin-email admin-password "admin")]
          (if (:success result)
            (logger/log-info "Создан пользователь admin по умолчанию. ПОЖАЛУЙСТА, смените пароль!")
            (logger/log-warn "Не удалось создать пользователя admin по умолчанию")))))
    (catch Exception e
      (logger/log-error e "Ошибка при инициализации таблицы пользователей"))))

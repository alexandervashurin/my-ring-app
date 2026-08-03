(ns my-ring-app.auth
  "Модуль аутентификации и авторизации пользователей"
  (:require [clojure.java.jdbc :as jdbc]
            [my-ring-app.config :as config :refer [db-spec url]]
            [my-ring-app.logger :as logger]
            [my-ring-app.session-audit :as session-audit]
            [buddy.hashers :as hashers]
            [ring.util.response :as resp]
            [my-ring-app.util :as util]
            [clojure.string :as str]))

;; ======================================================================
;; Константы и роли
;; ======================================================================

(def roles
  "Доступные глобальные роли пользователей"
  {:admin "Администратор — полный доступ"
   :manager "Менеджер — CRUD работников, зарплата, учёт времени"
   :viewer "Наблюдатель — только просмотр"
   :hr "HR — CRUD работников, без зарплаты"})

(def org-roles
  "Доступные роли на уровне организации.
   org_role переопределяет глобальную роль в рамках организации."
  {:org_admin "Администратор организации — полный доступ внутри организации"
   :org_manager "Менеджер организации — управление работниками, зарплатой, учётом времени"
   :org_hr "HR организации — управление работниками, без зарплаты"
   :org_viewer "Наблюдатель организации — только просмотр"})

(defn get-role-permissions
  "Получение прав для глобальной роли"
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

(defn get-org-role-permissions
  "Получение прав для роли организации.
   org_role переопределяет глобальную роль в рамках организации."
  [org-role]
  (case org-role
    "org_admin" {:workers [:read :write :delete]
                 :salary [:read :write]
                 :work-time [:read :write]
                 :users [:read :write]
                 :dashboard [:read]
                 :tables [:read]}
    "org_manager" {:workers [:read :write :delete]
                   :salary [:read :write]
                   :work-time [:read :write]
                   :dashboard [:read]
                   :tables [:read]}
    "org_hr" {:workers [:read :write :delete]
              :work-time [:read :write]
              :dashboard [:read]
              :tables [:read]}
    "org_viewer" {:workers [:read]
                  :salary [:read]
                  :work-time [:read]
                  :dashboard [:read]
                  :tables [:read]}
    {}))

(defn get-effective-permissions
  "Получение эффективных прав пользователя.
   Если у пользователя есть org_role, она переопределяет глобальную роль
   в рамках контекста организации. Глобальный admin имеет все права везде."
  [user]
  (let [global-role (:role user)
        org-role (:org_role user)]
    (if (= "admin" global-role)
      {:workers [:read :write :delete]
       :salary [:read :write]
       :work-time [:read :write]
       :users [:read :write]
       :dashboard [:read]
       :tables [:read]}
      (if org-role
        (get-org-role-permissions org-role)
        (get-role-permissions global-role)))))

(defn has-permission?
  "Проверка наличия конкретного права у пользователя.
   Пример: (has-permission? user :workers :write)
   Возвращает true/false."
  [user resource action]
  (let [perms (get-effective-permissions user)
        resource-perms (get perms resource)]
    (boolean (some #{action} resource-perms))))

;; ======================================================================
;; Константы и настройки организации
;; ======================================================================

(def ^:private default-org-id 1)

;; ======================================================================
;; Функции работы с ролями организации
;; ======================================================================

(defn update-user-org-role!
  "Обновление org_role пользователя.
   Только администратор организации может менять роль.
   user-id должен принадлежать org-id — защита от IDOR."
  [user-id org-id org-role]
  (try
    (let [valid-roles #{nil "org_admin" "org_manager" "org_hr" "org_viewer"}]
      (if (and org-role (not (contains? valid-roles org-role)))
        {:success false :message "Неверная роль организации"}
        (let [user-org (first (jdbc/query db-spec
                                          ["SELECT organization_id FROM \"Пользователь\" WHERE id = ?" user-id]))]
          (if (not= org-id (:organization_id user-org))
            {:success false :message "Пользователь не принадлежит организации"}
            (let [result (jdbc/update! db-spec :Пользователь
                                       {:org_role org-role
                                        :updated_at (util/now-timestamp)}
                                       ["id = ?" user-id])
                  affected (first result)]
              (if (pos? affected)
                (do
                  (logger/log-audit "UPDATE" "User" user-id
                                    (format "Роль организации изменена на %s" (or org-role "по умолчанию")))
                  {:success true :message "Роль организации обновлена"})
                {:success false :message "Пользователь не найден"}))))))
    (catch Exception e
      (logger/log-error e "Ошибка при обновлении роли организации" {:user-id user-id})
      {:success false :message "Внутренняя ошибка при обновлении роли"})))

(defn get-org-users
  "Получение списка пользователей организации с ролями"
  [org-id]
  (try
    (vec (jdbc/query db-spec
                     ["SELECT id, username, email, role, org_role, organization_id, is_active, created_at, last_login
                      FROM \"Пользователь\"
                      WHERE organization_id = ? AND is_active = true
                      ORDER BY CASE WHEN role = 'admin' THEN 0 ELSE 1 END, username"
                      org-id]))
    (catch Exception e
      (logger/log-error e "Ошибка при получении пользователей организации" {:org-id org-id})
      [])))

;; ======================================================================
;; Функции работы с организациями
;; ======================================================================

(defn get-organization-by-id
  "Получение организации по ID"
  [org-id]
  (try
    (first (jdbc/query db-spec ["SELECT * FROM \"Организация\" WHERE id = ? AND is_active = true" org-id]))
    (catch Exception e
      (logger/log-error e "Ошибка при получении организации" {:org-id org-id})
      nil)))

(defn get-all-organizations
  "Получение списка всех активных организаций"
  []
  (try
    (vec (jdbc/query db-spec ["SELECT * FROM \"Организация\" WHERE is_active = true ORDER BY name"]))
    (catch Exception e
      (logger/log-error e "Ошибка при получении списка организаций")
      [])))

(defn create-organization
  "Создание новой организации"
  [{:keys [name inn phone email address]}]
  (try
    (let [result (jdbc/insert! db-spec :Организация
                               {:name name
                                :inn inn
                                :phone phone
                                :email email
                                 :address address
                                 :is_active true})
          new-id (util/extract-id result)]
      (logger/log-audit "CREATE" "Organization" new-id
                        (format "Создана организация '%s'" name))
      {:success true :id new-id :message "Организация создана"})
    (catch Exception e
      (logger/log-error e "Ошибка при создании организации" {:name name})
      {:success false :message "Ошибка при создании организации"})))

(defn update-organization
  "Обновление данных организации"
  [org-id data]
  (try
    (let [allowed-keys #{:name :inn :phone :email :address}
          safe-data (select-keys data allowed-keys)
          result (jdbc/update! db-spec :Организация safe-data ["id = ?" org-id])
          affected (first result)]
      (if (pos? affected)
        (do
          (logger/log-audit "UPDATE" "Organization" org-id "Организация обновлена")
          {:success true :message "Организация обновлена"})
        {:success false :message "Организация не найдена"}))
    (catch Exception e
      (logger/log-error e "Ошибка при обновлении организации" {:org-id org-id})
      {:success false :message "Ошибка при обновлении организации"})))

(defn deactivate-organization
  "Деактивация организации (мягкое удаление)"
  [org-id]
  (try
    (let [result (jdbc/update! db-spec :Организация
                                     {:is_active false :updated_at (util/now-timestamp)}
                                     ["id = ?" org-id])
          affected (first result)]
      (when (pos? affected)
        (logger/log-audit "DELETE" "Organization" org-id "Организация деактивирована"))
      {:success true :message "Организация деактивирована"})
    (catch Exception e
      (logger/log-error e "Ошибка при деактивации организации" {:org-id org-id})
      {:success false :message "Ошибка при деактивации организации"})))

;; ======================================================================
;; Функции работы с пользователями
;; ======================================================================

(defn get-user-by-username
  "Получение пользователя по имени пользователя"
  [username]
  (try
    (let [result (jdbc/query db-spec
                             ["SELECT u.*, o.name as org_name
                              FROM \"Пользователь\" u
                              LEFT JOIN \"Организация\" o ON u.organization_id = o.id
                              WHERE u.username = ? AND u.is_active = true"
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
                             ["SELECT u.*, o.name as org_name
                              FROM \"Пользователь\" u
                              LEFT JOIN \"Организация\" o ON u.organization_id = o.id
                              WHERE u.id = ? AND u.is_active = true"
                              id])]
      (first result))
    (catch Exception e
      (logger/log-error e "Ошибка при получении пользователя по ID" {:id id})
      nil)))

(defn create-user
  "Создание нового пользователя"
  [username email password role & [org-id org-role]]
  (try
    (let [password-hash (hashers/encrypt password)
          result (jdbc/insert! db-spec :Пользователь
                               {:username username
                                :email email
                                :password_hash password-hash
                                :role role
                                :org_role org-role
                                 :organization_id (or org-id default-org-id)
                                 :is_active true})
          new-id (util/extract-id result)]
      (logger/log-audit "CREATE" "User" new-id
                        (format "Создан пользователь %s (роль: %s, org_role: %s, org: %d)" username role (or org-role "-") (or org-id default-org-id)))
      {:success true :id new-id :message "Пользователь создан"})
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
          result (jdbc/update! db-spec :Пользователь clean-data ["id = ?" id])
          affected (first result)]
      (if (and affected (pos? affected))
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
                                     {:is_active false
                                      :updated_at (util/now-timestamp)}
                                     ["id = ?" id])
          affected (first result)]
      (when (pos? affected)
        (logger/log-audit "DELETE" "User" id "Пользователь деактивирован"))
      {:success true :message "Пользователь деактивирован"})
    (catch Exception e
      (logger/log-error e "Ошибка при деактивации пользователя" {:id id})
      {:success false :message "Внутренняя ошибка при деактивации пользователя"})))

(defn get-all-users
  "Получение списка активных пользователей. Если org-id не nil — фильтрует по организации."
  ([] (get-all-users nil))
  ([org-id]
   (try
      (vec
        (if org-id
          (jdbc/query db-spec ["SELECT id, username, email, role, org_role, organization_id, is_active, created_at, last_login
                                FROM \"Пользователь\" WHERE is_active = true AND organization_id = ? ORDER BY username"
                               org-id])
          (jdbc/query db-spec ["SELECT id, username, email, role, org_role, organization_id, is_active, created_at, last_login
                                FROM \"Пользователь\" WHERE is_active = true ORDER BY username"])))
     (catch Exception e
       (logger/log-error e "Ошибка при получении списка пользователей")
       []))))

;; ======================================================================
;; Аутентификация
;; ======================================================================

(defn authenticate
  "Проверка учётных данных пользователя
   Возвращает пользователя при успехе, nil при ошибке.
   Опционально принимает options с :ip-address и :user-agent для логирования сессии."
  ([username password] (authenticate username password nil))
  ([username password options]
   (try
     (let [user (get-user-by-username username)
           ip-address (:ip-address options)
           user-agent (:user-agent options)]
        (if (and user
                 (hashers/check password (:password_hash user)))
         (do
           ;; Обновляем last_login
           (jdbc/update! db-spec :Пользователь
                         {:last_login (util/now-timestamp)}
                         ["id = ?" (:id user)])
           ;; Логирование успешного входа в БД
           (let [session-id (session-audit/log-login! (:id user) username ip-address user-agent
                                                       (:organization_id user) true)]
             (logger/log-audit "LOGIN" "User" (:id user)
                               (format "Пользователь %s вошёл в систему (org: %s)"
                                       username (or (:org_name user) "default")))
             (logger/log-info (format "Успешная аутентификация: %s (org: %s)" username (or (:org_name user) "default")))
             {:id (:id user)
              :username (:username user)
              :email (:email user)
              :role (:role user)
              :org_role (:org_role user)
              :organization_id (:organization_id user)
              :org_name (:org_name user)
              :permissions (get-effective-permissions user)
              :session_id session-id}))
          (do
           (when user
             ;; Логирование неудачной попытки входа
             (session-audit/log-login! (:id user) username ip-address user-agent
                                       (:organization_id user) false "Неверный пароль")
             (logger/log-warn (format "Неверный пароль для пользователя: %s" username)))
           nil)))
     (catch Exception e
       (logger/log-error e "Ошибка при аутентификации" {:username username})
       nil))))

;; ======================================================================
;; Middleware для сессионной аутентификации
;; ======================================================================

(defn get-org-id
  "Получение organization_id из запроса (из текущего пользователя)"
  [request]
  (or (get-in request [:identity :organization_id]) default-org-id))

(defn wrap-org-context
  "Middleware для добавления organization_id в request из сессии пользователя.
   Все модули, работающие с данными организации, должны использовать (:org-id request)."
  [handler]
  (fn [request]
    (let [org-id (get-org-id request)]
      (handler (assoc request :org-id org-id)))))

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

(defn require-authentication
  "Middleware требующий аутентификации"
  [handler]
  (fn [request]
    (if (authenticated? request)
      (handler request)
      (-> (resp/redirect (url "/login"))
          (resp/status 302)))))

(defn require-role
  "Middleware требующий определённой глобальной роли.
   Глобальный admin всегда проходит. Также проверяет org_role."
  [handler & allowed-roles]
  (let [allowed (set allowed-roles)]
    (fn [request]
      (let [user (:identity request)
            effective-role (or (:org_role user) (:role user))]
        (if (and user (or (= "admin" (:role user))
                          (contains? allowed effective-role)
                          (contains? allowed (:role user))))
          (handler request)
          (util/html-response "Доступ запрещён: недостаточно прав" 403))))))

(defn require-org-role
  "Middleware требующий определённой роли организации.
   Глобальный admin всегда проходит. Если у пользователя есть org_role,
   проверяется соответствие org_role, иначе — глобальная роль."
  [handler & allowed-roles]
  (let [allowed (set allowed-roles)]
    (fn [request]
      (let [user (:identity request)
            effective-role (or (:org_role user) (:role user))]
        (if (and user (or (= "admin" (:role user))
                          (contains? allowed effective-role)))
          (handler request)
          (util/html-response "Доступ запрещён: недостаточно прав" 403))))))

(defn require-own-org-scope
  "Middleware: ограничивает доступ к организации из route-параметра :id
   только своей организацией. Глобальный admin может работать с любой
   организацией. Защищает от IDOR между организациями."
  [handler]
  (fn [request]
    (let [user (:identity request)
          target-org-id (some-> (or (get-in request [:params :id])
                                    (get-in request [:route-params :id]))
                                (util/parse-int nil))
          user-org-id (get-in user [:organization_id])]
      (if (or (= "admin" (:role user))
              (and user target-org-id (= target-org-id user-org-id)))
        (handler request)
        (util/html-response "Доступ запрещён: чужая организация" 403)))))

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
    (let [users (jdbc/query db-spec ["SELECT COUNT(*) as count FROM \"Пользователь\""])]
      (when (or (empty? users)
                (zero? (:count (first users))))
        ;; Создаём админа по умолчанию с безопасным паролем
        (let [admin-password (or (System/getProperty "ADMIN_PASSWORD") (System/getenv "ADMIN_PASSWORD")
                                  (when (= "production" (:env config/app-config))
                                    (throw (IllegalStateException. "ADMIN_PASSWORD env var is required in production")))
                                  "changeme!")
              admin-email (or (System/getProperty "ADMIN_EMAIL") (System/getenv "ADMIN_EMAIL") "admin@example.com")
              is-default-password (and (nil? (System/getProperty "ADMIN_PASSWORD")) (nil? (System/getenv "ADMIN_PASSWORD"))
                                       (not= "production" (:env config/app-config)))
              result (create-user "admin" admin-email admin-password "admin" default-org-id)]
          (if (:success result)
            (do
              (logger/log-info "Создан пользователь admin по умолчанию (org: 1)")
              (when is-default-password
                (logger/log-warn "Используется пароль по умолчанию 'changeme!'. Задайте переменную окружения ADMIN_PASSWORD или смените пароль после первого входа!")))
            (logger/log-warn "Не удалось создать пользователя admin по умолчанию")))))
    (catch Exception e
      (logger/log-error e "Ошибка при инициализации таблицы пользователей"))))

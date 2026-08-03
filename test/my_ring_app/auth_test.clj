(ns my-ring-app.auth-test
  "Тесты для модуля аутентификации и авторизации"
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [my-ring-app.auth :as auth]
            [my-ring-app.config :refer [db-spec]]
            [my-ring-app.test-helper :as helper]))

;; ======================================================================
;; Тестовые данные
;; ======================================================================

(def test-username (str "test_user_" (System/currentTimeMillis)))
(def test-email (str test-username "@test.com"))
(def test-password "test_password_123!")

;; ======================================================================
;; Фикстура: ensure DB initialized + clean up test user
;; ======================================================================

(defn cleanup-test-user [f]
  (try (jdbc/delete! db-spec :Пользователь ["username = ?" test-username]) (catch Exception _))
  (f)
  (try (jdbc/delete! db-spec :Пользователь ["username = ?" test-username]) (catch Exception _)))

(use-fixtures :once helper/setup-db)
(use-fixtures :each cleanup-test-user)

;; ======================================================================
;; Тесты: get-role-permissions
;; ======================================================================

(deftest test-get-role-permissions-admin
  (testing "Администратор имеет все права"
    (let [perms (auth/get-role-permissions "admin")]
      (is (some #{:read} (:workers perms)))
      (is (some #{:write} (:workers perms)))
      (is (some #{:delete} (:workers perms)))
      (is (some #{:read} (:salary perms)))
      (is (some #{:write} (:salary perms)))
      (is (some #{:read} (:users perms)))
      (is (some #{:write} (:users perms))))))

(deftest test-get-role-permissions-viewer
  (testing "Наблюдатель имеет только права чтения"
    (let [perms (auth/get-role-permissions "viewer")]
      (is (some #{:read} (:workers perms)))
      (is (nil? (some #{:write} (:workers perms))))
      (is (some #{:read} (:salary perms)))
      (is (nil? (some #{:write} (:salary perms)))))))

(deftest test-get-role-permissions-unknown
  (testing "Неизвестная роль возвращает пустые права"
    (let [perms (auth/get-role-permissions "unknown_role")]
      (is (empty? perms)))))

;; ======================================================================
;; Тесты: create-user / get-user-by-username
;; ======================================================================

(deftest test-create-user
  (testing "Создание нового пользователя"
    (let [result (auth/create-user test-username test-email test-password "viewer")]
      (is (true? (:success result)))
      (is (number? (:id result)))
      (is (pos? (:id result))))))

(deftest test-get-user-by-username
  (testing "Получение пользователя по username"
    ;; Ensure user exists
    (auth/create-user test-username test-email test-password "viewer")
    (let [user (auth/get-user-by-username test-username)]
      (is (some? user))
      (is (= test-username (:username user)))
      (is (= test-email (:email user)))
      (is (= "viewer" (:role user)))
      (is (= 1 (:is_active user))))))

(deftest test-get-user-by-username-not-found
  (testing "Несуществующий пользователь возвращает nil"
    (let [user (auth/get-user-by-username "nonexistent_user_xyz")]
      (is (nil? user)))))

;; ======================================================================
;; Тесты: authenticate
;; ======================================================================

(deftest test-authenticate-success
  (testing "Успешная аутентификация"
    (auth/create-user test-username test-email test-password "viewer")
    (let [result (auth/authenticate test-username test-password)]
      (is (some? result))
      (is (= test-username (:username result)))
      (is (= "viewer" (:role result)))
      (is (map? (:permissions result))))))

(deftest test-authenticate-wrong-password
  (testing "Неверный пароль возвращает nil"
    (auth/create-user test-username test-email test-password "viewer")
    (let [result (auth/authenticate test-username "wrong_password")]
      (is (nil? result)))))

(deftest test-authenticate-nonexistent-user
  (testing "Несуществующий пользователь возвращает nil"
    (let [result (auth/authenticate "nonexistent_user_xyz" "any_password")]
      (is (nil? result)))))

;; ======================================================================
;; Тесты: update-user!
;; ======================================================================

(deftest test-update-user-email
  (testing "Обновление email пользователя"
    (auth/create-user test-username test-email test-password "viewer")
    (let [user (auth/get-user-by-username test-username)
          new-email (str "updated_" test-email)
          result (auth/update-user! (:id user) {:email new-email})]
      (is (true? (:success result)))
      (let [updated (auth/get-user-by-username test-username)]
        (is (= new-email (:email updated)))))))

(deftest test-update-user-password
  (testing "Обновление пароля пользователя"
    (auth/create-user test-username test-email test-password "viewer")
    (let [user (auth/get-user-by-username test-username)
          new-password "new_secure_password_456!"
          result (auth/update-user! (:id user) {:password new-password})]
      (is (true? (:success result)))
      (let [auth-result (auth/authenticate test-username new-password)]
        (is (some? auth-result))))))

(deftest test-update-user-not-found
  (testing "Обновление несуществующего пользователя"
    (let [result (auth/update-user! 99999 {:email "test@test.com"})]
      (is (false? (:success result))))))

;; ======================================================================
;; Тесты: get-all-users
;; ======================================================================

(deftest test-get-all-users
  (testing "Получение списка пользователей"
    (auth/create-user test-username test-email test-password "viewer")
    (let [users (auth/get-all-users)]
      (is (vector? users))
      (is (pos? (count users))))))

(deftest test-get-all-users-by-org
  (testing "Получение пользователей по организации"
    (let [users (auth/get-all-users 1)]
      (is (vector? users))
      (is (pos? (count users))))))

;; ======================================================================
;; Тесты: Middleware
;; ======================================================================

(deftest test-authenticated?
  (testing "Проверка аутентификации"
    (is (false? (auth/authenticated? {})))
    (is (false? (auth/authenticated? {:identity nil})))
    (is (true? (auth/authenticated? {:identity {:id 1 :username "test"}})))))

(deftest test-get-org-id
  (testing "Получение org-id из запроса"
    (is (= 1 (auth/get-org-id {})))
    (is (= 5 (auth/get-org-id {:identity {:organization_id 5}})))))

(deftest test-wrap-org-context
  (testing "Middleware добавляет org-id в request"
    (let [handler (fn [request] {:org-id (:org-id request)})
          wrapped (auth/wrap-org-context handler)]
      (is (= {:org-id 1} (wrapped {})))
      (is (= {:org-id 5} (wrapped {:identity {:organization_id 5}}))))))

(deftest test-wrap-authentication
  (testing "Middleware добавляет пользователя из сессии"
    (auth/create-user test-username test-email test-password "viewer")
    (let [user (auth/get-user-by-username test-username)
          handler (fn [request] {:user (:identity request)})
          wrapped (auth/wrap-authentication handler)
          result (wrapped {:session {:user user}})]
      (is (some? (:user result)))
      (is (= test-username (get-in result [:user :username]))))))

(deftest test-wrap-authentication-no-session
  (testing "Middleware без сессии не добавляет пользователя"
    (let [handler (fn [request] {:user (:identity request)})
          wrapped (auth/wrap-authentication handler)
          result (wrapped {})]
      (is (nil? (:user result))))))

;; ======================================================================
;; Тесты: require-authentication
;; ======================================================================

(deftest test-require-authentication-unauthenticated
  (testing "Неаутентифицированный пользователь перенаправляется на /login"
    (let [handler (fn [request] {:status 200})
          wrapped (auth/require-authentication handler)
          result (wrapped {})]
      (is (= 302 (:status result)))
      (is (= "/login" (get-in result [:headers "Location"]))))))

(deftest test-require-authentication-authenticated
  (testing "Аутентифицированный пользователь проходит"
    (let [handler (fn [request] {:status 200})
          wrapped (auth/require-authentication handler)
          result (wrapped {:identity {:id 1}})]
      (is (= 200 (:status result))))))

;; ======================================================================
;; Тесты: require-role
;; ======================================================================

(deftest test-require-role-authorized
  (testing "Пользователь с правильной ролью проходит"
    (let [handler (fn [request] {:status 200})
          wrapped (auth/require-role handler "admin" "manager")
          result (wrapped {:identity {:role "admin"}})]
      (is (= 200 (:status result))))))

(deftest test-require-role-unauthorized
  (testing "Пользователь с неправильной ролью получает 403"
    (let [handler (fn [request] {:status 200})
          wrapped (auth/require-role handler "admin")
          result (wrapped {:identity {:role "viewer"}})]
      (is (= 403 (:status result))))))

(deftest test-require-role-no-user
  (testing "Неаутентифицированный пользователь получает 403"
    (let [handler (fn [request] {:status 200})
          wrapped (auth/require-role handler "admin")
          result (wrapped {})]
      (is (= 403 (:status result))))))

;; ======================================================================
;; Тесты: Organization CRUD
;; ======================================================================

(deftest test-get-organization-by-id
  (testing "Получение организации по ID"
    (let [org (auth/get-organization-by-id 1)]
      (is (some? org))
      (is (= 1 (:id org)))
      (is (string? (:name org))))))

(deftest test-get-all-organizations
  (testing "Получение списка организаций"
    (let [orgs (auth/get-all-organizations)]
      (is (vector? orgs))
      (is (pos? (count orgs))))))

(deftest test-create-and-deactivate-organization
  (testing "Создание и деактивация организации"
    (let [org-name (str "Test Org " (System/currentTimeMillis))
          result (auth/create-organization {:name org-name :inn "1234567890"})]
      (is (true? (:success result)))
      (is (number? (:id result)))
      (let [deact-result (auth/deactivate-organization (:id result))]
        (is (true? (:success deact-result)))))))

;; ======================================================================
;; Тесты: Organization Roles (org_role)
;; ======================================================================

(deftest test-get-org-role-permissions-org-admin
  (testing "Администратор организации имеет все права"
    (let [perms (auth/get-org-role-permissions "org_admin")]
      (is (some #{:read} (:workers perms)))
      (is (some #{:write} (:workers perms)))
      (is (some #{:delete} (:workers perms)))
      (is (some #{:read} (:salary perms)))
      (is (some #{:write} (:salary perms)))
      (is (some #{:read} (:users perms)))
      (is (some #{:write} (:users perms))))))

(deftest test-get-org-role-permissions-org-viewer
  (testing "Наблюдатель организации имеет только права чтения"
    (let [perms (auth/get-org-role-permissions "org_viewer")]
      (is (some #{:read} (:workers perms)))
      (is (nil? (some #{:write} (:workers perms))))
      (is (some #{:read} (:salary perms)))
      (is (nil? (some #{:write} (:salary perms)))))))

(deftest test-get-org-role-permissions-unknown
  (testing "Неизвестная org-роль возвращает пустые права"
    (let [perms (auth/get-org-role-permissions "unknown_role")]
      (is (empty? perms)))))

(deftest test-get-effective-permissions-global-admin
  (testing "Глобальный admin имеет все права независимо от org_role"
    (let [perms (auth/get-effective-permissions {:role "admin" :org_role nil})]
      (is (some #{:write} (:users perms))))))

(deftest test-get-effective-permissions-org-overrides-global
  (testing "org_role переопределяет глобальную роль"
    (let [user {:role "viewer" :org_role "org_admin"}
          perms (auth/get-effective-permissions user)]
      (is (some #{:write} (:workers perms)))
      (is (some #{:write} (:users perms))))))

(deftest test-get-effective-permissions-no-org-role
  (testing "Без org_role используются права глобальной роли"
    (let [user {:role "manager" :org_role nil}
          perms (auth/get-effective-permissions user)]
      (is (some #{:write} (:workers perms)))
      (is (nil? (some #{:write} (:users perms)))))))

(deftest test-has-permission-check
  (testing "Проверка конкретного права"
    (let [admin-user {:role "admin"}
          viewer-user {:role "viewer"}
          org-admin-user {:role "viewer" :org_role "org_admin"}]
      (is (true? (auth/has-permission? admin-user :workers :delete)))
      (is (false? (auth/has-permission? viewer-user :workers :write)))
      (is (true? (auth/has-permission? org-admin-user :users :read))))))

(deftest test-create-user-with-org-role
  (testing "Создание пользователя с org_role"
    (let [result (auth/create-user test-username test-email test-password "viewer" 1 "org_manager")
          user (auth/get-user-by-username test-username)]
      (is (true? (:success result)))
      (is (= "org_manager" (:org_role user))))))

(deftest test-update-user-org-role
  (testing "Обновление org_role пользователя"
    (auth/create-user test-username test-email test-password "viewer" 1 "org_viewer")
    (let [user (auth/get-user-by-username test-username)
          result (auth/update-user-org-role! (:id user) 1 "org_admin")]
      (is (true? (:success result)))
      (let [updated (auth/get-user-by-username test-username)]
        (is (= "org_admin" (:org_role updated)))))))

(deftest test-update-user-org-role-clear
  (testing "Сброс org_role в nil"
    (auth/create-user test-username test-email test-password "viewer" 1 "org_viewer")
    (let [user (auth/get-user-by-username test-username)
          result (auth/update-user-org-role! (:id user) 1 nil)]
      (is (true? (:success result)))
      (let [updated (auth/get-user-by-username test-username)]
        (is (nil? (:org_role updated)))))))

(deftest test-update-user-org-role-invalid
  (testing "Неверная org-роль отклоняется"
    (let [result (auth/update-user-org-role! 1 1 "invalid_role")]
      (is (false? (:success result))))))

(deftest test-update-user-org-role-cross-org
  (testing "Нельзя менять роль пользователя чужой организации (IDOR)"
    (auth/create-user test-username test-email test-password "viewer" 2 "org_viewer")
    (let [user (auth/get-user-by-username test-username)
          result (auth/update-user-org-role! (:id user) 1 "org_admin")]
      (is (false? (:success result)))
      (is (= "org_viewer" (:org_role (auth/get-user-by-username test-username)))))
    (let [result (auth/update-user-org-role! 99999 1 "org_admin")]
      (is (false? (:success result))))))

(deftest test-get-org-users
  (testing "Получение пользователей организации"
    (let [users (auth/get-org-users 1)]
      (is (vector? users))
      (is (every? #(= 1 (:organization_id %)) users)))))

(deftest test-require-org-role-allows-org-admin
  (testing "org_admin проходит через require-org-role"
    (let [handler (fn [request] {:status 200})
          wrapped (auth/require-org-role handler "org_admin")
          result (wrapped {:identity {:role "manager" :org_role "org_admin"}})]
      (is (= 200 (:status result))))))

(deftest test-require-org-role-blocks-viewer
  (testing "org_viewer не проходит require-org-role для org_admin"
    (let [handler (fn [request] {:status 200})
          wrapped (auth/require-org-role handler "org_admin")
          result (wrapped {:identity {:role "viewer" :org_role "org_viewer"}})]
      (is (= 403 (:status result))))))

(deftest test-require-org-role-allows-global-admin
  (testing "Глобальный admin проходит require-org-role"
    (let [handler (fn [request] {:status 200})
          wrapped (auth/require-org-role handler "org_admin")
          result (wrapped {:identity {:role "admin"}})]
      (is (= 200 (:status result))))))

(deftest test-require-role-with-org-role-override
  (testing "require-role использует org_role как effective-role (совпадает с allowed)"
    (let [handler (fn [request] {:status 200})
          wrapped (auth/require-role handler "org_admin")
          result (wrapped {:identity {:role "viewer" :org_role "org_admin"}})]
      (is (= 200 (:status result))))))

(deftest test-require-role-ignores-org-role-when-not-matching
  (testing "require-role с org_role не совпадающим с allowed возвращает 403"
    (let [handler (fn [request] {:status 200})
          wrapped (auth/require-role handler "admin" "manager")
          result (wrapped {:identity {:role "viewer" :org_role "org_manager"}})]
      (is (= 403 (:status result))))))

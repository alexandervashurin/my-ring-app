(ns my-ring-app.controllers.auth
  "Контроллеры аутентификации"
  (:require [ring.util.response :as resp]
            [my-ring-app.auth :as auth]
            [my-ring-app.views.layout :as layout]
            [my-ring-app.views.auth :as auth-views]
            [my-ring-app.logger :as logger]
            [clojure.string :as str]))

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
        redirect-url (or (:redirect-url (:session request)) "/")]
    (logger/log-info (format "Попытка входа пользователя: %s" username))

    (cond
      ;; Пустые данные
      (or (empty? username) (empty? password))
      (-> (resp/redirect (str "/login?error=empty"))
          (resp/status 302))

      ;; Аутентификация
      :else
      (if-let [user (auth/authenticate username password)]
        ;; Успешный вход
        (do
          (logger/log-info (format "Пользователь %s успешно вошёл в систему" username))
          (-> (resp/redirect redirect-url)
              (resp/status 302)
              (assoc :session {:user user
                               :redirect-url nil})))
        ;; Ошибка аутентификации
        (do
          (logger/log-warn (format "Неудачная попытка входа: %s" username))
          (-> (resp/redirect (str "/login?error=invalid"))
              (resp/status 302)))))))

(defn logout
  "Выход из системы"
  [request]
  (let [user (:identity request)]
    (when user
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

(defn access-denied
  "Страница доступа запрещён"
  [request]
  (-> (resp/response (auth-views/render-access-denied (:identity request)))
      (resp/status 403)
      (resp/content-type "text/html; charset=utf-8")))

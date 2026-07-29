(ns my-ring-app.views.auth
  "Представления аутентификации"
  (:require [my-ring-app.views.layout :refer [wrap-html html-escape csrf-field]]
            [my-ring-app.config :refer [url]]
            [clojure.string :as str]))

(defn render-login-page
  "Рендер страницы входа"
  [error redirect-url]
  (let [error-messages {:empty "Введите имя пользователя и пароль"
                        :invalid "Неверное имя пользователя или пароль"}
        error-text (when error (get error-messages (keyword error)))]
    (wrap-html
     (str "<div class='login-container'>"
          "<div class='login-box'>"
          "<h1>🔐 Вход в систему</h1>"
          "<p class='login-subtitle'>Система управления персоналом</p>"

          (when error-text
            (str "<div class='error-message'>" error-text "</div>"))

          "<form method='POST' action='" (url "/login") "' class='login-form'>"
          (csrf-field)
          "<div class='form-group'>"
          "<label for='username'>Имя пользователя</label>"
          "<input type='text' id='username' name='username' required "
          "placeholder='Введите имя пользователя' autofocus>"
          "</div>"

          "<div class='form-group'>"
          "<label for='password'>Пароль</label>"
          "<input type='password' id='password' name='password' required "
          "placeholder='Введите пароль'>"
          "</div>"

          "<button type='submit' class='btn btn-primary btn-block'>Войти</button>"
          "</form>"

          "<div class='login-footer'>"
          "</div>"
          "</div>"
          "</div>")

     "Вход в систему"
     nil
     true)))

(defn render-profile-page
  "Рендер страницы профиля"
  [user]
  (wrap-html
   (str "<div class='profile-container'>"
        "<h1>👤 Профиль пользователя</h1>"

        "<div class='profile-card'>"
        "<h2>Информация о пользователе</h2>"
        "<table class='data-table'>"
        "<tr><td>ID</td><td>" (html-escape (str (:id user))) "</td></tr>"
        "<tr><td>Имя пользователя</td><td>" (html-escape (:username user)) "</td></tr>"
        "<tr><td>Email</td><td>" (html-escape (:email user)) "</td></tr>"
        "<tr><td>Роль</td><td>" (html-escape (get {:admin "Администратор"
                                                    :manager "Менеджер"
                                                    :viewer "Наблюдатель"
                                                    :hr "HR-специалист"}
                                                  (keyword (:role user)) (:role user))) "</td></tr>"
        "</table>"
        "</div>"

        "<div class='profile-card'>"
        "<h2>🔑 Смена пароля</h2>"
        "<form method='POST' action='" (url "/change-password") "' class='profile-form'>"
        (csrf-field)
        "<div class='form-group'>"
        "<label for='current-password'>Текущий пароль</label>"
        "<input type='password' id='current-password' name='current-password' required>"
        "</div>"
        "<div class='form-group'>"
        "<label for='new-password'>Новый пароль</label>"
        "<input type='password' id='new-password' name='new-password' required minlength='6'>"
        "</div>"
        "<div class='form-group'>"
        "<label for='confirm-password'>Подтверждение пароля</label>"
        "<input type='password' id='confirm-password' name='confirm-password' required minlength='6'>"
        "</div>"
        "<button type='submit' class='btn btn-warning'>Изменить пароль</button>"
        "</form>"
        "</div>")
   "Профиль"
   nil))

(defn- session-table-html
  [sessions title empty-text is-admin?]
  (if (empty? sessions)
    (str "<p>" empty-text "</p>")
    (str "<h3>" title "</h3>"
         "<div class='table-responsive'>"
         "<table class='data-table'>"
         "<thead><tr>"
         "<th>ID</th>"
         "<th>Пользователь</th>"
         "<th>Вход</th>"
         "<th>Выход</th>"
         "<th>Длительность</th>"
         "<th>IP адрес</th>"
         "<th>Статус</th>"
         "</tr></thead>"
         "<tbody>"
         (apply str
                (for [s sessions]
                  (let [success (:success s)
                        duration (:duration s)]
                    (str "<tr" (when (not success) " class='row-danger'") ">"
                         "<td>" (:id s) "</td>"
                         "<td>" (html-escape (:username s)) "</td>"
                         "<td>" (html-escape (str (:login_time s))) "</td>"
                         "<td>" (html-escape (or (str (:logout_time s)) "-")) "</td>"
                         "<td>" (or duration "-") "</td>"
                         "<td>" (html-escape (or (:ip_address s) "-")) "</td>"
                         "<td>"
                         (cond
                           (not success) (str "<span class='badge badge-danger'>Ошибка</span>"
                                              (when (:fail_reason s)
                                                (str " (" (html-escape (:fail_reason s)) ")")))
                           (:logout_time s) "<span class='badge badge-success'>Завершена</span>"
                           :else "<span class='badge badge-warning'>Активна</span>")
                         "</td>"
                         "</tr>"))))
         "</tbody></table>"
         "</div>")))

(defn render-sessions-page
  "Страница истории сессий"
  [user sessions active-sessions failed-logins]
  (let [lang "ru"
        is-admin (= "admin" (:role user))]
    (wrap-html
     (str "<div class='page-header'>"
          "<h2>История сессий</h2>"
          "</div>"
          (when (and is-admin active-sessions (seq active-sessions))
            (session-table-html active-sessions "Активные сессии" "Нет активных сессий" is-admin))
          (session-table-html failed-logins "Неудачные попытки входа" "Нет неудачных попыток" is-admin)
          (session-table-html sessions "История входов" "Нет записей" is-admin))
     "История сессий" "sessions" lang)))

(defn render-access-denied
  "Рендер страницы доступа запрещён"
  [user]
  (wrap-html
   (str "<div class='access-denied'>"
        "<h1>🚫 Доступ запрещён</h1>"
        "<p class='denied-message'>У вас недостаточно прав для доступа к этой странице.</p>"
        "<p class='denied-user'>Вы вошли как: <strong>" (html-escape (:username user)) "</strong> (" 
        (html-escape (:role user)) ")</p>"
        "<a href='" (url "/") "' class='btn btn-primary'>Вернуться на главную</a>"
        "</div>")
   "Доступ запрещён"
   nil))

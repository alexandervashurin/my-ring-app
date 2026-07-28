(ns my-ring-app.views.auth
  "Представления аутентификации"
  (:require [my-ring-app.views.layout :refer [wrap-html html-escape csrf-field]]))

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

          "<form method='POST' action='/login' class='login-form'>"
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
        "<form method='POST' action='/change-password' class='profile-form'>"
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

(defn render-access-denied
  "Рендер страницы доступа запрещён"
  [user]
  (wrap-html
   (str "<div class='access-denied'>"
        "<h1>🚫 Доступ запрещён</h1>"
        "<p class='denied-message'>У вас недостаточно прав для доступа к этой странице.</p>"
        "<p class='denied-user'>Вы вошли как: <strong>" (html-escape (:username user)) "</strong> (" 
        (html-escape (:role user)) ")</p>"
        "<a href='/' class='btn btn-primary'>Вернуться на главную</a>"
        "</div>")
   "Доступ запрещён"
   nil))

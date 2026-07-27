(ns my-ring-app.views.auth
  "Представления аутентификации"
  (:require [my-ring-app.views.layout :refer [wrap-html html-escape csrf-field]]))

(defn render-login-page [error redirect-url]
  "Рендер страницы входа"
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
          "</div>"

          "<style>"
          ".login-container { display: flex; justify-content: center; align-items: center; min-height: 100vh; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }"
          ".login-box { background: white; padding: 40px; border-radius: 10px; box-shadow: 0 10px 40px rgba(0,0,0,0.2); width: 100%; max-width: 400px; }"
          ".login-box h1 { text-align: center; color: #667eea; margin-bottom: 10px; }"
          ".login-subtitle { text-align: center; color: #666; margin-bottom: 30px; }"
          ".login-form .form-group { margin-bottom: 20px; }"
          ".login-form label { display: block; margin-bottom: 5px; font-weight: 600; color: #555; }"
          ".login-form input { width: 100%; padding: 12px; border: 2px solid #ddd; border-radius: 5px; font-size: 14px; box-sizing: border-box; }"
          ".login-form input:focus { outline: none; border-color: #667eea; }"
          ".btn-block { width: 100%; padding: 12px; font-size: 16px; }"
          ".error-message { background: #fee; border: 1px solid #fcc; color: #c00; padding: 10px; border-radius: 5px; margin-bottom: 20px; }"
          ".login-footer { margin-top: 20px; text-align: center; }"
          ".default-credentials { font-size: 12px; color: #666; background: #f0f4ff; padding: 10px; border-radius: 5px; }"
          "</style>")
     "Вход в систему"
     nil
     true)))

(defn render-profile-page [user]
  "Рендер страницы профиля"
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
        "</div>"

        "<style>"
        ".profile-container { max-width: 800px; margin: 0 auto; }"
        ".profile-card { background: white; padding: 25px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); margin-bottom: 20px; }"
        ".profile-card h2 { margin-top: 0; color: #667eea; }"
        ".profile-form .form-group { margin-bottom: 15px; }"
        ".profile-form label { display: block; margin-bottom: 5px; font-weight: 600; }"
        ".profile-form input { width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 5px; box-sizing: border-box; }"
        ".profile-form input:focus { outline: none; border-color: #667eea; }"
        "</style>")
   "Профиль"
   nil))

(defn render-access-denied [user]
  "Рендер страницы доступа запрещён"
  (wrap-html
   (str "<div class='access-denied'>"
        "<h1>🚫 Доступ запрещён</h1>"
        "<p class='denied-message'>У вас недостаточно прав для доступа к этой странице.</p>"
        "<p class='denied-user'>Вы вошли как: <strong>" (html-escape (:username user)) "</strong> (" 
        (html-escape (:role user)) ")</p>"
        "<a href='/' class='btn btn-primary'>Вернуться на главную</a>"
        "</div>"

        "<style>"
        ".access-denied { text-align: center; padding: 60px 20px; }"
        ".access-denied h1 { font-size: 48px; color: #c00; margin-bottom: 20px; }"
        ".denied-message { font-size: 18px; color: #666; margin-bottom: 30px; }"
        ".denied-user { color: #999; margin-bottom: 30px; }"
        "</style>")
   "Доступ запрещён"
   nil))

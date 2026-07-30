(ns my-ring-app.views.layout
  (:require [clojure.string :as str]
            [ring.util.anti-forgery :as af]
            [my-ring-app.i18n :as i18n]
            [my-ring-app.config :refer [url]]))

(defn html-escape
  "Экранирование HTML-символов для защиты от XSS"
  [s]
  (if (nil? s)
    ""
    (-> (str s)
        (str/replace "&" "&amp;")
        (str/replace "<" "&lt;")
        (str/replace ">" "&gt;")
        (str/replace "\"" "&quot;")
        (str/replace "'" "&#x27;"))))

(defn csrf-field
  "Генерация скрытого поля с CSRF-токеном для форм"
  []
  (af/anti-forgery-field))

(defn- generate-language-switcher
  "Генерация переключателя языков"
  [current-lang]
  (let [languages (i18n/get-available-languages)]
    (str "<div class='language-switcher'>"
         (apply str (for [lang languages]
                      (if (= (name lang) current-lang)
                        (str "<span class='lang-current'>" (i18n/get-language-name lang) "</span>")
                        (str "<a href='" (url (str "/lang/" lang)) "' class='lang-link'>" (i18n/get-language-name lang) "</a>"))))
         "</div>")))

(defn- generate-css
  "Генерация ссылки на внешний CSS-файл"
  []
  (str "<link rel='stylesheet' href='" (url "/css/app.css") "'>"))

(defn- generate-header
  "Генерация шапки страницы"
  [user current-lang]
  (let [user-info (when user
                    (str "<div class='user-info'>"
                         "<span class='user-greeting'>👤 " (html-escape (:username user)) " ("
                         (html-escape (get {"admin" "Администратор"
                                            "manager" "Менеджер"
                                            "viewer" "Наблюдатель"
                                            "hr" "HR-специалист"} (:role user) (:role user))) ")</span>"
                          "<a href='" (url "/profile") "' class='btn btn-sm btn-info btn-user-action'>" (i18n/t current-lang :auth :profile) "</a>"
                          "<form method='POST' action='" (url "/logout") "' class='inline-form'>"
                         (csrf-field)
                         "<button type='submit' class='btn btn-sm btn-secondary'>" (i18n/t current-lang :auth :logout) "</button>"
                         "</form>"
                         "</div>"))
        lang-switcher (generate-language-switcher current-lang)]
    (str "<header>"
         "<div class='header-row'>"
         "<div>"
         "<h1>🏭 " (i18n/t current-lang :common :title) "</h1>"
         "<p class='header-subtitle'>" (i18n/t current-lang :common :subtitle) "</p>"
         "</div>"
         "<div class='header-right'>"
         lang-switcher
         user-info
         "</div>"
         "</div>"
         "</header>")))

(defn- generate-navigation
  "Генерация навигационного меню"
  [active-page user lang]
  (let [active-class (fn [page] (if (= page active-page) " class='active'" ""))
        user-role (:role user)
        has-salary-access (contains? #{"admin" "manager"} user-role)
        is-admin (= user-role "admin")]
    (str "<nav>"
         "<a href='" (url "/") "'" (active-class "home") ">Главная</a>"
         "<a href='" (url "/dashboard") "'" (active-class "dashboard") ">Дашборд</a>"
         "<a href='" (url "/workers") "'" (active-class "workers") ">Работники</a>"
         (when is-admin
           (str "<a href='" (url "/organizations") "'" (active-class "organizations") ">Организации</a>"))
         (when has-salary-access
           (str "<a href='" (url "/db") "'" (active-class "db") ">Все таблицы</a>"))
         "</nav>")))

(defn wrap-html
  "Оборачивает контент в полную HTML-страницу"
  [content title & [active-page user current-lang]]
  (let [lang (or current-lang "ru")]
    (str "<!DOCTYPE html>"
         "<html lang='" lang "'>"
         "<head>"
         "<meta charset='UTF-8'>"
         "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
         "<meta name='theme-color' content='#667eea'>"
         "<meta name='description' content='Система управления персоналом - CRUD приложение для базы данных работников'>"
         "<meta name='mobile-web-app-capable' content='yes'>"
         "<meta name='apple-mobile-web-app-capable' content='yes'>"
         "<meta name='apple-mobile-web-app-status-bar-style' content='default'>"
         "<meta name='apple-mobile-web-app-title' content='HR System'>"
         "<link rel='manifest' href='" (url "/manifest.json") "'>"
         "<link rel='apple-touch-icon' href='" (url "/icons/icon-192x192.png") "'>"
         "<title>" title " - " (i18n/t lang :common :title) "</title>"
         (generate-css)
         "<script src='https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js' integrity='sha384-e6nUZLBkQ86NJ6TVVKAeSaK8jWa3NhkYWZFomE39AvDbQWeie9PlQqM3pmYW5d1g' crossorigin='anonymous'></script>"
         "</head>"
         "<body>"
         "<div class='container'>"
         (generate-header user lang)
         (generate-navigation (or active-page "home") user lang)
         content
           "<script src='" (url "/js/validation.js") "'></script>"
           "<script src='" (url "/js/app.js") "'></script>"
           "<script src='" (url "/js/charts.js") "'></script>"
         "</body>"
         "</html>")))
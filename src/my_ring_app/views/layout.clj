(ns my-ring-app.views.layout
  (:require [clojure.string :as str]
            [my-ring-app.i18n :as i18n]))

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

(defn- generate-language-switcher [current-lang]
  "Генерация переключателя языков"
  (let [languages (i18n/get-available-languages)]
    (str "<div class='language-switcher'>"
         (apply str (for [lang languages]
                      (if (= (name lang) current-lang)
                        (str "<span class='lang-current'>" (i18n/get-language-name lang) "</span>")
                        (str "<a href='/lang/" lang "' class='lang-link'>" (i18n/get-language-name lang) "</a>"))))
         "</div>")))

(defn- generate-css []
  "Генерация стилей CSS"
  "<style>
    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background: #f5f7fa; color: #333; }
    .container { max-width: 1400px; margin: 0 auto; }
    header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 10px; margin-bottom: 30px; box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3); }
    header h1 { margin: 0; font-size: 28px; }
    nav { background: white; padding: 15px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); display: flex; flex-wrap: wrap; gap: 10px; }
    nav a { display: inline-block; padding: 10px 15px; text-decoration: none; color: #667eea; border-radius: 5px; transition: all 0.3s; }
    nav a:hover { background: #f0f4ff; }
    nav a.active { background: #667eea; color: white; }
    .form-container, .table-container { background: white; padding: 25px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); margin-bottom: 20px; }
    .form-group { margin-bottom: 15px; }
    .form-group label { display: block; margin-bottom: 5px; font-weight: 600; color: #555; }
    .form-group input, .form-group select { width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 4px; font-size: 14px; }
    .form-group input:focus, .form-group select:focus { outline: none; border-color: #667eea; box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1); }
    .btn { padding: 10px 20px; border: none; border-radius: 5px; cursor: pointer; font-weight: 600; transition: all 0.3s; text-decoration: none; display: inline-block; text-align: center; font-size: 14px; }
    .btn-primary { background: #667eea; color: white; }
    .btn-primary:hover { background: #5568d3; transform: translateY(-2px); box-shadow: 0 4px 8px rgba(102, 126, 234, 0.4); }
    .btn-secondary { background: #e0e0e0; color: #333; }
    .btn-secondary:hover { background: #d0d0d0; }
    .btn-success { background: #4CAF50; color: white; }
    .btn-success:hover { background: #45a049; }
    .btn-warning { background: #FF9800; color: white; }
    .btn-warning:hover { background: #F57C00; }
    .btn-danger { background: #F44336; color: white; }
    .btn-danger:hover { background: #E53935; }
    .btn-info { background: #2196F3; color: white; }
    .btn-info:hover { background: #1976D2; }
    .btn-sm { padding: 6px 10px; font-size: 12px; }
    .data-table { width: 100%; border-collapse: collapse; margin-top: 10px; display: block; overflow-x: auto; }
    .data-table thead { display: table-header-group; }
    .data-table tbody { display: table-row-group; }
    .data-table th { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 12px; text-align: left; font-weight: 600; white-space: nowrap; }
    .data-table td { padding: 10px; border-bottom: 1px solid #eee; }
    .data-table tr:hover { background: #f9f9f9; }
    .empty-state { padding: 40px; text-align: center; color: #999; font-size: 16px; background: #f9f9f9; border-radius: 8px; }
    .alert { padding: 15px; border-radius: 5px; margin-bottom: 20px; font-weight: 500; }
    .alert-success { background: #e8f5e9; color: #2e7d32; border-left: 4px solid #4CAF50; }
    .alert-error { background: #ffebee; color: #c62828; border-left: 4px solid #F44336; }
    .alert-info { background: #e3f2fd; color: #1565c0; border-left: 4px solid #2196F3; }
    .dashboard-stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-bottom: 30px; }
    .stat-card { display: flex; align-items: center; padding: 20px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); transition: transform 0.3s, box-shadow 0.3s; position: relative; overflow: hidden; }
    .stat-card:hover { transform: translateY(-5px); box-shadow: 0 8px 24px rgba(0,0,0,0.15); }
    .stat-card__icon { font-size: 48px; margin-right: 20px; flex-shrink: 0; }
    .stat-card__content { flex: 1; }
    .stat-card__value { font-size: 28px; font-weight: 700; margin-bottom: 5px; }
    .stat-card__label { font-size: 14px; opacity: 0.8; margin-bottom: 8px; }
    .stat-card__trend { font-size: 12px; margin-top: 8px; padding: 4px 8px; border-radius: 4px; display: inline-block; }
    .stat-card__trend--up { background: rgba(76, 175, 80, 0.2); color: #4CAF50; }
    .stat-card__trend--down { background: rgba(244, 67, 54, 0.2); color: #F44336; }
    .stat-card--primary { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; }
    .stat-card--success { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); color: white; }
    .stat-card--info { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); color: white; }
    .stat-card--warning { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); color: white; }
    .stat-card--purple { background: linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%); color: white; }
    .stat-card--blue { background: linear-gradient(135deg, #5ee7df 0%, #b490ca 100%); color: white; }
    .dashboard-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px; flex-wrap: wrap; gap: 15px; }
    .dashboard-actions { display: flex; gap: 10px; }
    .charts-row, .tables-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(400px, 1fr)); gap: 20px; margin-bottom: 30px; }
    .chart-container, .dashboard-table { background: white; padding: 25px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
    .chart-title, .table-title { margin: 0 0 20px 0; color: #333; font-size: 18px; border-bottom: 2px solid #667eea; padding-bottom: 10px; }
    .worker-link { color: #667eea; text-decoration: none; font-weight: 600; }
    .worker-link:hover { text-decoration: underline; }
    .progress-container { margin-top: 10px; }
    .progress-bar { height: 8px; background: rgba(255,255,255,0.3); border-radius: 4px; overflow: hidden; margin-top: 5px; }
    .progress-fill { height: 100%; background: rgba(255,255,255,0.8); border-radius: 4px; transition: width 0.5s ease; }
    .pagination { display: flex; justify-content: center; align-items: center; gap: 10px; margin-top: 20px; flex-wrap: wrap; }
    .pagination a, .pagination span { padding: 8px 12px; border: 1px solid #ddd; border-radius: 4px; text-decoration: none; color: #667eea; transition: all 0.3s; }
    .pagination a:hover { background: #f0f4ff; border-color: #667eea; }
    .pagination .active { background: #667eea; color: white; border-color: #667eea; }
    .search-box { background: #f0f4ff; padding: 20px; border-radius: 8px; margin-bottom: 25px; border: 1px solid #bbdefb; }
    .search-box input[type='text'] { width: 100%; padding: 12px; border: 2px solid #bbdefb; border-radius: 6px; font-size: 14px; transition: border-color 0.3s; }
    .search-box input[type='text']:focus { outline: none; border-color: #667eea; }
    .search-results-info { margin-top: 15px; padding: 12px; background: #e3f2fd; border-radius: 6px; color: #1976d2; font-size: 14px; }
    
    /* Mobile responsive */
    @media (max-width: 768px) {
      body { padding: 10px; }
      header { padding: 15px; }
      header h1 { font-size: 22px; }
      .user-info { margin-top: 10px; flex-direction: column; align-items: flex-start; gap: 10px; }
      nav { flex-direction: column; }
      nav a { display: block; margin: 5px 0; }
      .data-table { font-size: 12px; }
      .data-table th, .data-table td { padding: 8px; }
      .dashboard-stats { grid-template-columns: 1fr; }
      .dashboard-header { flex-direction: column; align-items: flex-start; }
      .dashboard-actions { width: 100%; justify-content: space-between; }
      .btn-refresh, .btn-export { flex: 1; justify-content: center; }
      .charts-row, .tables-row { grid-template-columns: 1fr; }
      .form-row { grid-template-columns: 1fr; }
      .btn-group { flex-direction: column; }
      .btn-group .btn { width: 100%; }
      .pagination { flex-direction: column; }
      .stat-card { flex-direction: column; text-align: center; }
      .stat-card__icon { margin-right: 0; margin-bottom: 10px; }
      .stat-card__value { font-size: 24px; }
    }
  </style>")

(defn- generate-header [user current-lang]
  "Генерация шапки страницы"
  (let [user-info (when user
                    (str "<div class='user-info'>"
                         "<span class='user-greeting'>👤 " (html-escape (:username user)) " ("
                         (html-escape (get {"admin" "Администратор"
                                            "manager" "Менеджер"
                                            "viewer" "Наблюдатель"
                                            "hr" "HR-специалист"} (:role user) (:role user))) ")</span>"
                         "<a href='/profile' class='btn btn-sm btn-info' style='margin-left: 10px;'>" (i18n/t current-lang :auth :profile) "</a>"
                         "<a href='/logout' class='btn btn-sm btn-secondary' style='margin-left: 5px;'>" (i18n/t current-lang :auth :logout) "</a>"
                         "</div>"))
        lang-switcher (generate-language-switcher current-lang)]
    (str "<header>"
         "<div style='display: flex; justify-content: space-between; align-items: center;'>"
         "<div>"
         "<h1>🏭 " (i18n/t current-lang :common :title) "</h1>"
         "<p style='opacity: 0.9; margin-top: 5px;'>" (i18n/t current-lang :common :subtitle) "</p>"
         "</div>"
         "<div style='display: flex; align-items: center; gap: 15px;'>"
         lang-switcher
         user-info
         "</div>"
         "</div>"
         "</header>")))

(defn- generate-navigation [active-page user]
  "Генерация навигационного меню"
  (let [active-class (fn [page] (if (= page active-page) " class='active'" ""))
        user-role (:role user)
        has-salary-access (contains? #{"admin" "manager"} user-role)]
    (str "<nav>"
         "<a href='/'" (active-class "home") ">Главная</a>"
         "<a href='/dashboard'" (active-class "dashboard") ">Дашборд</a>"
         "<a href='/workers'" (active-class "workers") ">Работники</a>"
         (when has-salary-access
           "<a href='/db'" (active-class "db") ">Все таблицы</a>")
         "</nav>")))

(defn wrap-html [content title & [active-page user current-lang]]
  "Оборачивает контент в полную HTML-страницу"
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
         "<link rel='manifest' href='/manifest.json'>"
         "<link rel='apple-touch-icon' href='/icons/icon-192x192.png'>"
         "<title>" title " - " (i18n/t lang :common :title) "</title>"
         (generate-css)
         "<style>"
         ".language-switcher { display: flex; gap: 10px; align-items: center; }"
         ".lang-link { color: white; text-decoration: underline; cursor: pointer; font-size: 14px; }"
         ".lang-link:hover { color: #f0f4ff; }"
         ".lang-current { color: white; font-weight: bold; font-size: 14px; }"
         ".user-info { display: flex; align-items: center; font-size: 14px; }"
         ".user-greeting { color: white; opacity: 0.95; }"
         "</style>"
         "<script src='https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js'></script>"
         "</head>"
         "<body>"
         "<div class='container'>"
         (generate-header user lang)
         (generate-navigation (or active-page "home") user lang)
         content
         "<script src='/js/app.js'></script>"
         "<script src='/js/charts.js'></script>"
         "<script>"
         "// Регистрация Service Worker"
         "if ('serviceWorker' in navigator) {"
         "  window.addEventListener('load', function() {"
         "    navigator.serviceWorker.register('/sw.js')"
         "      .then(function(registration) {"
         "        console.log('[PWA] Service Worker зарегистрирован:', registration.scope);"
         "      })"
         "      .catch(function(error) {"
         "        console.log('[PWA] Ошибка регистрации Service Worker:', error);"
         "      });"
         "  });"
         "}"
         "</script>"
         "</body>"
         "</html>")))
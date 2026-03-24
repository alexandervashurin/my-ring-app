(ns my-ring-app.views.layout
  (:require [clojure.string :as str]))

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

(defn- generate-css []
  "Генерация стилей CSS"
  "<style>
    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background: #f5f7fa; color: #333; }
    .container { max-width: 1400px; margin: 0 auto; }
    header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 10px; margin-bottom: 30px; box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3); }
    header h1 { margin: 0; font-size: 28px; }
    nav { background: white; padding: 15px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
    nav a { display: inline-block; margin: 0 10px; padding: 10px 15px; text-decoration: none; color: #667eea; border-radius: 5px; transition: all 0.3s; }
    nav a:hover { background: #f0f4ff; }
    nav a.active { background: #667eea; color: white; }
    .form-container, .table-container { background: white; padding: 25px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); margin-bottom: 20px; }
    .form-group { margin-bottom: 15px; }
    .form-group label { display: block; margin-bottom: 5px; font-weight: 600; color: #555; }
    .form-group input, .form-group select { width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 4px; font-size: 14px; }
    .form-group input:focus, .form-group select:focus { outline: none; border-color: #667eea; box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1); }
    .btn { padding: 10px 20px; border: none; border-radius: 5px; cursor: pointer; font-weight: 600; transition: all 0.3s; text-decoration: none; display: inline-block; text-align: center; }
    .btn-primary { background: #667eea; color: white; }
    .btn-primary:hover { background: #5568d3; transform: translateY(-2px); box-shadow: 0 4px 8px rgba(102, 126, 234, 0.4); }
    .btn-secondary { background: #e0e0e0; color: #333; }
    .btn-secondary:hover { background: #d0d0d0; }
    .btn-success { background: #4CAF50; color: white; padding: 8px 12px; }
    .btn-success:hover { background: #45a049; }
    .btn-warning { background: #FF9800; color: white; padding: 8px 12px; }
    .btn-warning:hover { background: #F57C00; }
    .btn-danger { background: #F44336; color: white; padding: 8px 12px; }
    .btn-danger:hover { background: #E53935; }
    .btn-info { background: #2196F3; color: white; padding: 8px 12px; }
    .btn-info:hover { background: #1976D2; }
    .btn-sm { padding: 6px 10px; font-size: 12px; }
    .data-table { width: 100%; border-collapse: collapse; margin-top: 10px; }
    .data-table th { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 12px; text-align: left; font-weight: 600; }
    .data-table td { padding: 10px; border-bottom: 1px solid #eee; }
    .data-table tr:hover { background: #f9f9f9; }
    .empty-state { padding: 40px; text-align: center; color: #999; font-size: 16px; background: #f9f9f9; border-radius: 8px; }
    .alert { padding: 15px; border-radius: 5px; margin-bottom: 20px; font-weight: 500; }
    .alert-success { background: #e8f5e9; color: #2e7d32; border-left: 4px solid #4CAF50; }
    .alert-error { background: #ffebee; color: #c62828; border-left: 4px solid #F44336; }
    .alert-info { background: #e3f2fd; color: #1565c0; border-left: 4px solid #2196F3; }
    
    /* Dashboard styles */
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
    .btn-refresh { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border: none; padding: 10px 20px; border-radius: 8px; cursor: pointer; font-weight: 600; transition: all 0.3s; display: flex; align-items: center; gap: 8px; }
    .btn-refresh:hover { transform: rotate(180deg); box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4); }
    .btn-export { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); color: white; border: none; padding: 10px 20px; border-radius: 8px; cursor: pointer; font-weight: 600; transition: all 0.3s; display: flex; align-items: center; gap: 8px; }
    .btn-export:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(67, 233, 123, 0.4); }
    
    .charts-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(400px, 1fr)); gap: 20px; margin-bottom: 30px; }
    .chart-container { background: white; padding: 25px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
    .chart-title { margin: 0 0 20px 0; color: #333; font-size: 18px; border-bottom: 2px solid #667eea; padding-bottom: 10px; }
    .tables-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(400px, 1fr)); gap: 20px; }
    .dashboard-table { background: white; padding: 25px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
    .table-title { margin: 0 0 20px 0; color: #333; font-size: 18px; border-bottom: 2px solid #667eea; padding-bottom: 10px; }
    .worker-link { color: #667eea; text-decoration: none; font-weight: 600; }
    .worker-link:hover { text-decoration: underline; }
    
    /* Progress bars */
    .progress-container { margin-top: 10px; }
    .progress-bar { height: 8px; background: rgba(255,255,255,0.3); border-radius: 4px; overflow: hidden; margin-top: 5px; }
    .progress-fill { height: 100%; background: rgba(255,255,255,0.8); border-radius: 4px; transition: width 0.5s ease; }
    
    @media (max-width: 768px) {
      nav a { display: block; margin: 5px 0; }
      .data-table { font-size: 12px; }
      .data-table th, .data-table td { padding: 8px; }
      .dashboard-stats { grid-template-columns: 1fr; }
      .charts-row, .tables-row { grid-template-columns: 1fr; }
      .dashboard-header { flex-direction: column; align-items: flex-start; }
      .dashboard-actions { width: 100%; justify-content: space-between; }
      .btn-refresh, .btn-export { flex: 1; justify-content: center; }
    }
  </style>")

(defn- generate-header [user]
  "Генерация шапки страницы"
  (let [user-info (when user
                    (str "<div class='user-info'>"
                         "<span class='user-greeting'>👤 " (html-escape (:username user)) " (" 
                         (html-escape (get {"admin" "Администратор"
                                            "manager" "Менеджер"
                                            "viewer" "Наблюдатель"
                                            "hr" "HR-специалист"} (:role user) (:role user))) ")</span>"
                         "<a href='/profile' class='btn btn-sm btn-info' style='margin-left: 10px;'>Профиль</a>"
                         "<a href='/logout' class='btn btn-sm btn-secondary' style='margin-left: 5px;'>Выход</a>"
                         "</div>"))]
    (str "<header>"
         "<div style='display: flex; justify-content: space-between; align-items: center;'>"
         "<div>"
         "<h1>🏭 Система управления персоналом</h1>"
         "<p style='opacity: 0.9; margin-top: 5px;'>CRUD-приложение для базы данных работников</p>"
         "</div>"
         user-info
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

(defn wrap-html [content title & [active-page user]]
  "Оборачивает контент в полную HTML-страницу"
  (str "<!DOCTYPE html>"
       "<html lang='ru'>"
       "<head>"
       "<meta charset='UTF-8'>"
       "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
       "<title>" title " - Управление БД работников</title>"
       (generate-css)
       "<style>"
       ".user-info { display: flex; align-items: center; font-size: 14px; }"
       ".user-greeting { color: white; opacity: 0.95; }"
       "</style>"
       "</head>"
       "<body>"
       "<div class='container'>"
       (generate-header user)
       (generate-navigation (or active-page "home") user)
       content
       "</div>"
       "</body>"
       "</html>"))
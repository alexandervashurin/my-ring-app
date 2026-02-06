(ns my-ring-app.views.layout
  (:require [clojure.string :as str]))

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
    @media (max-width: 768px) {
      nav a { display: block; margin: 5px 0; }
      .data-table { font-size: 12px; }
      .data-table th, .data-table td { padding: 8px; }
    }
  </style>")

(defn- generate-header []
  "Генерация шапки страницы"
  "<header>
    <h1>🏭 Система управления персоналом</h1>
    <p style='opacity: 0.9; margin-top: 5px;'>CRUD-приложение для базы данных работников</p>
  </header>")

(defn- generate-navigation [active-page]
  "Генерация навигационного меню"
  (let [active-class (fn [page] (if (= page active-page) " class='active'" ""))]
    (str "<nav>"
         "<a href='/'" (active-class "home") ">Главная</a>"
         "<a href='/workers'" (active-class "workers") ">Работники</a>"
         "<a href='/db'" (active-class "db") ">Все таблицы</a>"
         "</nav>")))

(defn wrap-html [content title & [active-page]]
  "Оборачивает контент в полную HTML-страницу"
  (str "<!DOCTYPE html>"
       "<html lang='ru'>"
       "<head>"
       "<meta charset='UTF-8'>"
       "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
       "<title>" title " - Управление БД работников</title>"
       (generate-css)
       "</head>"
       "<body>"
       "<div class='container'>"
       (generate-header)
       (generate-navigation (or active-page "home"))
       content
       "</div>"
       "</body>"
       "</html>"))
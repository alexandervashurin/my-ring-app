(ns my-ring-app.views
  (:require [clojure.string :as str]))

;; ======================================================================
;; Вспомогательные функции
;; ======================================================================

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

(defn- generate-navigation []
  "Генерация навигационного меню"
  "<nav>
    <a href='/'>Главная</a>
    <a href='/workers' class='active'>Работники</a>
    <a href='/db'>Все таблицы</a>
  </nav>")

(defn- wrap-html [content title]
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
       (generate-navigation)
       content
       "</div>"
       "</body>"
       "</html>"))

(defn- option-tag [item value-key label-key selected-value]
  "Генерация опции для выпадающего списка"
  (let [item-id (str (value-key item))
        item-label (str (label-key item))
        selected (if (= item-id (str selected-value)) " selected" "")]
    (str "<option value='" item-id "'" selected ">" item-label "</option>")))

(defn- render-error-messages [errors]
  "Рендер списка ошибок валидации"
  (when (seq errors)
    (str "<div class='alert alert-error' style='margin-bottom: 20px;'>"
         "<strong>⚠️ Ошибки валидации:</strong>"
         "<ul style='margin: 10px 0 0 20px;'>"
         (apply str 
           (map (fn [error]
                  (str "<li>" error "</li>"))
                errors))
         "</ul>"
         "</div>")))

;; ======================================================================
;; Главная страница
;; ======================================================================

(defn render-home []
  "Рендер главной страницы"
  (wrap-html
    "<div class='alert alert-info'>Добро пожаловать в систему управления персоналом! Выберите раздел в меню выше.</div>"
    "Главная"))

;; ======================================================================
;; Страница списка работников
;; ======================================================================

(defn render-workers-table [workers search-query]
  "Рендер таблицы работников"
  (let [table-content (if (empty? workers)
                        "<div class='empty-state'>Нет работников в базе данных</div>"
                        (str "<table class='data-table'>"
                             "<thead>"
                             "<tr>"
                             "<th>ID</th>"
                             "<th>ФИО</th>"
                             "<th>Дата приема</th>"
                             "<th>Цех</th>"
                             "<th>Система оплаты</th>"
                             "<th>Категория</th>"
                             "<th>Разряд</th>"
                             "<th>Режим работы</th>"
                             "<th>Действия</th>"
                             "</tr>"
                             "</thead>"
                             "<tbody>"
                             (apply str 
                               (map (fn [w]
                                      (str "<tr>"
                                           "<td>" (:id w) "</td>"
                                           "<td><strong>" (:фамилия w) "</strong> " (:имя w) " " (or (:отчество w) "") "</td>"
                                           "<td>" (:дата_приема w) "</td>"
                                           "<td>" (:цех w) "</td>"
                                           "<td>" (:система w) "</td>"
                                           "<td>" (:категория w) "</td>"
                                           "<td>" (:разряд w) "</td>"
                                           "<td>" (:режим w) "</td>"
                                           "<td>"
                                           "<a href='/workers/" (:id w) "/work-time' class='btn btn-sm btn-info' title='Учет времени'>⏰</a> "
                                           "<a href='/workers/" (:id w) "/salary' class='btn btn-sm btn-success' title='Зарплата'>💰</a> "
                                           "<a href='/workers/" (:id w) "/edit' class='btn btn-sm btn-warning' title='Редактировать'>✏️</a> "
                                           "<form method='POST' action='/workers/" (:id w) "/delete' style='display: inline;' onsubmit='return confirm(\"Удалить работника " (:фамилия w) " " (:имя w) "?\")'>"
                                           "<button type='submit' class='btn btn-sm btn-danger' title='Удалить'>🗑️</button>"
                                           "</form>"
                                           "</td>"
                                           "</tr>"))
                                    workers))
                             "</tbody>"
                             "</table>"))]
    (str "<div class='table-container'>"
         "<div style='display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;'>"
         "<h2>📋 Список работников</h2>"
         "<a href='/workers/new' class='btn btn-success'>➕ Добавить работника</a>"
         "</div>"
         
         ;; Форма поиска
         "<div style='background: #f0f4ff; padding: 20px; border-radius: 8px; margin-bottom: 25px; border: 1px solid #bbdefb;'>"
         "<form method='GET' action='/workers' style='display: flex; gap: 10px; flex-wrap: wrap; align-items: flex-end;'>"
         "<div style='flex: 1; min-width: 250px;'>"
         "<label style='display: block; margin-bottom: 5px; font-weight: 600; color: #1976d2;'>Поиск по ФИО или цеху:</label>"
         "<input type='text' name='search' placeholder='Введите фамилию, имя, отчество или цех...' value='" (or search-query "") "'"
         " style='width: 100%; padding: 12px; border: 2px solid #bbdefb; border-radius: 6px; font-size: 14px; transition: border-color 0.3s;'>"
         "</div>"
         "<button type='submit' class='btn btn-primary' style='padding: 12px 24px; white-space: nowrap;'>🔍 Найти</button>"
         (when search-query
           (str "<a href='/workers' class='btn btn-secondary' style='padding: 12px 24px; white-space: nowrap; margin-left: 10px;'>Сбросить фильтр</a>"
                "<div style='margin-top: 15px; padding: 12px; background: #e3f2fd; border-radius: 6px; color: #1976d2; font-size: 14px;'>"
                "<strong>Результаты поиска:</strong> найдено " (count workers) " работников по запросу: <em>\"" search-query "\"</em>"
                "</div>"))
         "</form>"
         "</div>"
         
         table-content
         "</div>")))

(defn render-workers-page [workers search-query]
  "Рендер страницы списка работников"
  (wrap-html (render-workers-table workers search-query) "Работники"))

;; ======================================================================
;; Форма работника
;; ======================================================================

(defn render-worker-form [mode worker-data цеха системы_оплаты категории разряды режимы оклады ставки & [options]]
  "Рендер формы создания/редактирования работника"
  (let [цех-id (:цех_id worker-data)
        система-id (:система_оплаты_id worker-data)
        категория-id (:категория_работника_id worker-data)
        разряд-id (:разряд_id worker-data)
        режим-id (:режим_работы_id worker-data)
        оклад-id (:оклад_id worker-data)
        ставка-id (:почасовая_ставка_id worker-data)
        form-title (if (= mode :create) "➕ Добавить работника" "✏️ Редактировать работника")
        form-action (if (= mode :edit) (str "/workers/" (:id worker-data) "/update") "/workers/create")
        errors (:errors options)]
    
    (str "<div class='form-container'>"
         "<h2>" form-title "</h2>"
         
         ;; Отображение ошибок валидации
         (render-error-messages errors)
         
         "<form method='POST' action='" form-action "' style='background: white; padding: 25px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1);'>"
         
         ;; Фамилия
         "<div class='form-group'>"
         "<label>Фамилия *</label>"
         "<input type='text' name='фамилия' value='" (or (:фамилия worker-data) "") "' required>"
         "</div>"
         
         ;; Имя
         "<div class='form-group'>"
         "<label>Имя *</label>"
         "<input type='text' name='имя' value='" (or (:имя worker-data) "") "' required>"
         "</div>"
         
         ;; Отчество
         "<div class='form-group'>"
         "<label>Отчество</label>"
         "<input type='text' name='отчество' value='" (or (:отчество worker-data) "") "'>"
         "</div>"
         
         ;; Дата приема
         "<div class='form-group'>"
         "<label>Дата приема *</label>"
         "<input type='date' name='дата_приема' value='" (or (:дата_приема worker-data) "") "' required>"
         "</div>"
         
         ;; Цех
         "<div class='form-group'>"
         "<label>Цех *</label>"
         "<select name='цех_id' required>"
         "<option value=''>Выберите цех</option>"
         (apply str (map #(option-tag % :id :название_цеха цех-id) цеха))
         "</select>"
         "</div>"
         
         ;; Система оплаты
         "<div class='form-group'>"
         "<label>Система оплаты *</label>"
         "<select name='система_оплаты_id' required onchange='togglePaymentFields(this.value)'>"
         "<option value=''>Выберите систему</option>"
         (apply str (map #(option-tag % :id :название_системы система-id) системы_оплаты))
         "</select>"
         "</div>"
         
         ;; Категория работника
         "<div class='form-group'>"
         "<label>Категория работника *</label>"
         "<select name='категория_работника_id' required>"
         "<option value=''>Выберите категорию</option>"
         (apply str (map #(option-tag % :id :название_категории категория-id) категории))
         "</select>"
         "</div>"
         
         ;; Разряд
         "<div class='form-group'>"
         "<label>Разряд *</label>"
         "<select name='разряд_id' required>"
         "<option value=''>Выберите разряд</option>"
         (apply str (map #(option-tag % :id :номер_разряда разряд-id) разряды))
         "</select>"
         "</div>"
         
         ;; Режим работы
         "<div class='form-group'>"
         "<label>Режим работы *</label>"
         "<select name='режим_работы_id' required>"
         "<option value=''>Выберите режим</option>"
         (apply str (map #(option-tag % :id :название_режима режим-id) режимы))
         "</select>"
         "</div>"
         
         ;; Оклад (показывается только при системе оплаты "Оклад")
         "<div class='form-group payment-field' id='oklad-field' style='display: none;'>"
         "<label>Оклад</label>"
         "<select name='оклад_id'>"
         "<option value=''>Выберите оклад</option>"
         (apply str (map #(option-tag % :id :оклад_в_месяц оклад-id) оклады))
         "</select>"
         "</div>"
         
         ;; Почасовая ставка (показывается только при системе оплаты "Почасовая")
         "<div class='form-group payment-field' id='stavka-field' style='display: none;'>"
         "<label>Почасовая ставка</label>"
         "<select name='почасовая_ставка_id'>"
         "<option value=''>Выберите ставку</option>"
         (apply str (map #(option-tag % :id :ставка_в_час ставка-id) ставки))
         "</select>"
         "</div>"
         
         ;; Кнопки
         "<div style='display: flex; gap: 10px; margin-top: 20px;'>"
         "<button type='submit' class='btn btn-primary'>"
         (if (= mode :create) "Создать" "Сохранить")
         "</button>"
         "<a href='/workers' class='btn btn-secondary'>Отмена</a>"
         "</div>"
         
         "</form>"
         "</div>"

         "<script>
           function togglePaymentFields(systemId) {
             const okladField = document.getElementById('oklad-field');
             const stavkaField = document.getElementById('stavka-field');
             
             if (systemId == '1') {
               okladField.style.display = 'block';
               stavkaField.style.display = 'none';
             } else if (systemId == '2') {
               okladField.style.display = 'none';
               stavkaField.style.display = 'block';
             } else {
               okladField.style.display = 'none';
               stavkaField.style.display = 'none';
             }
           }
           
           window.onload = function() {
             const systemSelect = document.querySelector('select[name=\"система_оплаты_id\"]');
             if (systemSelect && systemSelect.value) {
               togglePaymentFields(systemSelect.value);
             }
           };
         </script>")))

(defn render-new-worker-page [цеха системы_оплаты категории разряды режимы оклады ставки & [options]]
  "Рендер страницы создания нового работника"
  (let [errors (:errors options)
        worker-data (:worker-data options)]
    (wrap-html 
      (render-worker-form :create (or worker-data {}) цеха системы_оплаты категории разряды режимы оклады ставки {:errors errors})
      "Новый работник")))

(defn render-edit-worker-page [worker цеха системы_оплаты категории разряды режимы оклады ставки & [options]]
  "Рендер страницы редактирования работника"
  (let [errors (:errors options)]
    (wrap-html 
      (render-worker-form :edit worker цеха системы_оплаты категории разряды режимы оклады ставки {:errors errors})
      "Редактировать работника")))

;; ======================================================================
;; Страница зарплаты работника
;; ======================================================================

(defn- render-salary-details [salary-info]
  "Рендер детальной информации о зарплате"
  (if salary-info
    (str "<div style='background: #e8f5e9; padding: 25px; border-radius: 8px; margin-bottom: 20px; border: 2px solid #4caf50;'>"
         "<h3 style='margin-top: 0; color: #1b5e20;'>📊 Расчет за октябрь 2025</h3>"
         
         "<div style='display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 20px;'>"
         
         "<div>"
         "<p><strong>👨‍💼 ФИО:</strong> <span style='font-size: 18px; color: #1976d2;'>" (:фамилия salary-info) " " (:имя salary-info) " " (or (:отчество salary-info) "") "</span></p>"
         "<p><strong>🏭 Цех:</strong> " (:название_цеха salary-info) "</p>"
         "<p><strong>💰 Система оплаты:</strong> " (:название_системы salary-info) "</p>"
         "<p><strong>⏰ Отработано часов:</strong> <span style='font-weight: bold; color: #43a047;'>" (:всего_отработанных_часов salary-info) " ч.</span></p>"
         "<p><strong>🏥 Больничные дни:</strong> <span style='color: #ff9800;'>" (:больничные_дни salary-info) "</span></p>"
         "<p><strong>✈️ Командировочные дни:</strong> <span style='color: #2196f3;'>" (:командировочные_дни salary-info) "</span></p>"
         "</div>"
         
         "<div style='background: #fff3cd; padding: 15px; border-radius: 6px; border: 1px solid #ffc107;'>"
         "<h4 style='margin-top: 0; color: #5d4037;'>💰 Начислено:</h4>"
         (when (:оклад_в_месяц salary-info)
           (str "<p style='margin: 8px 0;'><strong>💵 Оклад:</strong> <span style='color: #2e7d32; font-weight: bold;'>" (:оклад_в_месяц salary-info) " руб.</span></p>"))
         (when (:ставка_в_час salary-info)
           (str "<p style='margin: 8px 0;'><strong>⏱️ Ставка:</strong> <span style='color: #1976d2; font-weight: bold;'>" (:ставка_в_час salary-info) " руб./час</span></p>"))
         (when (> (:зарплата_за_больничные_дни salary-info 0) 0)
           (str "<p style='margin: 8px 0;'><strong>🏥 За больничные:</strong> <span style='color: #ff9800; font-weight: bold;'>" (:зарплата_за_больничные_дни salary-info) " руб.</span></p>"))
         (when (> (:зарплата_за_командировочные_дни salary-info 0) 0)
           (str "<p style='margin: 8px 0;'><strong>✈️ За командировки:</strong> <span style='color: #2196f3; font-weight: bold;'>" (:зарплата_за_командировочные_дни salary-info) " руб.</span></p>"))
         "<hr style='border: 2px solid #2e7d32; margin: 15px 0;'>"
         "<p style='font-size: 24px; font-weight: bold; color: #1b5e20; margin: 0;'>"
         "Итого: <span style='color: #c62828;'>" (:общая_зарплата salary-info) " руб.</span>"
         "</p>"
         "</div>"
         
         "</div>"
         
         "</div>")
    "<div class='alert alert-error'>Данные о зарплате не найдены</div>"))

(defn- render-salary-history [history]
  "Рендер истории зарплат"
  (if (empty? history)
    ""
    (str "<div class='table-container' style='margin-top: 30px;'>"
         "<h3>📈 История начислений</h3>"
         "<table class='data-table'>"
         "<thead>"
         "<tr>"
         "<th>Год</th>"
         "<th>Месяц</th>"
         "<th>Зарплата</th>"
         "<th>Больничные</th>"
         "<th>Командировки</th>"
         "<th>Б/д</th>"
         "<th>К/д</th>"
         "</tr>"
         "</thead>"
         "<tbody>"
         (apply str
           (map (fn [h]
                  (str "<tr>"
                       "<td>" (:год h) "</td>"
                       "<td>" (:месяц h) "</td>"
                       "<td style='color: #c62828; font-weight: bold;'>" (:общая_зарплата h) " руб.</td>"
                       "<td>" (if (> (:зарплата_за_больничные_дни h 0) 0) (str (:зарплата_за_больничные_дни h) " руб.") "-") "</td>"
                       "<td>" (if (> (:зарплата_за_командировочные_дни h 0) 0) (str (:зарплата_за_командировочные_дни h) " руб.") "-") "</td>"
                       "<td>" (:больничные_дни h) "</td>"
                       "<td>" (:командировочные_дни h) "</td>"
                       "</tr>"))
                history))
         "</tbody>"
         "</table>"
         "</div>")))

(defn render-salary-page [worker salary-info salary-history]
  "Рендер страницы зарплаты работника"
  (wrap-html
    (str "<div class='form-container'>"
         "<h2>💰 Расчет зарплаты</h2>"
         
         ;; Хлебные крошки
         "<div style='margin-bottom: 20px;'>"
         "<a href='/workers' style='color: #667eea; text-decoration: none;'>&larr; Назад к списку работников</a>"
         "</div>"
         
         ;; Информация о работнике
         "<div style='background: #f5f5f5; padding: 15px; border-radius: 6px; margin-bottom: 20px;'>"
         "<p style='margin: 5px 0;'><strong>ФИО:</strong> " (:фамилия worker) " " (:имя worker) " " (or (:отчество worker) "") "</p>"
         "<p style='margin: 5px 0;'><strong>Дата приема:</strong> " (:дата_приема worker) "</p>"
         "</div>"
         
         ;; Детали зарплаты
         (render-salary-details salary-info)
         
         ;; История зарплат
         (render-salary-history salary-history)
         
         "</div>")
    (str "Зарплата: " (:фамилия worker) " " (:имя worker))))

;; ======================================================================
;; Страница учета рабочего времени
;; ======================================================================

(defn- render-work-time-table [records worker-id]
  "Рендер таблицы учета рабочего времени"
  (if (empty? records)
    "<div class='empty-state'>Нет записей учета рабочего времени</div>"
    (str "<table class='data-table'>"
         "<thead>"
         "<tr>"
         "<th>Год</th>"
         "<th>Месяц</th>"
         "<th>План, ч</th>"
         "<th>Факт, ч</th>"
         "<th>Отработано дней</th>"
         "<th>Часов в день</th>"
         "<th>Всего отработано</th>"
         "<th>Больничные</th>"
         "<th>Командировки</th>"
         "<th>Действия</th>"
         "</tr>"
         "</thead>"
         "<tbody>"
         (apply str
           (map (fn [r]
                  (str "<tr>"
                       "<td>" (:год r) "</td>"
                       "<td>" (:месяц r) "</td>"
                       "<td style='color: #1976d2; font-weight: bold;'>" (:всего_часов_за_месяц_по_плану r) "</td>"
                       "<td style='color: #43a047; font-weight: bold;'>" (:всего_часов_в_месяц_по_факту r) "</td>"
                       "<td>" (or (:количество_отработанных_дней r) "-") "</td>"
                       "<td>" (or (:количество_рабочих_часов_в_день r) "-") "</td>"
                       "<td style='color: #c62828; font-weight: bold;'>" (or (:всего_отработанных_часов r) "-") "</td>"
                       "<td style='color: #ff9800;'>" (:больничные_дни r) "</td>"
                       "<td style='color: #2196f3;'>" (:командировочные_дни r) "</td>"
                       "<td>"
                       "<a href='/work-time/" (:id r) "/edit' class='btn btn-sm btn-warning' title='Редактировать'>✏️</a>"
                       "</td>"
                       "</tr>"))
                records))
         "</tbody>"
         "</table>")))

(defn render-work-time-page [worker records]
  "Рендер страницы учета рабочего времени работника"
  (wrap-html
    (str "<div class='form-container'>"
         "<h2>⏰ Учет рабочего времени</h2>"
         
         ;; Хлебные крошки
         "<div style='margin-bottom: 20px;'>"
         "<a href='/workers' style='color: #667eea; text-decoration: none;'>&larr; Назад к списку работников</a> | "
         "<a href='/workers/" (:id worker) "/salary' style='color: #4CAF50; text-decoration: none;'>💰 Зарплата</a>"
         "</div>"
         
         ;; Информация о работнике
         "<div style='background: #f5f5f5; padding: 15px; border-radius: 6px; margin-bottom: 20px;'>"
         "<p style='margin: 5px 0;'><strong>👨‍💼 ФИО:</strong> " (:фамилия worker) " " (:имя worker) " " (or (:отчество worker) "") "</p>"
         "<p style='margin: 5px 0;'><strong>🏭 Цех:</strong> " (:цех worker) "</p>"
         "<p style='margin: 5px 0;'><strong>📅 Дата приема:</strong> " (:дата_приема worker) "</p>"
         "</div>"
         
         ;; Таблица учета времени
         "<div class='table-container'>"
         (render-work-time-table records (:id worker))
         "</div>"
         
         "</div>")
    (str "Учет времени: " (:фамилия worker) " " (:имя worker))))

(defn render-edit-work-time-form [work-time-record worker & [options]]
  "Рендер формы редактирования записи учета рабочего времени"
  (let [errors (:errors options)]
    (wrap-html
      (str "<div class='form-container'>"
           "<h2>✏️ Редактирование учета рабочего времени</h2>"
           
           ;; Хлебные крошки
           "<div style='margin-bottom: 20px;'>"
           "<a href='/workers/" (:id worker) "/work-time' style='color: #667eea; text-decoration: none;'>&larr; Назад к учету времени</a>"
           "</div>"
           
           ;; Информация о работнике
           "<div style='background: #f5f5f5; padding: 15px; border-radius: 6px; margin-bottom: 20px;'>"
           "<p style='margin: 5px 0;'><strong>👨‍💼 Работник:</strong> " (:фамилия worker) " " (:имя worker) " " (or (:отчество worker) "") "</p>"
           "<p style='margin: 5px 0;'><strong>🏭 Цех:</strong> " (:цех worker) "</p>"
           "</div>"
           
           ;; Ошибки валидации
           (render-error-messages errors)
           
           "<form method='POST' action='/work-time/" (:id work-time-record) "/update' style='background: white; padding: 25px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1);'>"
           
           ;; Год
           "<div class='form-group'>"
           "<label>Год *</label>"
           "<input type='text' name='год' value='" (:год work-time-record) "' required>"
           "</div>"
           
           ;; Месяц
           "<div class='form-group'>"
           "<label>Месяц * (1-12)</label>"
           "<input type='text' name='месяц' value='" (:месяц work-time-record) "' required>"
           "</div>"
           
           ;; Плановые часы
           "<div class='form-group'>"
           "<label>Всего часов за месяц по плану *</label>"
           "<input type='text' name='всего_часов_за_месяц_по_плану' value='" (:всего_часов_за_месяц_по_плану work-time-record) "' required>"
           "</div>"
           
           ;; Фактические часы
           "<div class='form-group'>"
           "<label>Всего часов в месяц по факту *</label>"
           "<input type='text' name='всего_часов_в_месяц_по_факту' value='" (:всего_часов_в_месяц_по_факту work-time-record) "' required>"
           "</div>"
           
           ;; Отработанные дни
           "<div class='form-group'>"
           "<label>Количество отработанных дней</label>"
           "<input type='text' name='количество_отработанных_дней' value='" (or (:количество_отработанных_дней work-time-record) "") "'>"
           "</div>"
           
           ;; Рабочие часы в день
           "<div class='form-group'>"
           "<label>Количество рабочих часов в день</label>"
           "<input type='text' name='количество_рабочих_часов_в_день' value='" (or (:количество_рабочих_часов_в_день work-time-record) "") "'>"
           "</div>"
           
           ;; Всего отработанных часов
           "<div class='form-group'>"
           "<label>Всего отработанных часов</label>"
           "<input type='text' name='всего_отработанных_часов' value='" (or (:всего_отработанных_часов work-time-record) "") "'>"
           "</div>"
           
           ;; Сколько должны отработать
           "<div class='form-group'>"
           "<label>Сколько должны отработать</label>"
           "<input type='text' name='сколько_должны_отработать' value='" (or (:сколько_должны_отработать work-time-record) "") "'>"
           "</div>"
           
           ;; Больничные дни
           "<div class='form-group'>"
           "<label>Больничные дни</label>"
           "<input type='text' name='больничные_дни' value='" (or (:больничные_дни work-time-record) "0") "'>"
           "</div>"
           
           ;; Командировочные дни
           "<div class='form-group'>"
           "<label>Командировочные дни</label>"
           "<input type='text' name='командировочные_дни' value='" (or (:командировочные_дни work-time-record) "0") "'>"
           "</div>"
           
           ;; Кнопки
           "<div style='display: flex; gap: 10px; margin-top: 20px;'>"
           "<button type='submit' class='btn btn-primary'>Сохранить</button>"
           "<a href='/workers/" (:id worker) "/work-time' class='btn btn-secondary'>Отмена</a>"
           "</div>"
           
           "</form>"
           "</div>")
      (str "Редактирование времени: " (:фамилия worker) " " (:имя worker)))))

;; ======================================================================
;; Страница всех таблиц
;; ======================================================================

(defn render-all-tables-page [tables-data]
  "Рендер страницы со всеми таблицами БД"
  (let [content (apply str 
                (map (fn [tbl]
                       (str "<h3>" (:table tbl) "</h3>"
                            "<table border='1' cellpadding='8' cellspacing='0' style='border-collapse: collapse; margin: 10px 0; width: 100%;'>"
                            "<thead style='background: #667eea; color: white;'>"
                            "<tr>" 
                            (apply str 
                              (map (fn [col] 
                                     (str "<th style='padding: 10px;'>" col "</th>")) 
                                   (keys (first (:rows tbl)))))
                            "</tr>"
                            "</thead>"
                            "<tbody>"
                            (apply str 
                              (map (fn [row]
                                     (str "<tr>"
                                          (apply str 
                                            (map (fn [[k v]] 
                                                   (str "<td style='padding: 8px; border: 1px solid #ddd;'>" 
                                                        (if (nil? v) "NULL" (str v)) 
                                                        "</td>"))
                                                 row))
                                          "</tr>"))
                                    (:rows tbl)))
                            "</tbody>"
                            "</table>"))
                     tables-data))]
    (wrap-html content "Все таблицы")))

;; ======================================================================
;; Страница ошибки
;; ======================================================================

(defn render-error-page [message]
  "Рендер страницы ошибки"
  (wrap-html 
    (str "<div class='alert alert-error'>" message "</div>")
    "Ошибка"))
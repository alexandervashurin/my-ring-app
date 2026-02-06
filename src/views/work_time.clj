(ns my-ring-app.views.work-time
  (:require [my-ring-app.views.layout :refer [wrap-html]]
            [my-ring-app.views.helpers :as helpers]))

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
         (helpers/breadcrumbs
           "<a href='/workers' style='color: #667eea; text-decoration: none;'>&larr; Назад к списку работников</a>"
           "<a href='/workers/" (:id worker) "/salary' style='color: #4CAF50; text-decoration: none;'>💰 Зарплата</a>")
         
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
    (str "Учет времени: " (:фамилия worker) " " (:имя worker))
    "workers"))

(defn render-edit-work-time-form [work-time-record worker & [options]]
  "Рендер формы редактирования записи учета рабочего времени"
  (let [errors (:errors options)]
    (wrap-html
      (str "<div class='form-container'>"
           "<h2>✏️ Редактирование учета рабочего времени</h2>"
           
           ;; Хлебные крошки
           (helpers/breadcrumbs
             "<a href='/workers/" (:id worker) "/work-time' style='color: #667eea; text-decoration: none;'>&larr; Назад к учету времени</a>")
           
           ;; Информация о работнике
           "<div style='background: #f5f5f5; padding: 15px; border-radius: 6px; margin-bottom: 20px;'>"
           "<p style='margin: 5px 0;'><strong>👨‍💼 Работник:</strong> " (:фамилия worker) " " (:имя worker) " " (or (:отчество worker) "") "</p>"
           "<p style='margin: 5px 0;'><strong>🏭 Цех:</strong> " (:цех worker) "</p>"
           "</div>"
           
           ;; Ошибки валидации
           (helpers/render-error-messages errors)
           
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
      (str "Редактирование времени: " (:фамилия worker) " " (:имя worker))
      "workers")))
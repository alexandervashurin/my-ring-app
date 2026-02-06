(ns my-ring-app.views.salary
  (:require [my-ring-app.views.layout :refer [wrap-html]]
            [my-ring-app.views.helpers :as helpers]))

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
         (helpers/breadcrumbs
           "<a href='/workers' style='color: #667eea; text-decoration: none;'>&larr; Назад к списку работников</a>")
         
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
    (str "Зарплата: " (:фамилия worker) " " (:имя worker))
    "workers"))
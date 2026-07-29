(ns my-ring-app.views.salary
  (:require [my-ring-app.views.layout :refer [wrap-html html-escape]]
            [my-ring-app.views.helpers :as helpers]
            [my-ring-app.config :refer [url]]))

(def ^:private month-names
  {1 "январь" 2 "февраль" 3 "март" 4 "апрель"
   5 "май" 6 "июнь" 7 "июль" 8 "август"
   9 "сентябрь" 10 "октябрь" 11 "ноябрь" 12 "декабрь"})

(defn- render-salary-details
  "Рендер детальной информации о зарплате"
  [salary-info]
  (if salary-info
    (str "<div class='salary-summary'>"
         (str "<h3 >📊 Расчет за " (get month-names (:месяц salary-info) "?") " " (:год salary-info) "</h3>")
         
         "<div class='salary-grid'>"
         
         "<div>"
         "<p><strong>👨‍💼 ФИО:</strong> <span class='fs-medium text-blue'>" (html-escape (:фамилия salary-info)) " " (html-escape (:имя salary-info)) " " (html-escape (or (:отчество salary-info) "")) "</span></p>"
         "<p><strong>🏭 Цех:</strong> " (html-escape (:название_цеха salary-info)) "</p>"
         "<p><strong>💰 Система оплаты:</strong> " (html-escape (:название_системы salary-info)) "</p>"
         "<p><strong>⏰ Отработано часов:</strong> <span class='fw-bold text-green'>" (html-escape (str (:всего_отработанных_часов salary-info))) " ч.</span></p>"
         "<p><strong>🏥 Больничные дни:</strong> <span class='text-orange'>" (html-escape (str (:больничные_дни salary-info))) "</span></p>"
         "<p><strong>✈️ Командировочные дни:</strong> <span class='cell-info'>" (html-escape (str (:командировочные_дни salary-info))) "</span></p>"
         "</div>"
         
         "<div class='salary-extra'>"
         "<h4 >💰 Начислено:</h4>"
         (when (:оклад_в_месяц salary-info)
           (str "<p ><strong>💵 Оклад:</strong> <span class='text-green fw-bold'>" (html-escape (str (:оклад_в_месяц salary-info))) " руб.</span></p>"))
         (when (:ставка_в_час salary-info)
           (str "<p ><strong>⏱️ Ставка:</strong> <span class='text-blue fw-bold'>" (html-escape (str (:ставка_в_час salary-info))) " руб./час</span></p>"))
         (when (> (:зарплата_за_больничные_дни salary-info 0) 0)
           (str "<p ><strong>🏥 За больничные:</strong> <span class='text-orange fw-bold'>" (html-escape (str (:зарплата_за_больничные_дни salary-info))) " руб.</span></p>"))
         (when (> (:зарплата_за_командировочные_дни salary-info 0) 0)
           (str "<p ><strong>✈️ За командировки:</strong> <span class='cell-info fw-bold'>" (html-escape (str (:зарплата_за_командировочные_дни salary-info))) " руб.</span></p>"))
         "<hr >"
         "<p class='total'>"
         "Итого: <span class='text-red'>" (html-escape (str (:общая_зарплата salary-info))) " руб.</span>"
         "</p>"
         "</div>"
         
         "</div>"
         
         "</div>")
    "<div class='alert alert-error'>Данные о зарплате не найдены</div>"))

(defn- render-salary-history
  "Рендер истории зарплат"
  [history]
  (if (empty? history)
    ""
    (str "<div class='table-container mt-30'>"
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
                        "<td>" (html-escape (str (:год h))) "</td>"
                        "<td>" (html-escape (str (:месяц h))) "</td>"
                        "<td class='text-red fw-bold'>" (html-escape (str (:общая_зарплата h))) " руб.</td>"
                        "<td>" (if (> (:зарплата_за_больничные_дни h 0) 0) (str (html-escape (str (:зарплата_за_больничные_дни h))) " руб.") "-") "</td>"
                        "<td>" (if (> (:зарплата_за_командировочные_дни h 0) 0) (str (html-escape (str (:зарплата_за_командировочные_дни h))) " руб.") "-") "</td>"
                        "<td>" (html-escape (str (:больничные_дни h))) "</td>"
                        "<td>" (html-escape (str (:командировочные_дни h))) "</td>"
                        "</tr>"))
                history))
         "</tbody>"
         "</table>"
         "</div>")))

(defn render-salary-page
  "Рендер страницы зарплаты работника"
  [worker salary-info salary-history]
  (wrap-html
    (str "<div class='form-container'>"
         "<h2>💰 Расчет зарплаты</h2>"
         
         ;; Хлебные крошки
          (helpers/breadcrumbs
            (str "<a href='" (url "/workers") "' class='back-link'>&larr; Назад к списку работников</a>"))
         
         ;; Информация о работнике
         "<div class='info-box'>"
         "<p ><strong>ФИО:</strong> " (html-escape (:фамилия worker)) " " (html-escape (:имя worker)) " " (html-escape (or (:отчество worker) "")) "</p>"
         "<p ><strong>Дата приема:</strong> " (html-escape (:дата_приема worker)) "</p>"
         "</div>"
         
         ;; Детали зарплаты
         (render-salary-details salary-info)
         
         ;; История зарплат
         (render-salary-history salary-history)
         
         "</div>")
    (str "Зарплата: " (html-escape (:фамилия worker)) " " (html-escape (:имя worker)))
    "workers"))
(ns my-ring-app.views.work-time
  (:require [my-ring-app.views.layout :refer [wrap-html html-escape csrf-field]]
            [my-ring-app.views.helpers :as helpers]
            [my-ring-app.config :refer [url]]))

(defn- render-work-time-table
  "Рендер таблицы учета рабочего времени"
  [records worker-id]
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
                        "<td>" (html-escape (str (:год r))) "</td>"
                        "<td>" (html-escape (str (:месяц r))) "</td>"
                        "<td class='cell-blue'>" (html-escape (str (:всего_часов_за_месяц_по_плану r))) "</td>"
                        "<td class='cell-green'>" (html-escape (str (:всего_часов_в_месяц_по_факту r))) "</td>"
                        "<td>" (html-escape (str (or (:количество_отработанных_дней r) "-"))) "</td>"
                        "<td>" (html-escape (str (or (:количество_рабочих_часов_в_день r) "-"))) "</td>"
                        "<td class='cell-red'>" (html-escape (str (or (:всего_отработанных_часов r) "-"))) "</td>"
                        "<td class='cell-orange'>" (html-escape (str (:больничные_дни r))) "</td>"
                        "<td class='cell-info'>" (html-escape (str (:командировочные_дни r))) "</td>"
                       "<td>"
                        "<a href='" (url (str "/work-time/" (html-escape (str (:id r))) "/edit")) "' class='btn btn-sm btn-warning' title='Редактировать'>✏️</a>"
                       "</td>"
                       "</tr>"))
                records))
         "</tbody>"
         "</table>")))

(defn render-work-time-page
  "Рендер страницы учета рабочего времени работника"
  [worker records]
  (wrap-html
    (str "<div class='form-container'>"
         "<h2>⏰ Учет рабочего времени</h2>"
         
         ;; Хлебные крошки
            (helpers/breadcrumbs
              (str "<a href='" (url "/workers") "' class='back-link'>&larr; Назад к списку работников</a>")
               (str "<a href='" (url (str "/workers/" (html-escape (str (:id worker))) "/salary")) "' class='green-link'>💰 Зарплата</a>"))
         
         ;; Информация о работнике
         "<div class='info-box'>"
          "<p ><strong>👨‍💼 ФИО:</strong> " (helpers/render-full-name worker) "</p>"
          "<p ><strong>🏭 Цех:</strong> " (html-escape (:цех worker)) "</p>"
          "<p ><strong>📅 Дата приема:</strong> " (html-escape (:дата_приема worker)) "</p>"
         "</div>"
         
         ;; Таблица учета времени
         "<div class='table-container'>"
         (render-work-time-table records (:id worker))
         "</div>"
         
         "</div>")
    (str "Учет времени: " (helpers/render-short-name worker))
    "workers"))

(defn render-edit-work-time-form
  "Рендер формы редактирования записи учета рабочего времени"
  [work-time-record worker & [options]]
  (let [errors (:errors options)]
    (wrap-html
      (str "<div class='form-container'>"
           "<h2>✏️ Редактирование учета рабочего времени</h2>"
           
           ;; Хлебные крошки
            (helpers/breadcrumbs
              (str "<a href='" (url (str "/workers/" (html-escape (str (:id worker))) "/work-time")) "' class='back-link'>&larr; Назад к учету времени</a>"))
           
           ;; Информация о работнике
           "<div class='info-box'>"
            "<p ><strong>👨‍💼 Работник:</strong> " (helpers/render-full-name worker) "</p>"
            "<p ><strong>🏭 Цех:</strong> " (html-escape (:цех worker)) "</p>"
           "</div>"
           
           ;; Ошибки валидации
           (helpers/render-error-messages errors)
           
           (str "<form method='POST' action='" (url (str "/work-time/" (html-escape (str (:id work-time-record))) "/update")) "' class='form-card'>")
           (csrf-field)

           (helpers/text-input "Год" :год (:год work-time-record) {:required true})
           (helpers/text-input "Месяц (1-12)" :месяц (:месяц work-time-record) {:required true})
           (helpers/text-input "Всего часов за месяц по плану" :всего_часов_за_месяц_по_плану (:всего_часов_за_месяц_по_плану work-time-record) {:required true})
           (helpers/text-input "Всего часов в месяц по факту" :всего_часов_в_месяц_по_факту (:всего_часов_в_месяц_по_факту work-time-record) {:required true})
           (helpers/text-input "Количество отработанных дней" :количество_отработанных_дней (:количество_отработанных_дней work-time-record))
           (helpers/text-input "Количество рабочих часов в день" :количество_рабочих_часов_в_день (:количество_рабочих_часов_в_день work-time-record))
           (helpers/text-input "Всего отработанных часов" :всего_отработанных_часов (:всего_отработанных_часов work-time-record))
           (helpers/text-input "Сколько должны отработать" :сколько_должны_отработать (:сколько_должны_отработать work-time-record))
           (helpers/text-input "Больничные дни" :больничные_дни (:больничные_дни work-time-record))
           (helpers/text-input "Командировочные дни" :командировочные_дни (:командировочные_дни work-time-record))
           
           ;; Кнопки
           "<div class='flex-row mt-20'>"
           "<button type='submit' class='btn btn-primary'>Сохранить</button>"
            (str "<a href='" (url (str "/workers/" (html-escape (str (:id worker))) "/work-time")) "' class='btn btn-secondary'>Отмена</a>")
           "</div>"
           
           "</form>"
           "</div>")
      (str "Редактирование времени: " (helpers/render-short-name worker))
      "workers")))
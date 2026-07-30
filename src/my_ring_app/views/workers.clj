(ns my-ring-app.views.workers
  (:require [my-ring-app.views.layout :refer [wrap-html html-escape csrf-field]]
            [my-ring-app.views.helpers :as helpers]
            [my-ring-app.config :refer [url]]))

(defn render-workers-table
  "Рендер таблицы работников"
  [workers search-query]
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
                                           "<td>" (html-escape (str (:id w))) "</td>"
                                           "<td><strong>" (helpers/render-full-name w) "</strong></td>"
                                           "<td>" (html-escape (:дата_приема w)) "</td>"
                                           "<td>" (html-escape (:цех w)) "</td>"
                                           "<td>" (html-escape (:система w)) "</td>"
                                           "<td>" (html-escape (:категория w)) "</td>"
                                           "<td>" (html-escape (:разряд w)) "</td>"
                                           "<td>" (html-escape (:режим w)) "</td>"
                                           "<td>"
                                            "<a href='" (url (str "/workers/" (:id w) "/work-time")) "' class='btn btn-sm btn-info' title='Учет времени'>⏰</a> "
                                            "<a href='" (url (str "/workers/" (:id w) "/salary")) "' class='btn btn-sm btn-success' title='Зарплата'>💰</a> "
                                            "<a href='" (url (str "/workers/" (:id w) "/edit")) "' class='btn btn-sm btn-warning' title='Редактировать'>✏️</a> "
                                             "<form method='POST' action='" (url (str "/workers/" (:id w) "/delete")) "' class='inline-form' onsubmit='" (html-escape (str "return confirm(\"Удалить работника " (helpers/escape-js-string (:фамилия w)) " " (helpers/escape-js-string (:имя w)) "?\")")) "'>"
                                           (csrf-field)
                                           "<button type='submit' class='btn btn-sm btn-danger' title='Удалить'>🗑️</button>"
                                           "</form>"
                                           "</td>"
                                           "</tr>"))
                                    workers))
                             "</tbody>"
                             "</table>"))]
    (str "<div class='table-container'>"
         "<div class='flex-between mb-20'>"
         "<h2>📋 Список работников</h2>"
         "<a href='" (url "/workers/new") "' class='btn btn-success'>➕ Добавить работника</a>"
         "</div>"
         
         ;; Форма поиска
         "<div class='search-box'>"
         "<form method='GET' action='" (url "/workers") "' >"
         "<div >"
         "<label >Поиск по ФИО или цеху:</label>"
         "<input type='text' name='search' placeholder='Введите фамилию, имя, отчество или цех...' value='" (html-escape (or search-query "")) "'"
         " >"
         "</div>"
         "<button type='submit' class='btn btn-primary' >🔍 Найти</button>"
         (when search-query
            (str "<a href='" (url "/workers") "' class='btn btn-secondary' >Сбросить фильтр</a>"
                "<div class='search-results-info'>"
                 "<strong>Результаты поиска:</strong> найдено " (count workers) " работников по запросу: <em>\"" (html-escape search-query) "\"</em>"
                "</div>"))
         "</form>"
         "</div>"
         
         table-content
         "</div>")))

(defn render-workers-page
  "Рендер страницы списка работников"
  [workers search-query]
  (wrap-html (render-workers-table workers search-query) "Работники" "workers"))

(defn render-worker-form
  "Рендер формы создания/редактирования работника"
  [mode worker-data цеха системы_оплаты категории разряды режимы оклады ставки & [options]]
  (let [цех-id (:цех_id worker-data)
        система-id (:система_оплаты_id worker-data)
        категория-id (:категория_работника_id worker-data)
        разряд-id (:разряд_id worker-data)
        режим-id (:режим_работы_id worker-data)
        оклад-id (:оклад_id worker-data)
        ставка-id (:почасовая_ставка_id worker-data)
        form-title (if (= mode :create) "➕ Добавить работника" "✏️ Редактировать работника")
        form-action (if (= mode :edit) (url (str "/workers/" (:id worker-data) "/update")) (url "/workers/create"))
        errors (:errors options)]
    
    (str "<div class='form-container'>"
         "<h2>" (html-escape form-title) "</h2>"

         ;; Отображение ошибок валидации
         (helpers/render-error-messages errors)

         "<form method='POST' action='" (html-escape form-action) "' class='form-card'>"
         (csrf-field)

         (helpers/text-input "Фамилия" :фамилия (:фамилия worker-data) {:required true})
         (helpers/text-input "Имя" :имя (:имя worker-data) {:required true})
         (helpers/text-input "Отчество" :отчество (:отчество worker-data))
         (helpers/text-input "Дата приема" :дата_приема (:дата_приема worker-data) {:required true :type "date"})
         (helpers/select-input "Цех" :цех_id цеха :id :название_цеха цех-id {:required true})
         (helpers/select-input "Система оплаты" :система_оплаты_id системы_оплаты :id :название_системы система-id
                               {:required true :attrs {:onchange "togglePaymentFields(this.value)"}})
         (helpers/select-input "Категория работника" :категория_работника_id категории :id :название_категории категория-id {:required true})
         (helpers/select-input "Разряд" :разряд_id разряды :id :номер_разряда разряд-id {:required true})
         (helpers/select-input "Режим работы" :режим_работы_id режимы :id :название_режима режим-id {:required true})
         
          (helpers/select-input "Оклад" :оклад_id оклады :id :оклад_в_месяц оклад-id
                                {:placeholder "Выберите оклад"
                                 :container-class "payment-field"
                                 :container-id "oklad-field"
                                 :container-style "display: none;"})

          (helpers/select-input "Почасовая ставка" :почасовая_ставка_id ставки :id :ставка_в_час ставка-id
                                {:placeholder "Выберите ставку"
                                 :container-class "payment-field"
                                 :container-id "stavka-field"
                                 :container-style "display: none;"})
         
         ;; Кнопки
         "<div class='flex-row mt-20'>"
         "<button type='submit' class='btn btn-primary'>"
         (if (= mode :create) "Создать" "Сохранить")
         "</button>"
         "<a href='" (url "/workers") "' class='btn btn-secondary'>Отмена</a>"
         "</div>"
         
         "</form>"
         "</div>")))

(defn render-new-worker-page
  "Рендер страницы создания нового работника"
  [цеха системы_оплаты категории разряды режимы оклады ставки & [options]]
  (let [errors (:errors options)
        worker-data (:worker-data options)]
    (wrap-html 
      (render-worker-form :create (or worker-data {}) цеха системы_оплаты категории разряды режимы оклады ставки {:errors errors})
      "Новый работник"
      "workers")))

(defn render-edit-worker-page
  "Рендер страницы редактирования работника"
  [worker цеха системы_оплаты категории разряды режимы оклады ставки & [options]]
  (let [errors (:errors options)]
    (wrap-html 
      (render-worker-form :edit worker цеха системы_оплаты категории разряды режимы оклады ставки {:errors errors})
      "Редактировать работника"
      "workers")))
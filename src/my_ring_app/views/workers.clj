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
                                           "<td><strong>" (html-escape (:фамилия w)) "</strong> " (html-escape (:имя w)) " " (html-escape (or (:отчество w) "")) "</td>"
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
         "<div class='flex-between' style='margin-bottom: 20px;'>"
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

         ;; Фамилия
         "<div class='form-group'>"
         "<label>Фамилия *</label>"
         "<input type='text' name='фамилия' value='" (html-escape (or (:фамилия worker-data) "")) "' required>"
         "</div>"

         ;; Имя
         "<div class='form-group'>"
         "<label>Имя *</label>"
         "<input type='text' name='имя' value='" (html-escape (or (:имя worker-data) "")) "' required>"
         "</div>"

         ;; Отчество
         "<div class='form-group'>"
         "<label>Отчество</label>"
         "<input type='text' name='отчество' value='" (html-escape (or (:отчество worker-data) "")) "'>"
         "</div>"

         ;; Дата приема
         "<div class='form-group'>"
         "<label>Дата приема *</label>"
         "<input type='date' name='дата_приема' value='" (html-escape (or (:дата_приема worker-data) "")) "' required>"
         "</div>"
         
         ;; Цех
         "<div class='form-group'>"
         "<label>Цех *</label>"
         "<select name='цех_id' required>"
         "<option value=''>Выберите цех</option>"
         (apply str (map #(helpers/option-tag % :id :название_цеха цех-id) цеха))
         "</select>"
         "</div>"
         
         ;; Система оплаты
         "<div class='form-group'>"
         "<label>Система оплаты *</label>"
         "<select name='система_оплаты_id' required onchange='togglePaymentFields(this.value)'>"
         "<option value=''>Выберите систему</option>"
         (apply str (map #(helpers/option-tag % :id :название_системы система-id) системы_оплаты))
         "</select>"
         "</div>"
         
         ;; Категория работника
         "<div class='form-group'>"
         "<label>Категория работника *</label>"
         "<select name='категория_работника_id' required>"
         "<option value=''>Выберите категорию</option>"
         (apply str (map #(helpers/option-tag % :id :название_категории категория-id) категории))
         "</select>"
         "</div>"
         
         ;; Разряд
         "<div class='form-group'>"
         "<label>Разряд *</label>"
         "<select name='разряд_id' required>"
         "<option value=''>Выберите разряд</option>"
         (apply str (map #(helpers/option-tag % :id :номер_разряда разряд-id) разряды))
         "</select>"
         "</div>"
         
         ;; Режим работы
         "<div class='form-group'>"
         "<label>Режим работы *</label>"
         "<select name='режим_работы_id' required>"
         "<option value=''>Выберите режим</option>"
         (apply str (map #(helpers/option-tag % :id :название_режима режим-id) режимы))
         "</select>"
         "</div>"
         
         ;; Оклад (показывается только при системе оплаты "Оклад")
         "<div class='form-group payment-field' id='oklad-field' style='display: none;'>"
         "<label>Оклад</label>"
         "<select name='оклад_id'>"
         "<option value=''>Выберите оклад</option>"
         (apply str (map #(helpers/option-tag % :id :оклад_в_месяц оклад-id) оклады))
         "</select>"
         "</div>"
         
         ;; Почасовая ставка (показывается только при системе оплаты "Почасовая")
         "<div class='form-group payment-field' id='stavka-field' style='display: none;'>"
         "<label>Почасовая ставка</label>"
         "<select name='почасовая_ставка_id'>"
         "<option value=''>Выберите ставку</option>"
         (apply str (map #(helpers/option-tag % :id :ставка_в_час ставка-id) ставки))
         "</select>"
         "</div>"
         
         ;; Кнопки
         "<div class='flex-row' style='margin-top: 20px;'>"
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
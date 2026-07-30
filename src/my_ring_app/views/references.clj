(ns my-ring-app.views.references
  (:require [clojure.string :as str]
            [my-ring-app.views.layout :refer [wrap-html html-escape csrf-field]]
            [my-ring-app.views.helpers :as helpers]
            [my-ring-app.config :refer [url]]))

(defn render-list-page
  [table-title rows columns & [active-page]]
  (let [has-rows (seq rows)
        page (or active-page "references")]
    (wrap-html
      (str "<div class='page-header'>"
           "<h2>" (html-escape table-title) "</h2>"
           "<a href='" (url (str "/" page "/new")) "' class='btn btn-primary'>Добавить</a>"
           "</div>"
           (if has-rows
             (str "<div class='table-container'>"
                  "<table class='data-table'>"
                  "<thead><tr>"
                  (apply str (for [[col _] columns] (str "<th>" col "</th>")))
                  "<th>Действия</th>"
                  "</tr></thead>"
                  "<tbody>"
                  (apply str
                    (for [row rows]
                      (str "<tr>"
                           (apply str (for [[_ field] columns]
                                        (str "<td>" (html-escape (str (get row field ""))) "</td>")))
                           "<td class='actions'>"
                           "<a href='" (url (str "/" page "/" (:id row) "/edit")) "' class='btn btn-sm btn-info'>✏️</a>"
                           "<form method='POST' action='" (url (str "/" page "/" (:id row) "/delete")) "' class='inline-form' onsubmit=\"return confirm('Удалить запись?');\">"
                           (csrf-field)
                           "<button type='submit' class='btn btn-sm btn-danger'>🗑️</button>"
                           "</form>"
                           "</td>"
                           "</tr>")))
                  "</tbody></table></div>")
             (str "<div class='empty-state'>Нет записей</div>")))
      table-title
      page)))

(defn render-form-page
  [mode table-title fields data errors & [active-page]]
  (let [page (or active-page "references")
        is-create (= mode :create)
        form-action (if is-create
                      (str "/" page "/create")
                      (str "/" page "/" (:id data) "/update"))
        submit-text (if is-create "Создать" "Сохранить")]
    (wrap-html
      (str (helpers/breadcrumbs
             (if is-create "Новая запись" "Редактирование"))
           "<div class='form-container'>"
           "<h2>" (if is-create "Добавление" "Редактирование") ": " table-title "</h2>"
           (helpers/render-error-messages errors)
           "<form method='POST' action='" (url form-action) "' class='data-form'>"
           (csrf-field)
           (apply str
             (for [[label field type & opts] fields]
               (let [value (get data field)
                     opts-map (when opts (apply hash-map opts))]
                 (case type
                   :text (helpers/text-input label field value opts-map)
                   :number (helpers/text-input label field value (assoc opts-map :type "number"))
                   :textarea (helpers/textarea-input label field value opts-map)
                   :select (let [items (or (:items opts-map) [])
                                 value-key (or (:value-key opts-map) :id)
                                 label-key (or (:label-key opts-map) label)]
                             (helpers/select-input label field items value-key label-key value opts-map))
                   (helpers/text-input label field value opts-map)))))
           "<div class='form-actions'>"
           "<button type='submit' class='btn btn-primary'>" submit-text "</button>"
           " <a href='" (url (str "/" page)) "' class='btn btn-secondary'>Отмена</a>"
           "</div>"
           "</form>"
            "</div>")
      table-title
      page)))

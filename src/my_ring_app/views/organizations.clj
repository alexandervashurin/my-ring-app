(ns my-ring-app.views.organizations
  "Представления для управления организациями"
  (:require [clojure.string :as string]
            [my-ring-app.views.layout :as layout]))

(defn- org-table-html [organizations]
  (if (empty? organizations)
    "<p>Организации не найдены.</p>"
    (str
     "<table class='data-table'>"
     "<thead><tr>"
     "<th>ID</th><th>Название</th><th>ИНН</th><th>Телефон</th><th>Email</th><th>Действия</th>"
     "</tr></thead>"
     "<tbody>"
     (apply str
            (for [org organizations]
              (str "<tr>"
                   "<td>" (:id org) "</td>"
                   "<td><a href='/organizations/" (:id org) "'>"
                   (layout/html-escape (:name org)) "</a></td>"
                   "<td>" (layout/html-escape (or (:inn org) "-")) "</td>"
                   "<td>" (layout/html-escape (or (:phone org) "-")) "</td>"
                   "<td>" (layout/html-escape (or (:email org) "-")) "</td>"
                   "<td>"
                   "<a href='/organizations/" (:id org)
                   "/edit' class='btn btn-sm btn-info'>Редактировать</a> "
                   "<form method='POST' action='/organizations/" (:id org)
                   "/delete' class='inline-form'>"
                   (layout/csrf-field)
                   "<button type='submit' class='btn btn-sm btn-danger' "
                   "onclick=\"return confirm('Деактивировать организацию?')\">"
                   "Деактивировать</button></form>"
                    "</td></tr>")))
     "</tbody></table>")))

(defn render-organizations-page
  "Страница списка организаций"
  [organizations user & {:keys [success error]}]
  (let [lang "ru"]
    (layout/wrap-html
     (str
      "<div class='page-header'>"
      "<h2>Организации</h2>"
      (when success
        (str "<div class='alert alert-success'>"
             (layout/html-escape success) "</div>"))
      (when error
        (str "<div class='alert alert-error'>"
             (layout/html-escape error) "</div>"))
      "<a href='/organizations/new' class='btn btn-primary'>+ Новая организация</a>"
      "</div>"
      (org-table-html organizations))
     "Организации" "organizations" lang)))

(defn render-organization-form
  "Форма создания/редактирования организации"
  [organization user & {:keys [errors]}]
  (let [lang "ru"
        is-edit (some? (:id organization))
        title (if is-edit "Редактирование организации" "Новая организация")
        action (if is-edit
                 (str "/organizations/" (:id organization) "/update")
                 "/organizations/create")]
    (layout/wrap-html
     (str
      "<div class='page-header'>"
      "<h2>" title "</h2>"
      "</div>"
      (when (seq errors)
        (str "<div class='alert alert-error'>"
             (string/join "<br>" (map layout/html-escape errors))
             "</div>"))
      "<form method='POST' action='" action "' class='form'>"
      (layout/csrf-field)
      "<div class='form-group'>"
      "<label for='name'>Название *</label>"
      "<input type='text' id='name' name='name' value='"
      (layout/html-escape (or (:name organization) ""))
      "' required class='form-control'>"
      "</div>"
      "<div class='form-group'>"
      "<label for='inn'>ИНН</label>"
      "<input type='text' id='inn' name='inn' value='"
      (layout/html-escape (or (:inn organization) ""))
      "' class='form-control'>"
      "</div>"
      "<div class='form-group'>"
      "<label for='phone'>Телефон</label>"
      "<input type='text' id='phone' name='phone' value='"
      (layout/html-escape (or (:phone organization) ""))
      "' class='form-control'>"
      "</div>"
      "<div class='form-group'>"
      "<label for='email'>Email</label>"
      "<input type='email' id='email' name='email' value='"
      (layout/html-escape (or (:email organization) ""))
      "' class='form-control'>"
      "</div>"
      "<div class='form-group'>"
      "<label for='address'>Адрес</label>"
      "<input type='text' id='address' name='address' value='"
      (layout/html-escape (or (:address organization) ""))
      "' class='form-control'>"
      "</div>"
      "<div class='form-actions'>"
      "<button type='submit' class='btn btn-primary'>"
      (if is-edit "Сохранить" "Создать")
      "</button> "
      "<a href='/organizations' class='btn btn-secondary'>Отмена</a>"
      "</div>"
      "</form>")
     title "organizations" lang)))

(defn render-organization-detail
  "Страница детального просмотра организации"
  [organization user]
  (let [lang "ru"]
    (layout/wrap-html
     (str
      "<div class='page-header'>"
      "<h2>" (layout/html-escape (:name organization)) "</h2>"
      "<a href='/organizations/" (:id organization)
      "/edit' class='btn btn-info'>Редактировать</a> "
      "<a href='/organizations' class='btn btn-secondary'>Назад</a>"
      "</div>"
      "<div class='detail-card'>"
      "<table class='detail-table'>"
      "<tr><td><strong>ID:</strong></td><td>"
      (:id organization) "</td></tr>"
      "<tr><td><strong>Название:</strong></td><td>"
      (layout/html-escape (:name organization)) "</td></tr>"
      "<tr><td><strong>ИНН:</strong></td><td>"
      (layout/html-escape (or (:inn organization) "-")) "</td></tr>"
      "<tr><td><strong>Телефон:</strong></td><td>"
      (layout/html-escape (or (:phone organization) "-")) "</td></tr>"
      "<tr><td><strong>Email:</strong></td><td>"
      (layout/html-escape (or (:email organization) "-")) "</td></tr>"
      "<tr><td><strong>Адрес:</strong></td><td>"
      (layout/html-escape (or (:address organization) "-")) "</td></tr>"
      "<tr><td><strong>Статус:</strong></td><td>"
      (if (:is_active organization) "Активна" "Неактивна")
      "</td></tr>"
      "<tr><td><strong>Создана:</strong></td><td>"
      (layout/html-escape (str (:created_at organization)))
      "</td></tr>"
      "</table>"
      "</div>")
     (:name organization) "organizations" lang)))

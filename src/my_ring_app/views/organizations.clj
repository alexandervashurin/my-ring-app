(ns my-ring-app.views.organizations
  "Представления для управления организациями"
  (:require [clojure.string :as string]
            [my-ring-app.views.layout :as layout]
            [my-ring-app.tariff :as tariff]
            [my-ring-app.config :refer [url]]))

(def ^:private org-role-labels
  {"org_admin" "Администратор организации"
   "org_manager" "Менеджер организации"
   "org_hr" "HR организации"
   "org_viewer" "Наблюдатель организации"})

(def ^:private org-role-options
  [["" "По умолчанию (глобальная роль)"]
   ["org_admin" "Администратор организации"]
   ["org_manager" "Менеджер организации"]
   ["org_hr" "HR организации"]
   ["org_viewer" "Наблюдатель организации"]])

(defn- org-user-table-html [org-id users current-user]
  (if (empty? users)
    "<p>Пользователи не найдены.</p>"
    (str
     "<h3>Пользователи организации</h3>"
     "<table class='data-table'>"
     "<thead><tr>"
     "<th>ID</th><th>Имя пользователя</th><th>Email</th><th>Глобальная роль</th><th>Роль в организации</th><th>Действия</th>"
     "</tr></thead>"
     "<tbody>"
     (apply str
            (for [u users]
              (let [is-self (= (:id u) (:id current-user))
                    current-org-role (or (:org_role u) "")
                    role-label (or (get org-role-labels (:org_role u)) "По умолчанию")]
                (str "<tr>"
                     "<td>" (:id u) "</td>"
                     "<td>" (layout/html-escape (:username u)) "</td>"
                     "<td>" (layout/html-escape (:email u)) "</td>"
                     "<td>" (layout/html-escape (or (:role u) "-")) "</td>"
                     "<td>" (layout/html-escape role-label) "</td>"
                     "<td>"
                     (if is-self
                       "<em>текущий пользователь</em>"
                       (str
                        (str "<form method='POST' action='" (url (str "/organizations/" org-id "/users/" (:id u) "/role")) "' class='inline-form'>")
                        (layout/csrf-field)
                        "<select name='org_role' class='form-control form-control-sm'>"
                        (apply str
                               (for [[val label] org-role-options]
                                 (str "<option value='" val "'"
                                      (if (= val current-org-role) " selected" "")
                                      ">" label "</option>")))
                        "</select> "
                        "<button type='submit' class='btn btn-sm btn-primary'>Изменить</button>"
                        "</form>"))
                     "</td></tr>"))))
     "</tbody></table>")))

(defn- org-table-html [organizations]
  (if (empty? organizations)
    "<p>Организации не найдены.</p>"
    (str
     "<table class='data-table'>"
     "<thead><tr>"
     "<th>ID</th><th>Название</th><th>ИНН</th><th>Телефон</th><th>Email</th><th>Тариф</th><th>Действия</th>"
     "</tr></thead>"
     "<tbody>"
     (apply str
            (for [org organizations]
              (let [plan (try (tariff/get-org-plan (:id org)) (catch Exception _ nil))
                    plan-name (:name plan "Free")]
                (str "<tr>"
                     "<td>" (:id org) "</td>"
(str "<td><a href='" (url (str "/organizations/" (:id org))) "'>"
                      (layout/html-escape (:name org)) "</a></td>")
                     "<td>" (layout/html-escape (or (:inn org) "-")) "</td>"
                     "<td>" (layout/html-escape (or (:phone org) "-")) "</td>"
                     "<td>" (layout/html-escape (or (:email org) "-")) "</td>"
                     "<td>" (layout/html-escape plan-name) "</td>"
                     "<td>"
(str "<a href='" (url (str "/organizations/" (:id org)
                  "/edit")) "' class='btn btn-sm btn-info'>Редактировать</a> ")
                  (str "<form method='POST' action='" (url (str "/organizations/" (:id org)
                  "/delete")) "' class='inline-form'>")
                     (layout/csrf-field)
                     "<button type='submit' class='btn btn-sm btn-danger' "
                     "onclick=\"return confirm('Деактивировать организацию?')\">"
                     "Деактивировать</button></form>"
                     "</td></tr>"))))
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
      (str "<a href='" (url "/organizations/new") "' class='btn btn-primary'>+ Новая организация</a>")
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
                  (url (str "/organizations/" (:id organization) "/update"))
                  (url "/organizations/create"))]
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
      (str "<a href='" (url "/organizations") "' class='btn btn-secondary'>Отмена</a>")
      "</div>"
      "</form>")
     title "organizations" lang)))

(defn- render-tariff-info
  [organization current-user]
  (let [plan (tariff/get-org-plan (:id organization))
        worker-check (tariff/check-worker-limit (:id organization))
        is-admin (= "admin" (:role current-user))]
    (str "<div class='tariff-card' >"
         "<h3>Тарифный план</h3>"
         "<table class='detail-table'>"
         "<tr><td><strong>План:</strong></td><td>" (layout/html-escape (:name plan)) "</td></tr>"
         "<tr><td><strong>Работники:</strong></td><td>" (:current worker-check) " / " (:limit worker-check) "</td></tr>"
         "<tr><td><strong>Цена:</strong></td><td>" (:price-monthly plan) " ₽/мес</td></tr>"
         "</table>"
         (when is-admin
            (str "<div class='mt-10'>"
                 (str "<form method='POST' action='" (url (str "/organizations/" (:id organization) "/update-plan")) "' class='inline-form'>")
                 (layout/csrf-field)
                 "<select name='plan_id' class='form-control form-control-sm form-control-inline'>"
                (apply str
                       (for [p (tariff/get-all-plans)]
                         (str "<option value='" (:id p) "'"
                              (if (= (:id p) (:id plan)) " selected" "")
                              ">" (layout/html-escape (:name p))
                              " (" (:price-monthly p) " ₽/мес)" "</option>")))
                "</select> "
                "<button type='submit' class='btn btn-sm btn-primary'>Сменить план</button>"
                "</form></div>"))
         "</div>")))

(defn render-organization-detail
  "Страница детального просмотра организации"
  [organization user & {:keys [users]}]
  (let [lang "ru"]
    (layout/wrap-html
     (str
      "<div class='page-header'>"
      "<h2>" (layout/html-escape (:name organization)) "</h2>"
(str "<a href='" (url (str "/organizations/" (:id organization)
       "/edit")) "' class='btn btn-info'>Редактировать</a> ")
       (str "<a href='" (url "/organizations") "' class='btn btn-secondary'>Назад</a>")
      "</div>"
      (when (= (:role user) "admin")
        (str "<div class='alert alert-info'>Вы — глобальный администратор, "
             "поэтому видите все организации.</div>"))
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
      "</div>"
      (render-tariff-info organization user)
      "<hr>"
      (org-user-table-html (:id organization) users user))
     (:name organization) "organizations" lang)))

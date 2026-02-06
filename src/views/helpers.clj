(ns my-ring-app.views.helpers)

(defn option-tag [item value-key label-key selected-value]
  "Генерация опции для выпадающего списка"
  (let [item-id (str (value-key item))
        item-label (str (label-key item))
        selected (if (= item-id (str selected-value)) " selected" "")]
    (str "<option value='" item-id "'" selected ">" item-label "</option>")))

(defn render-error-messages [errors]
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

(defn breadcrumbs [& links]
  "Генерация хлебных крошек"
  (str "<div style='margin-bottom: 20px;'>"
       (apply str (interpose " | " links))
       "</div>"))
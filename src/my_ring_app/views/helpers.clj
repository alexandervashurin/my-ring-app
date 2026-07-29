(ns my-ring-app.views.helpers
  (:require [clojure.string :as str]
            [my-ring-app.views.layout :refer [html-escape]]))

(defn escape-js-string
  "Экранирует строку для безопасной вставки в JavaScript-контекст"
  [s]
  (when s
    (-> (str s)
        (str/replace "\\" "\\\\")
        (str/replace "\"" "\\\"")
        (str/replace "'" "\\'")
        (str/replace "\n" "\\n")
        (str/replace "\r" "\\r")
        (str/replace "\t" "\\t")
        (str/replace "<" "\\x3c")
        (str/replace ">" "\\x3e"))))

(defn option-tag
  "Генерация опции для выпадающего списка"
  [item value-key label-key selected-value]
  (let [item-id (str (value-key item))
        item-label (str (label-key item))
        selected (if (= item-id (str selected-value)) " selected" "")]
    (str "<option value='" (html-escape item-id) "'" selected ">" (html-escape item-label) "</option>")))

(defn render-error-messages
  "Рендер списка ошибок валидации"
  [errors]
  (when (seq errors)
    (str "<div class='alert alert-error'>"
         "<strong>⚠️ Ошибки валидации:</strong>"
         "<ul>"
          (apply str 
            (map (fn [error]
                   (str "<li>" (html-escape error) "</li>"))
                 errors))
         "</ul>"
         "</div>")))

(defn breadcrumbs
  "Генерация хлебных крошек"
  [& links]
  (str "<div class='breadcrumbs'>"
       (apply str (interpose " | " links))
       "</div>"))
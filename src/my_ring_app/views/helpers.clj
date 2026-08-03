(ns my-ring-app.views.helpers
  (:require [clojure.string :as str]
            [my-ring-app.views.layout :refer [html-escape]]))

(defn render-full-name
  "Рендер полного ФИО работника с HTML-экранированием
   (render-full-name w) => Иванов Иван Иванович"
  [worker]
  (str (html-escape (:фамилия worker))
       " " (html-escape (:имя worker))
       (when (:отчество worker)
         (str " " (html-escape (:отчество worker))))))

(defn render-short-name
  "Рендер сокращённого ФИО (без отчества)
   (render-short-name w) => Иванов Иван"
  [worker]
  (str (html-escape (:фамилия worker))
       " " (html-escape (:имя worker))))

(defn text-input
  "Генерация текстового поля формы
   (text-input 'Имя' :имя '' {:required true})"
  [label field-name value & [opts]]
  (let [required (if (:required opts) " required" "")
        input-type (or (:type opts) "text")
        extra-classes (or (:class opts) "")
        extra-attrs (apply str (for [[k v] (:attrs opts)] (str " " (clojure.core/name k) "='" (html-escape (str v)) "'")))]
    (str "<div class='form-group'>"
         "<label>" (html-escape label) (when (:required opts) " *") "</label>"
         "<input type='" input-type "' name='" (html-escape (clojure.core/name field-name)) "' value='" (html-escape (or (str value) "")) "'" required extra-attrs ">"
         "</div>")))

(defn password-input
  "Генерация поля пароля формы
   (password-input 'Пароль' :password nil {:required true})"
  [label name value & [opts]]
  (text-input label name value (merge {:type "password"} opts)))

(defn textarea-input
  "Генерация textarea для формы
   (textarea-input 'Адрес' :address '' {:required true})"
  [label field-name value & [opts]]
  (let [required (if (:required opts) " required" "")
        extra-attrs (apply str (for [[k v] (:attrs opts)] (str " " (clojure.core/name k) "='" (html-escape (str v)) "'")))]
    (str "<div class='form-group'>"
         "<label>" (html-escape label) (when (:required opts) " *") "</label>"
         "<textarea name='" (html-escape (clojure.core/name field-name)) "'" required extra-attrs ">" (html-escape (or (str value) "")) "</textarea>"
         "</div>")))

(defn option-tag
  "Генерация опции для выпадающего списка"
  [item value-key label-key selected-value]
  (let [item-id (str (value-key item))
        item-label (str (label-key item))
        selected (if (= item-id (str selected-value)) " selected" "")]
    (str "<option value='" (html-escape item-id) "'" selected ">" (html-escape item-label) "</option>")))

(defn select-input
  "Генерация выпадающего списка формы
   (select-input 'Цех' :цех_id items :id :название_цеха selected-id {:required true})"
  [label field-name items value-key label-key selected & [opts]]
  (let [required (if (:required opts) " required" "")
        placeholder (or (:placeholder opts) (str "Выберите " (str/lower-case label)))
        container-class (or (:container-class opts) "")
        container-id (when (:container-id opts) (str " id='" (html-escape (:container-id opts)) "'"))
        container-style (when (:container-style opts) (str " style='" (html-escape (:container-style opts)) "'"))
        extra-attrs (apply str (for [[k v] (:attrs opts)] (str " " (clojure.core/name k) "='" (html-escape (str v)) "'")))]
    (str "<div class='form-group" (when (not= container-class "") (str " " container-class)) "'" container-id container-style ">"
         "<label>" (html-escape label) (when (:required opts) " *") "</label>"
         "<select name='" (html-escape (clojure.core/name field-name)) "'" required extra-attrs ">"
         "<option value=''>" (html-escape placeholder) "</option>"
         (apply str (map #(option-tag % value-key label-key selected) items))
         "</select>"
         "</div>")))

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

(defn render-pagination
  "Рендер постраничной навигации. base-path — путь, extra-params — доп. query-параметры
   (например, search). Возвращает HTML или пустую строку, если страниц меньше двух."
  [page total-pages base-path extra-params]
  (if (<= total-pages 1)
    ""
    (let [params-str (apply str (for [[k v] extra-params
                                      :when v]
                                  (str "&" (name k) "=" (html-escape (str v)))))
          page-link (fn [p label active?]
                      (str "<a class='page-link" (when active? " active") "'"
                           " href='" (html-escape (str base-path "?page=" p params-str)) "'>"
                           (html-escape label) "</a>"))
          pages (filter #(or (= % 1)
                             (= % total-pages)
                             (<= (Math/abs (long (- % page))) 2))
                        (range 1 (inc total-pages)))]
      (str "<nav class='pagination'>"
           (when (> page 1)
             (page-link (dec page) "← Назад" false))
           (apply str (for [p pages]
                        (page-link p (str p) (= p page))))
           (when (< page total-pages)
             (page-link (inc page) "Вперёд →" false))
           "</nav>"))))

(defn breadcrumbs
  "Генерация хлебных крошек"
  [& links]
  (str "<div class='breadcrumbs'>"
       (apply str (interpose " | " links))
       "</div>"))
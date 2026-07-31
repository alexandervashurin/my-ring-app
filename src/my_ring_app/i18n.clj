(ns my-ring-app.i18n
  "Интернационализация (i18n) — поддержка нескольких языков"
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [my-ring-app.logger :as logger]))

;; ======================================================================
;; Загрузка переводов
;; ======================================================================

(def ^:private translations
  "Кэш переводов"
  (delay
    (try
      (edn/read-string (slurp (io/resource "i18n.edn")))
      (catch Exception e
        (logger/log-error e "Error loading i18n.edn")
        {}))))

;; ======================================================================
;; Вспомогательные функции
;; ======================================================================

(defn- get-translation
  "Получение перевода по ключу.
   Ключи в i18n.edn — строки, поэтому поиск идёт по строковым ключам."
  [lang & keys]
  (try
    (let [lang-data (get @translations (name lang) {})]
      (reduce get lang-data (map name keys)))
    (catch Exception e
      nil)))

(defn- get-default-translation
  "Получение перевода по умолчанию (русский)"
  [& keys]
  (apply get-translation "ru" keys))

;; ======================================================================
;; Публичные функции
;; ======================================================================

(defn t
  "Перевод строки
   (t :ru :workers :title) => \"Работники\"
   (t :en :workers :title) => \"Workers\""
  ([lang key]
   (or (get-translation (name lang) (name key))
       (get-default-translation (name key))
       (name key)))
  ([lang key & keys]
   (or (apply get-translation (name lang) (name key) (map name keys))
       (apply get-default-translation (name key) (map name keys))
       (name key))))

(defn t-format
  "Перевод с форматированием
   (t-format :ru :workers :search_results 5) => \"Результаты поиска: найдено 5 работников\""
  [lang key & args]
  (let [template (t lang key)]
    (if (and template (seq args))
      (apply format template args)
      template)))

(defn get-available-languages
  "Получение списка доступных языков"
  []
  (keys @translations))

(defn get-language-name
  "Получение названия языка"
  [lang]
  (case (name lang)
    "ru" "Русский"
    "en" "English"
    (name lang)))

(defn get-current-lang
  "Получение текущего языка из запроса"
  [request]
  (or (get-in request [:session :lang]) "ru"))

;; ======================================================================
;; Словари для часто используемых переводов
;; ======================================================================

(defn translate-field
  "Перевод названия поля"
  [lang field]
  (t lang :workers :fields field))

(defn translate-nav
  "Перевод навигации"
  [lang item]
  (t lang :navigation item))

(defn translate-error
  "Перевод ошибки"
  [lang error]
  (t lang :errors error))

(defn translate-message
  "Перевод сообщения"
  [lang msg]
  (t lang :messages msg))

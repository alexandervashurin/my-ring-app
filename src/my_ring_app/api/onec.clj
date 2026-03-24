(ns my-ring-app.api.onec
  "REST API для интеграции с 1С:ЗУП"
  (:require [compojure.core :refer [defroutes GET POST]]
            [ring.util.response :as resp]
            [clojure.data.xml :as xml]
            [clojure.string :as str]
            [my-ring-app.model :as model]
            [my-ring-app.logger :as logger]))

;; ======================================================================
;; Вспомогательные функции
;; ======================================================================

(defn- parse-int [s default]
  "Безопасное преобразование строки в число"
  (try
    (if (or (nil? s) (str/blank? s))
      default
      (Integer/parseInt (str/trim s)))
    (catch NumberFormatException e
      default)))

(defn- success-response
  "Стандартный ответ об успехе"
  ([data]
   {:success true
    :data data
    :message "Данные получены успешно"})
  ([data message]
   {:success true
    :data data
    :message message}))

(defn- error-response
  "Стандартный ответ об ошибке"
  [code message]
  {:success false
   :error {:code code
           :message message}})

(defn- format-date [date-str]
  "Форматирование даты для 1С (YYYY-MM-DD)"
  (or date-str "2000-01-01"))

;; ======================================================================
;; XML экспорт для 1С
;; ======================================================================

(defn- worker-to-xml [worker]
  "Конвертация работника в XML формат для 1С"
  [:Работник
   [:ID (:id worker)]
   [:Фамилия (or (:фамилия worker) "")]
   [:Имя (or (:имя worker) "")]
   [:Отчество (or (:отчество worker) "")]
   [:ДатаПриема (format-date (:дата_приема worker))]
   [:Цех (or (:цех worker) "")]
   [:СистемаОплаты (or (:система_оплаты worker) "")]
   [:Категория (or (:категория worker) "")]
   [:Разряд (or (:разряд worker) "")]
   [:РежимРаботы (or (:режим worker) "")]])

(defn- workers-to-xml [workers]
  "Конвертация списка работников в XML"
  (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
       (xml/emit-str
        [:ЭкспортРаботников
         [:ДатаВыгрузки (str (java.time.LocalDate/now))]
         [:ВерсияФормата "1.0"]
         (into [:Работники] (map worker-to-xml workers))])))

(defn- salary-to-xml [salary]
  "Конвертация зарплаты в XML для 1С"
  [:Начисление
   [:ID (:id salary)]
   [:РаботникID (:работник_id salary)]
   [:Год (:год salary)]
   [:Месяц (:месяц salary)]
   [:ОбщаяЗарплата (or (:общая_зарплата salary) 0)]
   [:Больничные (or (:зарплата_за_больничные_дни salary) 0)]
   [:Командировочные (or (:зарплата_за_командировочные_дни salary) 0)]])

(defn- salary-to-xml-doc [salary-data]
  "Конвертация списка зарплаты в XML документ"
  (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
       (xml/emit-str
        [:ЭкспортЗарплаты
         [:ДатаВыгрузки (str (java.time.LocalDate/now))]
         [:ВерсияФормата "1.0"]
         (into [:Начисления] (map salary-to-xml salary-data))])))

;; ======================================================================
;; JSON экспорт для 1С (современный формат)
;; ======================================================================

(defn- format-worker-json [worker]
  "Форматирование работника для JSON экспорта"
  {:id (:id worker)
   :surname (:фамилия worker)
   :name (:имя worker)
   :patronymic (:отчество worker)
   :hireDate (:дата_приема worker)
   :shop (:цех worker)
   :paymentSystem (:система_оплаты worker)
   :category (:категория worker)
   :rank (:разряд worker)
   :workMode (:режим worker)})

(defn- format-salary-json [salary]
  "Форматирование зарплаты для JSON экспорта"
  {:id (:id salary)
   :workerId (:работник_id salary)
   :year (:год salary)
   :month (:месяц salary)
   :totalSalary (:общая_зарплата salary)
   :sickPay (:зарплата_за_больничные_дни salary)
   :businessTripPay (:зарплата_за_командировочные_дни salary)})

;; ======================================================================
;; API endpoints
;; ======================================================================

(defn get-workers-export
  "GET /api/1c/workers — экспорт работников для 1С"
  [request]
  (try
    (let [query-params (:params request)
          format (:format query-params "json")
          workers (model/get-workers-with-details)
          formatted (map format-worker-json workers)]
      (logger/log-info (format "API: GET /api/1c/workers (формат: %s, записей: %d)" format (count workers)))
      (if (= format "xml")
        (-> (resp/response (workers-to-xml workers))
            (resp/header "Content-Type" "application/xml; charset=utf-8")
            (resp/header "Content-Disposition" "attachment; filename=\"workers_export.xml\""))
        (-> (resp/response (success-response {:workers formatted :count (count workers)}))
            (resp/content-type "application/json; charset=utf-8"))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при экспорте работников в 1С")
      (-> (resp/response (error-response "EXPORT_ERROR" "Ошибка при экспорте данных"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn get-salary-export
  "GET /api/1c/salary — экспорт зарплаты для 1С"
  [request]
  (try
    (let [query-params (:params request)
          format (:format query-params "json")
          year (parse-int (:year query-params) 2025)
          month (parse-int (:month query-params) 10)
          workers (model/get-workers-with-details)
          salary-data (map #(merge % (model/get-worker-salary (:id %) year month)) workers)
          formatted (map format-salary-json salary-data)]
      (logger/log-info (format "API: GET /api/1c/salary (формат: %s, период: %d-%d)" format year month))
      (if (= format "xml")
        (-> (resp/response (salary-to-xml-doc salary-data))
            (resp/header "Content-Type" "application/xml; charset=utf-8")
            (resp/header "Content-Disposition" "attachment; filename=\"salary_export.xml\""))
        (-> (resp/response (success-response {:salary formatted :period (str year "-" month) :count (count salary-data)}))
            (resp/content-type "application/json; charset=utf-8"))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при экспорте зарплаты в 1С")
      (-> (resp/response (error-response "EXPORT_ERROR" "Ошибка при экспорте данных"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn import-workers-from-1c
  "POST /api/1c/workers/import — импорт работников из 1С"
  [request]
  (try
    (let [body (:params request)
          workers (:workers body)]
      (if (not (sequential? workers))
        (-> (resp/response (error-response "INVALID_FORMAT" "Ожидаются данные в формате {\"workers\": [...]}"))
            (resp/status 400)
            (resp/content-type "application/json; charset=utf-8"))
        (do
          (logger/log-info (format "API: POST /api/1c/workers/import (записей: %d)" (count workers)))
          (-> (resp/response (success-response {:imported (count workers) :message "Данные готовы к импорту"}))
              (resp/content-type "application/json; charset=utf-8")))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при импорте работников из 1С")
      (-> (resp/response (error-response "IMPORT_ERROR" "Ошибка при импорте данных"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn get-1c-documentation
  "GET /api/1c/docs — документация API для 1С разработчиков"
  [request]
  (try
    (logger/log-info "API: GET /api/1c/docs")
    (-> (resp/response
         {:success true
          :documentation
          {:title "API для интеграции с 1С:ЗУП"
           :version "1.0"
           :endpoints
           [{:method "GET"
             :path "/api/1c/workers"
             :description "Экспорт списка работников"
             :params {:format "json|xml (по умолчанию json)"}}
            {:method "GET"
             :path "/api/1c/salary"
             :description "Экспорт данных по зарплате"
             :params {:format "json|xml"
                      :year "год (по умолчанию 2025)"
                      :month "месяц (по умолчанию 10)"}}
            {:method "POST"
             :path "/api/1c/workers/import"
             :description "Импорт работников из 1С"
             :body {:workers [{:id "number"
                               :surname "string"
                               :name "string"
                               :patronymic "string"
                               :hireDate "YYYY-MM-DD"
                               :shop "string"
                               :paymentSystem "string"
                               :category "string"
                               :rank "number"
                               :workMode "string"}]}}]
           :examples
           {:workers-export
            "GET /api/1c/workers?format=json"
            :salary-export
            "GET /api/1c/salary?format=xml&year=2025&month=10"}}})
        (resp/content-type "application/json; charset=utf-8")))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении документации 1С")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

;; ======================================================================
;; Маршруты API
;; ======================================================================

(defroutes api-routes
  (GET "/api/1c/workers" [] get-workers-export)
  (GET "/api/1c/salary" [] get-salary-export)
  (POST "/api/1c/workers/import" [] import-workers-from-1c)
  (GET "/api/1c/docs" [] get-1c-documentation))

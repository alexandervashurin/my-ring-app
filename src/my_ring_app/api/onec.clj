(ns my-ring-app.api.onec
  "REST API для интеграции с 1С:ЗУП"
  (:require [clojure.data.xml :as xml]
            [clojure.string :as str]
            [my-ring-app.model :as model]
            [my-ring-app.logger :as logger]
            [my-ring-app.util :as util]))

(def ^:private parse-int util/parse-int)

(defn- resolve-export-org-id
  "Определяет organization_id для экспорта в 1С:
   - админ может передать ?org_id= (без параметра — все организации),
   - остальные пользователи всегда видят только свою организацию."
  [request]
  (let [identity (:identity request)
        is-admin? (= "admin" (:role identity))
        org-id-param (util/parse-int (get-in request [:params :org_id]) nil)]
    (cond
      (and is-admin? org-id-param) org-id-param
      (not is-admin?) (or (:org-id request) (get-in identity [:organization_id]))
      :else nil)))

(defn- format-date
  "Форматирование даты для 1С (YYYY-MM-DD)"
  [date-str]
  (or date-str "2000-01-01"))

(defn- worker-to-xml
  "Конвертация работника в XML формат для 1С"
  [worker]
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

(defn- workers-to-xml
  "Конвертация списка работников в XML"
  [workers]
  (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
       (xml/emit-str
        [:ЭкспортРаботников
         [:ДатаВыгрузки (str (java.time.LocalDate/now))]
         [:ВерсияФормата "1.0"]
         (into [:Работники] (map worker-to-xml workers))])))

(defn- salary-to-xml
  "Конвертация зарплаты в XML для 1С"
  [salary]
  [:Начисление
   [:ID (:id salary)]
   [:РаботникID (:id salary)]
   [:Год (:год salary)]
   [:Месяц (:месяц salary)]
   [:ОбщаяЗарплата (or (:общая_зарплата salary) 0)]
   [:Больничные (or (:зарплата_за_больничные_дни salary) 0)]
   [:Командировочные (or (:зарплата_за_командировочные_дни salary) 0)]])

(defn- salary-to-xml-doc
  "Конвертация списка зарплаты в XML документ"
  [salary-data]
  (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
       (xml/emit-str
        [:ЭкспортЗарплаты
         [:ДатаВыгрузки (str (java.time.LocalDate/now))]
         [:ВерсияФормата "1.0"]
         (into [:Начисления] (map salary-to-xml salary-data))])))

(defn- format-worker-json
  "Форматирование работника для JSON экспорта"
  [worker]
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

(defn- format-salary-json
  "Форматирование зарплаты для JSON экспорта"
  [salary]
  {:id (:id salary)
   :workerId (:id salary)
   :year (:год salary)
   :month (:месяц salary)
   :totalSalary (:общая_зарплата salary)
   :sickPay (:зарплата_за_больничные_дни salary)
   :businessTripPay (:зарплата_за_командировочные_дни salary)})

(defn get-workers-export
  "GET /api/1c/workers — экспорт работников для 1С"
  [request]
  (try
    (let [query-params (:params request)
          export-format (:format query-params "json")
          org-id (resolve-export-org-id request)
          workers (model/get-workers-with-details org-id)
          formatted (map format-worker-json workers)]
      (logger/log-info (format "API: GET /api/1c/workers (формат: %s, org: %s, записей: %d)" export-format (str org-id) (count workers)))
      (if (= export-format "xml")
        (util/file-download (workers-to-xml workers) "application/xml; charset=utf-8" "workers_export.xml")
        (util/json-ok {:workers formatted :count (count workers)})))
    (catch Exception e
      (logger/log-error e "API: Ошибка при экспорте работников в 1С")
      (util/json-error 500 "EXPORT_ERROR" "Ошибка при экспорте данных"))))

(defn get-salary-export
  "GET /api/1c/salary — экспорт зарплаты для 1С"
  [request]
  (try
    (let [query-params (:params request)
          export-format (:format query-params "json")
          org-id (resolve-export-org-id request)
          [current-year current-month] (model/current-year-month)
          year (parse-int (:year query-params) current-year)
          month (parse-int (:month query-params) current-month)
          workers (model/get-workers-with-details org-id)
          salary-data (map #(merge % (model/get-worker-salary (:id %) year month org-id)) workers)
          formatted (map format-salary-json salary-data)]
      (logger/log-info (format "API: GET /api/1c/salary (формат: %s, период: %d-%d, org: %s)" export-format year month (str org-id)))
      (if (= export-format "xml")
        (util/file-download (salary-to-xml-doc salary-data) "application/xml; charset=utf-8" "salary_export.xml")
        (util/json-ok {:salary formatted :period (str year "-" month) :count (count salary-data)})))
    (catch Exception e
      (logger/log-error e "API: Ошибка при экспорте зарплаты в 1С")
      (util/json-error 500 "EXPORT_ERROR" "Ошибка при экспорте данных"))))

(defn import-workers-from-1c
  "POST /api/1c/workers/import — импорт работников из 1С"
  [request]
  (try
    (let [body (:params request)
          workers (:workers body)]
      (if (not (sequential? workers))
        (util/json-error 400 "INVALID_FORMAT" "Ожидаются данные в формате {\"workers\": [...]}")
        (do
          (logger/log-info (format "API: POST /api/1c/workers/import (записей: %d)" (count workers)))
          (util/json-ok {:imported (count workers) :message "Данные готовы к импорту"}))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при импорте работников из 1С")
      (util/json-error 500 "IMPORT_ERROR" "Ошибка при импорте данных"))))

(defn get-1c-documentation
  "GET /api/1c/docs — документация API для 1С разработчиков"
  [request]
  (try
    (logger/log-info "API: GET /api/1c/docs")
    (util/json-response
     {:success true
      :documentation
      {:title "API для интеграции с 1С:ЗУП"
       :version "1.0"
       :endpoints
       [{:method "GET"
         :path "/api/1c/workers"
         :description "Экспорт списка работников (организация из ?org_id=, по умолчанию — все для админа)"
         :params {:format "json|xml (по умолчанию json)"
                  :org_id "id организации (только для админа)"}}
        {:method "GET"
         :path "/api/1c/salary"
         :description "Экспорт данных по зарплате (организация из ?org_id=, по умолчанию — все для админа)"
         :params {:format "json|xml"
                  :year "год (по умолчанию текущий)"
                  :month "месяц (по умолчанию текущий)"
                  :org_id "id организации (только для админа)"}}
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
        "GET /api/1c/workers?format=json&org_id=1"
        :salary-export
        "GET /api/1c/salary?format=xml&year=2025&month=10&org_id=1"}}})
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении документации 1С")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

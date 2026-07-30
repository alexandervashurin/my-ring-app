(ns my-ring-app.controllers.references
  (:require [ring.util.response :as resp]
            [clojure.string :as str]
            [my-ring-app.model :as model]
            [my-ring-app.views.references :as ref-views]
            [my-ring-app.logger :as logger]
            [my-ring-app.util :as util]
            [my-ring-app.config :refer [url]]))

(def ^:private parse-int util/parse-int)
(def ^:private validate-id util/validate-id)

(def ^:private ref-config
  {:shops   {:table "Цех"               :title "Цеха"           :page "shops"
             :fields [["Название цеха" :название_цеха :text :required true]]
             :parse (fn [p] {:название_цеха (str/trim (:название_цеха p))})}
   :ranks   {:table "Разряд"            :title "Разряды"         :page "ranks"
             :fields [["Номер разряда" :номер_разряда :number :required true]]
             :parse (fn [p] {:номер_разряда (parse-int (:номер_разряда p) nil)})}
   :systems {:table "Система_оплаты"    :title "Системы оплаты" :page "salary-systems"
             :fields [["Название системы" :название_системы :text :required true]]
             :parse (fn [p] {:название_системы (str/trim (:название_системы p))})}
   :categories {:table "Категория_работника" :title "Категории работников" :page "categories"
                :fields [["Название категории" :название_категории :text :required true]]
                :parse (fn [p] {:название_категории (str/trim (:название_категории p))})}
   :modes   {:table "Режим_работы"      :title "Режимы работы"  :page "work-modes"
             :fields [["Название режима" :название_режима :text :required true]]
             :parse (fn [p] {:название_режима (str/trim (:название_режима p))})}
   :salaries {:table "Оклад"            :title "Оклады"          :page "salary-grades"
              :fields [["Оклад в месяц (₽)" :оклад_в_месяц :number :required true]]
              :parse (fn [p] {:оклад_в_месяц (parse-int (:оклад_в_месяц p) nil)})}
   :rates   {:table "Почасовые_ставки"  :title "Почасовые ставки" :page "hourly-rates"
             :fields [["Ставка в час (₽)" :ставка_в_час :number :required true]]
             :parse (fn [p] {:ставка_в_час (try (Double/parseDouble (str/trim (str (:ставка_в_час p)))) (catch Exception _ nil))})}
   :tariffs {:table "Тарифный_план"     :title "Тарифные планы"  :page "tariffs"
             :fields [["Код" :code :text :required true]
                      ["Название" :name :text :required true]
                      ["Макс. работников" :max_workers :number :required true]
                      ["Макс. организаций" :max_orgs :number :required true]
                      ["Цена в месяц (₽)" :price_monthly :number :required true]
                      ["Цена в год (₽)" :price_yearly :number :required true]
                      ["Порядок сортировки" :sort_order :number]]
             :parse (fn [p] {:code (str/trim (:code p))
                             :name (str/trim (:name p))
                             :max_workers (parse-int (:max_workers p) 10)
                             :max_orgs (parse-int (:max_orgs p) 1)
                             :price_monthly (parse-int (:price_monthly p) 0)
                             :price_yearly (parse-int (:price_yearly p) 0)
                             :sort_order (parse-int (:sort_order p) 0)})}})

(defn- id-from-request
  [request]
  (some-> request :params :id validate-id))

(defn- list-page
  [key request]
  (let [ref (get ref-config key)
        rows (model/get-table-data (:table ref))
        cols (mapv (fn [[label field]] [label field]) (:fields ref))]
    (util/html-response (ref-views/render-list-page (:title ref) rows cols (:page ref)))))

(defn- new-form
  [key request]
  (let [ref (get ref-config key)
        errors (some-> request :params :errors (str/split #","))]
    (util/html-response (ref-views/render-form-page :create (:title ref) (:fields ref) {:id nil} errors (:page ref)))))

(defn- edit-form
  [key request]
  (let [ref (get ref-config key)
        id (id-from-request request)
        record (when id (model/get-record-by-id (:table ref) id))
        errors (some-> request :params :errors (str/split #","))]
    (if record
      (util/html-response (ref-views/render-form-page :edit (:title ref) (:fields ref) record errors (:page ref)))
      (-> (resp/redirect (url (str "/" (:page ref)))) (resp/status 302)))))

(defn- create
  [key request]
  (let [ref (get ref-config key)
        page (:page ref)
        data ((:parse ref) (:params request))
        result (model/create-record (:table ref) data)]
    (if (:success result)
      (do
        (logger/log-info (format "Создана запись в %s: %s" (:table ref) (:message result)))
        (-> (resp/redirect (url (str "/" page))) (resp/status 302)))
      (-> (resp/redirect (url (str "/" page "/new?errors=" (:message result)))) (resp/status 302)))))

(defn- do-update
  [key request]
  (let [ref (get ref-config key)
        page (:page ref)
        id (id-from-request request)
        data ((:parse ref) (:params request))
        result (model/update-record (:table ref) id data)]
    (if (:success result)
      (do
        (logger/log-info (format "Обновлена запись в %s: id=%s" (:table ref) id))
        (-> (resp/redirect (url (str "/" page))) (resp/status 302)))
      (-> (resp/redirect (url (str "/" page "/" id "/edit?errors=" (:message result)))) (resp/status 302)))))

(defn- delete
  [key request]
  (let [ref (get ref-config key)
        page (:page ref)
        id (id-from-request request)
        result (model/delete-record (:table ref) id)]
    (logger/log-info (format "Удалена запись из %s: id=%s" (:table ref) id))
    (-> (resp/redirect (url (str "/" page))) (resp/status 302))))

;; ======================================================================
;; Цеха
;; ======================================================================
(defn list-shops [request] (list-page :shops request))
(defn new-shop-form [request] (new-form :shops request))
(defn edit-shop-form [request] (edit-form :shops request))
(defn create-shop [request] (create :shops request))
(defn update-shop [request] (do-update :shops request))
(defn delete-shop [request] (delete :shops request))

;; ======================================================================
;; Разряды
;; ======================================================================
(defn list-ranks [request] (list-page :ranks request))
(defn new-rank-form [request] (new-form :ranks request))
(defn edit-rank-form [request] (edit-form :ranks request))
(defn create-rank [request] (create :ranks request))
(defn update-rank [request] (do-update :ranks request))
(defn delete-rank [request] (delete :ranks request))

;; ======================================================================
;; Системы оплаты
;; ======================================================================
(defn list-systems [request] (list-page :systems request))
(defn new-system-form [request] (new-form :systems request))
(defn edit-system-form [request] (edit-form :systems request))
(defn create-system [request] (create :systems request))
(defn update-system [request] (do-update :systems request))
(defn delete-system [request] (delete :systems request))

;; ======================================================================
;; Категории работников
;; ======================================================================
(defn list-categories [request] (list-page :categories request))
(defn new-category-form [request] (new-form :categories request))
(defn edit-category-form [request] (edit-form :categories request))
(defn create-category [request] (create :categories request))
(defn update-category [request] (do-update :categories request))
(defn delete-category [request] (delete :categories request))

;; ======================================================================
;; Режимы работы
;; ======================================================================
(defn list-modes [request] (list-page :modes request))
(defn new-mode-form [request] (new-form :modes request))
(defn edit-mode-form [request] (edit-form :modes request))
(defn create-mode [request] (create :modes request))
(defn update-mode [request] (do-update :modes request))
(defn delete-mode [request] (delete :modes request))

;; ======================================================================
;; Оклады
;; ======================================================================
(defn list-salary-grades [request] (list-page :salaries request))
(defn new-salary-grade-form [request] (new-form :salaries request))
(defn edit-salary-grade-form [request] (edit-form :salaries request))
(defn create-salary-grade [request] (create :salaries request))
(defn update-salary-grade [request] (do-update :salaries request))
(defn delete-salary-grade [request] (delete :salaries request))

;; ======================================================================
;; Почасовые ставки
;; ======================================================================
(defn list-hourly-rates [request] (list-page :rates request))
(defn new-hourly-rate-form [request] (new-form :rates request))
(defn edit-hourly-rate-form [request] (edit-form :rates request))
(defn create-hourly-rate [request] (create :rates request))
(defn update-hourly-rate [request] (do-update :rates request))
(defn delete-hourly-rate [request] (delete :rates request))

;; ======================================================================
;; Тарифные планы
;; ======================================================================
(defn list-tariffs [request] (list-page :tariffs request))
(defn new-tariff-form [request] (new-form :tariffs request))
(defn edit-tariff-form [request] (edit-form :tariffs request))
(defn create-tariff [request] (create :tariffs request))
(defn update-tariff [request] (do-update :tariffs request))
(defn delete-tariff [request] (delete :tariffs request))

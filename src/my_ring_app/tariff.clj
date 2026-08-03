(ns my-ring-app.tariff
  (:require [clojure.java.jdbc :as jdbc]
            [my-ring-app.config :refer [db-spec]]
            [my-ring-app.logger :as logger]
            [my-ring-app.util :as util]
            [my-ring-app.auth :as auth]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [ring.util.response :as resp]))

(def default-plans
  [{:id 1 :code "free" :name "Бесплатный" :max-workers 10 :max-orgs 1
    :features {:export false, :reports false, :api false, :audit false
               :1c false, :email false, :analytics false, :support "community"}
    :price-monthly 0 :price-yearly 0 :sort-order 0}
   {:id 2 :code "pro" :name "Pro" :max-workers 50 :max-orgs 3
    :features {:export true, :reports true, :api false, :audit true
               :1c false, :email true, :analytics true, :support "email"}
    :price-monthly 2990 :price-yearly 29900 :sort-order 1}
   {:id 3 :code "enterprise" :name "Enterprise" :max-workers 999999 :max-orgs 999999
    :features {:export true, :reports true, :api true, :audit true
               :1c true, :email true, :analytics true, :support "dedicated"}
    :price-monthly 9990 :price-yearly 99900 :sort-order 2}])

(def plan-cache (atom nil))

(defn- parse-features
  [features-str]
  (try
    (json/read-str features-str :key-fn keyword)
    (catch Exception _
      {})))

(defn- format-features
  [features-map]
  (json/write-str features-map))

(defn load-plans!
  []
  (try
    (let [rows (jdbc/query db-spec ["SELECT * FROM \"Тарифный_план\" WHERE is_active = true ORDER BY sort_order"])]
      (reset! plan-cache
              (mapv (fn [row]
                      (assoc row
                             :features (parse-features (:features row))
                             :max-workers (int (:max_workers row))
                             :max-orgs (int (:max_orgs row))
                             :price-monthly (int (:price_monthly row))
                             :price-yearly (int (:price_yearly row))))
                    rows))
      (logger/log-info (format "Загружено %d тарифных планов" (count @plan-cache)))
      @plan-cache)
    (catch Exception e
      (logger/log-error e "Ошибка загрузки тарифных планов")
      (reset! plan-cache default-plans)
      default-plans)))

(defn get-plan
  ([code-or-id]
   (let [cache (or @plan-cache (load-plans!))]
     (if (number? code-or-id)
       (first (filter #(= (:id %) code-or-id) cache))
       (first (filter #(= (:code %) (name code-or-id)) cache))))))

(defn get-org-plan
  [org-id]
  (try
    (let [org (auth/get-organization-by-id org-id)
          plan-id (or (:plan_id org) 1)]
      (get-plan (int plan-id)))
    (catch Exception _
      (get-plan :free))))

(defn get-worker-count
  [org-id]
  (try
    (let [result (jdbc/query db-spec ["SELECT COUNT(*) as count FROM \"Работник\" WHERE organization_id = ?" org-id])]
      (:count (first result) 0))
    (catch Exception _
      0)))

(defn get-org-count
  []
  (try
    (let [result (jdbc/query db-spec ["SELECT COUNT(*) as count FROM \"Организация\" WHERE is_active = true"])]
      (:count (first result) 0))
    (catch Exception _
      0)))

(defn get-user-org-count
  [user-id]
  (try
    (let [result (jdbc/query db-spec ["SELECT COUNT(DISTINCT organization_id) as count FROM \"Пользователь\" WHERE id = ?" user-id])]
      (:count (first result) 0))
    (catch Exception _
      0)))

(defn check-worker-limit
  [org-id]
  (let [plan (get-org-plan org-id)
        current (get-worker-count org-id)
        max-workers (:max-workers plan 10)]
    (if (>= current max-workers)
      {:allowed false
       :message (format "Достигнут лимит работников (%d/%d). Обновите тарифный план для добавления новых работников." current max-workers)
       :current current
       :limit max-workers}
      {:allowed true
       :current current
       :limit max-workers})))

(defn check-org-limit
  [user-id]
  (let [user (auth/get-user-by-id user-id)
        user-org-id (:organization_id user)
        plan (get-org-plan user-org-id)
        current (get-org-count)
        max-orgs (:max-orgs plan 1)]
    (if (>= current max-orgs)
      {:allowed false
       :message (format "Достигнут лимит организаций (%d/%d). Обновите тарифный план." current max-orgs)
       :current current
       :limit max-orgs}
      {:allowed true
       :current current
       :limit max-orgs})))

(defn check-feature
  [org-id feature-key]
  (let [plan (get-org-plan org-id)
        features (:features plan {})
        allowed (get features feature-key false)]
    (if allowed
      {:allowed true}
      {:allowed false
       :message (format "Функция '%s' недоступна на тарифе '%s'. Обновите тарифный план."
                        (name feature-key) (:name plan ""))
       :required-plan (cond
                        (= feature-key :api) "Enterprise"
                        (= feature-key :1c) "Enterprise"
                        :else "Pro")})))

(defn require-feature
  [handler feature-key]
  (fn [request]
    (let [org-id (auth/get-org-id request)
          result (check-feature org-id feature-key)]
      (if (:allowed result)
        (handler request)
        (util/json-error 403 "FEATURE_NOT_AVAILABLE" (:message result))))))

(defn wrap-tariff
  [handler]
  (fn [request]
    (let [org-id (auth/get-org-id request)
          plan (when org-id (get-org-plan org-id))]
      (handler (assoc request :tariff-plan plan)))))

(defn get-all-plans
  []
  (or @plan-cache (load-plans!)))

(defn update-org-plan!
  [org-id plan-id user-id]
  (try
    (let [plan (get-plan (int plan-id))]
      (if (nil? plan)
        {:success false :message "Тарифный план не найден"}
        (do
          (jdbc/update! db-spec :Организация
                        {:plan_id (int plan-id)
                         :updated_at (util/now-timestamp)}
                        ["id = ?" org-id])
          (logger/log-audit "UPDATE" "Organization" org-id
                            (format "Изменён тарифный план организации на '%s' (пользователь: %s)"
                                    (:name plan) (or user-id "system")))
          {:success true :message (format "Тарифный план изменён на '%s'" (:name plan))})))
    (catch Exception e
      (logger/log-error e "Ошибка при изменении тарифного плана" {:org-id org-id :plan-id plan-id})
      {:success false :message "Ошибка при изменении тарифного плана"})))

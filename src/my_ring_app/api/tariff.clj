(ns my-ring-app.api.tariff
  (:require [my-ring-app.tariff :as tariff]
            [my-ring-app.auth :as auth]
            [my-ring-app.logger :as logger]
            [my-ring-app.util :as util]))

(def ^:private validate-id util/validate-id)

(defn- format-plan
  [plan]
  (when plan
    {:id (:id plan)
     :code (:code plan)
     :name (:name plan)
     :max_workers (:max-workers plan)
     :max_orgs (:max-orgs plan)
     :features (:features plan)
     :price_monthly (:price-monthly plan)
     :price_yearly (:price-yearly plan)
     :sort_order (:sort-order plan)}))

(defn get-plans
  [request]
  (try
    (let [plans (tariff/get-all-plans)]
      (logger/log-info (format "API: GET /api/tariffs (найдено: %d)" (count plans)))
      (util/json-ok (map format-plan plans)))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении тарифных планов")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

(defn get-org-plan-api
  [request]
  (try
    (let [org-id (auth/get-org-id request)]
      (if org-id
        (let [plan (tariff/get-org-plan org-id)]
          (logger/log-info (format "API: GET /api/tariffs/current (org: %d, plan: %s)" org-id (:code plan)))
          (util/json-ok (format-plan plan)))
        (util/json-error 400 "NO_ORG" "Пользователь не привязан к организации")))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении текущего тарифа")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

(defn get-org-plan-admin-api
  [request]
  (try
    (let [id (-> request :route-params :id validate-id)]
      (if (nil? id)
        (util/json-error 400 "INVALID_ID" "Некорректный идентификатор")
        (let [plan (tariff/get-org-plan id)]
          (util/json-ok (format-plan plan)))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении тарифа организации")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

(defn update-org-plan-api
  [request]
  (try
    (let [id (-> request :route-params :id validate-id)
          plan-id (util/parse-int (get-in request [:params :plan_id]) nil)
          user-id (:id (:identity request))]
      (if (or (nil? id) (nil? plan-id))
        (util/json-error 400 "INVALID_PARAMS" "Некорректные параметры")
        (let [result (tariff/update-org-plan! id plan-id user-id)]
          (if (:success result)
            (do
              (logger/log-info (format "API: PUT /api/tariffs/org/%d -> plan %d" id plan-id))
              (util/json-ok {:plan_id plan-id} (:message result)))
            (util/json-error 400 "UPDATE_ERROR" (:message result))))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при обновлении тарифа организации")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

(defn check-worker-limit-api
  [request]
  (try
    (let [org-id (or (-> request :route-params :id validate-id) (auth/get-org-id request))]
      (if org-id
        (let [result (tariff/check-worker-limit org-id)]
          (util/json-ok result))
        (util/json-error 400 "NO_ORG" "Организация не найдена")))
    (catch Exception e
      (logger/log-error e "API: Ошибка при проверке лимита работников")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

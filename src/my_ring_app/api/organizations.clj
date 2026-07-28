(ns my-ring-app.api.organizations
  "REST API для управления организациями"
  (:require [clojure.string :as str]
            [my-ring-app.auth :as auth]
            [my-ring-app.logger :as logger]
            [my-ring-app.util :as util]))

(def ^:private validate-id util/validate-id)

(defn format-organization
  "Форматирование данных организации для API"
  [org]
  (when org
    (select-keys org [:id :name :inn :phone :email :address :is_active :created_at :updated_at])))

(defn get-organizations
  "GET /api/organizations — список всех активных организаций"
  [request]
  (try
    (let [orgs (auth/get-all-organizations)]
      (logger/log-info (format "API: GET /api/organizations (найдено: %d)" (count orgs)))
      (util/json-ok (map format-organization orgs)
                    (str "Получено " (count orgs) " организаций")))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении списка организаций")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

(defn get-organization-by-id
  "GET /api/organizations/:id — получение организации по ID"
  [request]
  (try
    (let [id (-> request :route-params :id validate-id)]
      (if (nil? id)
        (util/json-error 400 "INVALID_ID" "Некорректный идентификатор")
        (let [org (auth/get-organization-by-id id)]
          (if org
            (do
              (logger/log-info (format "API: GET /api/organizations/%d" id))
              (util/json-ok (format-organization org)))
            (util/json-error 404 "NOT_FOUND" "Организация не найдена")))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении организации")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

(defn create-organization
  "POST /api/organizations — создание новой организации"
  [request]
  (try
    (let [data (:params request)
          name (:name data)]
      (if (or (nil? name) (str/blank? name))
        (util/json-error 400 "VALIDATION_ERROR" "Название организации обязательно")
        (let [result (auth/create-organization data)]
          (if (:success result)
            (do
              (logger/log-info (format "API: POST /api/organizations — создана организация ID=%s" (str (:id result))))
              (util/json-created {:id (:id result)} "Организация создана"))
            (util/json-error 500 "CREATE_ERROR" (:message result))))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при создании организации")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

(defn update-organization
  "PUT /api/organizations/:id — обновление данных организации"
  [request]
  (try
    (let [id (-> request :route-params :id validate-id)
          data (:params request)]
      (if (nil? id)
        (util/json-error 400 "INVALID_ID" "Некорректный идентификатор")
        (let [result (auth/update-organization id data)]
          (if (:success result)
            (do
              (logger/log-info (format "API: PUT /api/organizations/%d — обновлена организация" id))
              (util/json-ok (format-organization (auth/get-organization-by-id id))
                            "Организация обновлена"))
            (util/json-error 404 "UPDATE_ERROR" (:message result))))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при обновлении организации")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

(defn deactivate-organization
  "DELETE /api/organizations/:id — деактивация организации"
  [request]
  (try
    (let [id (-> request :route-params :id validate-id)]
      (if (nil? id)
        (util/json-error 400 "INVALID_ID" "Некорректный идентификатор")
        (let [result (auth/deactivate-organization id)]
          (if (:success result)
            (do
              (logger/log-info (format "API: DELETE /api/organizations/%d — деактивирована организация" id))
              (util/json-ok nil "Организация деактивирована"))
            (util/json-error 500 "DELETE_ERROR" (:message result))))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при деактивации организации")
      (util/json-error 500 "INTERNAL_ERROR" "Внутренняя ошибка сервера"))))

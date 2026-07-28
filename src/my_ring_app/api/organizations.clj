(ns my-ring-app.api.organizations
  "REST API для управления организациями"
  (:require [clojure.string :as str]
            [ring.util.response :as resp]
            [my-ring-app.auth :as auth]
            [my-ring-app.logger :as logger]
            [my-ring-app.util :as util]))

(def ^:private success-response util/success-response)
(def ^:private error-response util/error-response)
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
      (-> (resp/response (success-response (map format-organization orgs)
                                           (str "Получено " (count orgs) " организаций")))
          (resp/content-type "application/json; charset=utf-8")))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении списка организаций")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn get-organization-by-id
  "GET /api/organizations/:id — получение организации по ID"
  [request]
  (try
    (let [id (-> request :route-params :id validate-id)]
      (if (nil? id)
        (-> (resp/response (error-response "INVALID_ID" "Некорректный идентификатор"))
            (resp/status 400)
            (resp/content-type "application/json; charset=utf-8"))
        (let [org (auth/get-organization-by-id id)]
          (if org
            (do
              (logger/log-info (format "API: GET /api/organizations/%d" id))
              (-> (resp/response (success-response (format-organization org)))
                  (resp/content-type "application/json; charset=utf-8")))
            (-> (resp/response (error-response "NOT_FOUND" "Организация не найдена"))
                (resp/status 404)
                (resp/content-type "application/json; charset=utf-8"))))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при получении организации")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn create-organization
  "POST /api/organizations — создание новой организации"
  [request]
  (try
    (let [data (:params request)
          name (:name data)]
      (if (or (nil? name) (str/blank? name))
        (-> (resp/response (error-response "VALIDATION_ERROR" "Название организации обязательно"))
            (resp/status 400)
            (resp/content-type "application/json; charset=utf-8"))
        (let [result (auth/create-organization data)]
          (if (:success result)
            (do
              (logger/log-info (format "API: POST /api/organizations — создана организация ID=%s" (str (:id result))))
              (-> (resp/response (success-response {:id (:id result)} "Организация создана"))
                  (resp/status 201)
                  (resp/content-type "application/json; charset=utf-8")))
            (-> (resp/response (error-response "CREATE_ERROR" (:message result)))
                (resp/status 500)
                (resp/content-type "application/json; charset=utf-8"))))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при создании организации")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn update-organization
  "PUT /api/organizations/:id — обновление данных организации"
  [request]
  (try
    (let [id (-> request :route-params :id validate-id)
          data (:params request)]
      (if (nil? id)
        (-> (resp/response (error-response "INVALID_ID" "Некорректный идентификатор"))
            (resp/status 400)
            (resp/content-type "application/json; charset=utf-8"))
        (let [result (auth/update-organization id data)]
          (if (:success result)
            (do
              (logger/log-info (format "API: PUT /api/organizations/%d — обновлена организация" id))
              (-> (resp/response (success-response (format-organization (auth/get-organization-by-id id))
                                                   "Организация обновлена"))
                  (resp/content-type "application/json; charset=utf-8")))
            (-> (resp/response (error-response "UPDATE_ERROR" (:message result)))
                (resp/status 404)
                (resp/content-type "application/json; charset=utf-8"))))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при обновлении организации")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn deactivate-organization
  "DELETE /api/organizations/:id — деактивация организации"
  [request]
  (try
    (let [id (-> request :route-params :id validate-id)]
      (if (nil? id)
        (-> (resp/response (error-response "INVALID_ID" "Некорректный идентификатор"))
            (resp/status 400)
            (resp/content-type "application/json; charset=utf-8"))
        (let [result (auth/deactivate-organization id)]
          (if (:success result)
            (do
              (logger/log-info (format "API: DELETE /api/organizations/%d — деактивирована организация" id))
              (-> (resp/response (success-response nil "Организация деактивирована"))
                  (resp/content-type "application/json; charset=utf-8")))
            (-> (resp/response (error-response "DELETE_ERROR" (:message result)))
                (resp/status 500)
                (resp/content-type "application/json; charset=utf-8"))))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при деактивации организации")
      (-> (resp/response (error-response "INTERNAL_ERROR" "Внутренняя ошибка сервера"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(ns my-ring-app.controllers.organizations
  "Контроллеры для управления организациями"
  (:require [clojure.string :as str]
            [ring.util.response :as resp]
            [my-ring-app.auth :as auth]
            [my-ring-app.tariff :as tariff]
            [my-ring-app.views.organizations :as org-views]
            [my-ring-app.logger :as logger]
            [my-ring-app.util :as util]
            [my-ring-app.config :refer [url]]))

(def ^:private validate-id util/validate-id)

(defn organizations-page
  "GET /organizations — список организаций"
  [request]
  (logger/log-info "Открыта страница организаций")
  (let [organizations (auth/get-all-organizations)
        success (:success (:params request))
        error (:error (:params request))]
    (-> (resp/response (org-views/render-organizations-page
                        organizations (:identity request)
                        :success (case success
                                   "created" "Организация создана"
                                   "updated" "Организация обновлена"
                                   "deactivated" "Организация деактивирована"
                                   nil)
                        :error (case error
                                 "not_found" "Организация не найдена"
                                 "delete_failed" "Ошибка при деактивации организации"
                                 nil)))
        (resp/content-type "text/html; charset=utf-8"))))

(defn new-organization-form
  "GET /organizations/new — форма создания организации"
  [request]
  (logger/log-info "Открыта форма создания организации")
  (-> (resp/response (org-views/render-organization-form {} (:identity request)))
      (resp/content-type "text/html; charset=utf-8")))

(defn create-organization
  "POST /organizations/create — создание организации"
  [request]
  (let [params (:params request)
        name (:name params)]
    (logger/log-info "Попытка создания организации")
    (if (or (nil? name) (str/blank? name))
      (-> (resp/response (org-views/render-organization-form params (:identity request)
                                                             :errors ["Название организации обязательно"]))
          (resp/content-type "text/html; charset=utf-8"))
      (let [result (auth/create-organization params)]
        (if (:success result)
          (do
            (logger/log-info (format "Организация создана, ID=%s" (str (:id result))))
            (-> (resp/redirect (url "/organizations?success=created"))
                (resp/status 302)))
          (-> (resp/response (org-views/render-organization-form params (:identity request)
                                                                 :errors [(:message result)]))
              (resp/content-type "text/html; charset=utf-8")))))))

(defn edit-organization-form
  "GET /organizations/:id/edit — форма редактирования организации"
  [request]
  (let [id (validate-id (get-in request [:params :id]))]
    (if (nil? id)
      (-> (resp/redirect (url "/organizations?error=not_found"))
          (resp/status 302))
      (let [org (auth/get-organization-by-id id)]
        (if org
          (do
            (logger/log-info (format "Открыта форма редактирования организации ID=%d" id))
            (-> (resp/response (org-views/render-organization-form org (:identity request)))
                (resp/content-type "text/html; charset=utf-8")))
          (-> (resp/redirect (url "/organizations?error=not_found"))
              (resp/status 302)))))))

(defn update-organization
  "POST /organizations/:id/update — обновление организации"
  [request]
  (let [id (validate-id (get-in request [:params :id]))
        params (:params request)]
    (if (nil? id)
      (-> (resp/redirect (url "/organizations?error=not_found"))
          (resp/status 302))
      (do
        (logger/log-info (format "Попытка обновления организации ID=%d" id))
        (let [result (auth/update-organization id params)]
          (if (:success result)
            (do
              (logger/log-info (format "Организация ID=%d обновлена" id))
              (-> (resp/redirect (url "/organizations?success=updated"))
                  (resp/status 302)))
            (let [org (auth/get-organization-by-id id)]
              (-> (resp/response (org-views/render-organization-form (merge org params) (:identity request)
                                                                    :errors [(:message result)]))
                  (resp/content-type "text/html; charset=utf-8")))))))))

(defn delete-organization
  "POST /organizations/:id/delete — деактивация организации"
  [request]
  (let [id (validate-id (get-in request [:params :id]))]
    (if (nil? id)
      (-> (resp/redirect (url "/organizations?error=not_found"))
          (resp/status 302))
      (do
        (logger/log-info (format "Попытка деактивации организации ID=%d" id))
        (let [result (auth/deactivate-organization id)]
          (if (:success result)
            (do
              (logger/log-info (format "Организация ID=%d деактивирована" id))
              (-> (resp/redirect (url "/organizations?success=deactivated"))
                  (resp/status 302)))
            (-> (resp/redirect (url "/organizations?error=delete_failed"))
                (resp/status 302))))))))

(defn organization-detail
  "GET /organizations/:id — страница организации"
  [request]
  (let [id (validate-id (get-in request [:params :id]))]
    (if (nil? id)
      (-> (resp/redirect (url "/organizations?error=not_found"))
          (resp/status 302))
      (let [org (auth/get-organization-by-id id)]
        (if org
          (let [users (auth/get-org-users id)]
            (logger/log-info (format "Просмотр организации ID=%d" id))
            (-> (resp/response (org-views/render-organization-detail org (:identity request) :users users))
                (resp/content-type "text/html; charset=utf-8")))
          (-> (resp/redirect (url "/organizations?error=not_found"))
              (resp/status 302)))))))

(defn update-user-org-role
  "POST /organizations/:id/users/:user-id/role — обновление роли пользователя"
  [request]
  (let [org-id (validate-id (get-in request [:params :id]))
        user-id (validate-id (get-in request [:params :user-id]))
        org-role (get-in request [:params :org_role])]
    (if (or (nil? org-id) (nil? user-id))
      (-> (resp/redirect (url (str "/organizations/" org-id "?error=invalid")))
          (resp/status 302))
      (let [result (auth/update-user-org-role! user-id org-role)]
        (if (:success result)
          (do
            (logger/log-info (format "Роль пользователя %d в организации %d изменена на %s" user-id org-id (or org-role "default")))
            (-> (resp/redirect (url (str "/organizations/" org-id "?success=role_updated")))
                (resp/status 302)))
          (-> (resp/redirect (url (str "/organizations/" org-id "?error=role_failed")))
              (resp/status 302)))))))

(defn update-org-plan
  "POST /organizations/:id/update-plan — изменение тарифного плана организации"
  [request]
  (let [org-id (validate-id (get-in request [:params :id]))
        plan-id (util/parse-int (get-in request [:params :plan_id]) nil)
        user-id (:id (:identity request))]
    (if (or (nil? org-id) (nil? plan-id))
      (-> (resp/redirect (url (str "/organizations/" org-id "?error=invalid_params")))
          (resp/status 302))
      (let [result (tariff/update-org-plan! org-id plan-id user-id)]
        (if (:success result)
          (do
            (logger/log-info (format "Тариф организации %d изменён на план %d" org-id plan-id))
            (-> (resp/redirect (url (str "/organizations/" org-id "?success=plan_updated")))
                (resp/status 302)))
          (-> (resp/redirect (url (str "/organizations/" org-id "?error=plan_failed")))
              (resp/status 302)))))))

(ns my-ring-app.routes
  (:require [compojure.core :refer [defroutes GET POST PUT DELETE]]
            [compojure.route :as route]
            [ring.util.response :as resp]
            [my-ring-app.controllers :as controllers]
            [my-ring-app.controllers.auth :as auth-controllers]
            [my-ring-app.controllers.organizations :as org-controllers]
            [my-ring-app.controllers.references :as ref-controllers]
            [my-ring-app.api.workers :as api-workers]
            [my-ring-app.api.dashboard :as api-dashboard]
            [my-ring-app.api.salary :as api-salary]
            [my-ring-app.api.export :as api-export]
            [my-ring-app.api.audit :as api-audit]
            [my-ring-app.api.reports :as api-reports]
            [my-ring-app.api.notifications :as api-notifications]
            [my-ring-app.api.onec :as api-onec]
            [my-ring-app.api.monitoring :as api-monitoring]
            [my-ring-app.api.organizations :as api-organizations]
            [my-ring-app.api.tariff :as api-tariff]
            [my-ring-app.api.session-audit :as api-session-audit]
            [my-ring-app.sse :as sse]
            [my-ring-app.auth :as auth]))

(defn- auth-required
  "Обёртка для маршрутов, требующих аутентификации"
  [handler]
  (auth/require-authentication handler))

(defn- admin-only
  "Обёртка для маршрутов, доступных только администраторам"
  [handler]
  (-> handler auth/require-authentication (auth/require-role "admin")))

(defn- org-admin-only
  "Обёртка для маршрутов, доступных администратору организации"
  [handler]
  (-> handler auth/require-authentication (auth/require-org-role "org_admin")))

(defn- manager-or-admin
  "Обёртка для маршрутов, доступных менеджерам и администраторам"
  [handler]
  (-> handler auth/require-authentication (auth/require-role "admin" "manager")))

(defn- org-manager-or-admin
  "Обёртка для маршрутов, доступных менеджерам организации и выше"
  [handler]
  (-> handler auth/require-authentication (auth/require-org-role "org_admin" "org_manager")))

(defroutes app-routes
  ;; ======================================================================
  ;; Favicon
  ;; ======================================================================
  (GET "/favicon.ico" [] (-> (resp/resource-response "public/favicon.ico" {:root "resources"})
                              (resp/content-type "image/x-icon")))

  ;; ======================================================================
  ;; API Documentation (Swagger UI)
  ;; ======================================================================
  (GET "/api-docs" [] (-> (resp/redirect "/api-docs.html") (resp/status 302)))

  ;; ======================================================================
  ;; REST API - Мониторинг и метрики (health checks доступны без аутентификации)
  ;; ======================================================================
  (GET "/api/health" [] api-monitoring/health-check)
  (GET "/api/ready" [] api-monitoring/ready-check)
  (GET "/api/live" [] api-monitoring/live-check)
  (GET "/api/metrics" request ((auth-required api-monitoring/prometheus-metrics) request))
  (GET "/api/stats" request ((auth-required api-monitoring/app-statistics) request))
  (GET "/api/dashboard/poll" request ((auth-required sse/dashboard-poll) request))
  (GET "/api/migrations" request ((admin-only api-monitoring/migration-status) request))
  (POST "/api/cache/refresh" request ((admin-only api-monitoring/refresh-cache) request))

  ;; ======================================================================
  ;; REST API - Организации (мульти-тенантность)
  ;; ======================================================================
  (GET "/api/organizations" request ((auth-required api-organizations/get-organizations) request))
  (GET "/api/organizations/:id" request ((auth-required api-organizations/get-organization-by-id) request))
  (POST "/api/organizations" request ((admin-only api-organizations/create-organization) request))
  (PUT "/api/organizations/:id" request ((org-admin-only api-organizations/update-organization) request))
  (DELETE "/api/organizations/:id" request ((admin-only api-organizations/deactivate-organization) request))
  (GET "/api/organizations/:id/users" request ((org-admin-only api-organizations/get-org-users-api) request))
  (PUT "/api/organizations/:id/users/:user-id/role" request ((org-admin-only api-organizations/update-user-org-role-api) request))

  ;; ======================================================================
  ;; REST API - Тарифные планы
  ;; ======================================================================
  (GET "/api/tariffs" request ((auth-required api-tariff/get-plans) request))
  (GET "/api/tariffs/current" request ((auth-required api-tariff/get-org-plan-api) request))
  (GET "/api/tariffs/org/:id" request ((admin-only api-tariff/get-org-plan-admin-api) request))
  (PUT "/api/tariffs/org/:id" request ((admin-only api-tariff/update-org-plan-api) request))
  (GET "/api/tariffs/check-workers/:id" request ((admin-only api-tariff/check-worker-limit-api) request))
  (GET "/api/tariffs/check-workers" request ((auth-required api-tariff/check-worker-limit-api) request))

  ;; ======================================================================
  ;; REST API - Интеграция с 1С (только админ)
  ;; ======================================================================
  (GET "/api/1c/workers" request ((admin-only api-onec/get-workers-export) request))
  (GET "/api/1c/salary" request ((admin-only api-onec/get-salary-export) request))
  (POST "/api/1c/workers/import" request ((admin-only api-onec/import-workers-from-1c) request))
  (GET "/api/1c/docs" request ((auth-required api-onec/get-1c-documentation) request))

  ;; ======================================================================
  ;; Переключение языка (публичный маршрут)
  ;; ======================================================================
  (GET "/lang/:lang" [lang :as request] (let [valid-langs #{"ru" "en"}
                                               safe-lang (if (valid-langs lang) lang "ru")
                                               current-session (:session request {})]
                                           (-> (resp/redirect "/")
                                               (resp/status 302)
                                               (assoc :session (assoc current-session :lang safe-lang)))))

  ;; ======================================================================
  ;; REST API - Email уведомления
  ;; ======================================================================
  (GET "/api/notifications/test" request ((admin-only api-notifications/test-email-api) request))
  (POST "/api/notifications/new-worker" request ((manager-or-admin api-notifications/notify-new-worker-api) request))
  (POST "/api/notifications/birthday" request ((manager-or-admin api-notifications/notify-birthday-api) request))
  (POST "/api/notifications/anniversary" request ((manager-or-admin api-notifications/notify-anniversary-api) request))

  ;; ======================================================================
  ;; REST API - PDF отчёты
  ;; ======================================================================
  (GET "/api/reports/worker/:id/pdf" request ((auth-required api-reports/export-worker-pdf) request))
  (GET "/api/reports/workers/pdf" request ((auth-required api-reports/export-workers-list-pdf) request))
  (GET "/api/reports/salary/pdf" request ((auth-required api-reports/export-salary-report-pdf) request))

  ;; ======================================================================
  ;; REST API - Аудит (только админ)
  ;; ======================================================================
  (GET "/api/audit" request ((admin-only api-audit/get-audit-log-api) request))
  (GET "/api/audit/stats" request ((admin-only api-audit/get-audit-stats-api) request))
  (GET "/api/audit/:entity-type/:entity-id" request ((admin-only api-audit/get-audit-by-entity-api) request))
  (GET "/api/audit/user/:username" request ((admin-only api-audit/get-audit-by-user-api) request))

  ;; ======================================================================
  ;; REST API - Аудит сессий
  ;; ======================================================================
  (GET "/api/sessions" request ((auth-required api-session-audit/get-sessions-api) request))
  (GET "/api/sessions/active" request ((auth-required api-session-audit/get-active-sessions-api) request))
  (GET "/api/sessions/failed" request ((auth-required api-session-audit/get-failed-logins-api) request))
  (GET "/api/sessions/stats" request ((auth-required api-session-audit/get-session-stats-api) request))

  ;; ======================================================================
  ;; REST API - Экспорт данных
  ;; ======================================================================
  (GET "/api/export/workers.csv" request ((auth-required api-export/export-workers-csv) request))
  (GET "/api/export/salary.csv" request ((auth-required api-export/export-salary-csv) request))
  (GET "/api/export/workers.xlsx" request ((auth-required api-export/export-workers-excel) request))

  ;; ======================================================================
  ;; REST API - Дашборд и аналитика
  ;; ======================================================================
  (GET "/api/dashboard" request ((auth-required api-dashboard/get-dashboard) request))
  (GET "/api/dashboard/stats" request ((auth-required api-dashboard/get-dashboard-stats) request))
  (GET "/api/analytics/workers-by-shop" request ((auth-required api-dashboard/get-workers-by-shop) request))
  (GET "/api/analytics/workers-by-category" request ((auth-required api-dashboard/get-workers-by-category) request))
  (GET "/api/analytics/salary-distribution" request ((auth-required api-dashboard/get-salary-distribution) request))

  ;; ======================================================================
  ;; REST API - Зарплата и учёт времени
  ;; ======================================================================
  (GET "/api/salary/:worker-id" request ((auth-required api-salary/get-worker-salary) request))
  (GET "/api/work-time/:worker-id" request ((auth-required api-salary/get-worker-work-time) request))
  (PUT "/api/work-time/:id" request ((manager-or-admin api-salary/update-work-time) request))

  ;; ======================================================================
  ;; REST API - Работники
  ;; ======================================================================
  (GET "/api/workers" request ((auth-required api-workers/get-workers) request))
  (GET "/api/workers/search" request ((auth-required api-workers/search-workers-api) request))
  (GET "/api/workers/:id" request ((auth-required api-workers/get-worker-by-id) request))
  (POST "/api/workers" request ((manager-or-admin api-workers/create-worker) request))
  (PUT "/api/workers/:id" request ((manager-or-admin api-workers/update-worker) request))
  (DELETE "/api/workers/:id" request ((admin-only api-workers/delete-worker) request))

  ;; ======================================================================
  ;; Аутентификация (публичные маршруты)
  ;; ======================================================================
  (GET "/login" request (auth-controllers/login-page request))
  (POST "/login" request (auth-controllers/login-submit request))
  (POST "/logout" request (auth-controllers/logout request))
  (GET "/access-denied" request (auth-controllers/access-denied request))

  ;; ======================================================================
  ;; Аутентификация (требуют аутентификации)
  ;; ======================================================================
  (GET "/profile" request ((auth-required auth-controllers/profile-page) request))
  (POST "/change-password" request ((auth-required auth-controllers/change-password) request))
  (GET "/sessions" request ((auth-required auth-controllers/sessions-page) request))

  ;; ======================================================================
  ;; Основные страницы
  ;; ======================================================================
  ;; Главная страница (публичная)
  (GET "/" [] (controllers/home-page))

  ;; Дашборд с аналитикой
  (GET "/dashboard" request ((auth-required controllers/dashboard-page) request))

  ;; Организации (админ или org_admin)
  (GET "/organizations" request ((admin-only org-controllers/organizations-page) request))
  (GET "/organizations/new" request ((admin-only org-controllers/new-organization-form) request))
  (POST "/organizations/create" request ((admin-only org-controllers/create-organization) request))
  (GET "/organizations/:id" request ((org-admin-only org-controllers/organization-detail) request))
  (GET "/organizations/:id/edit" request ((org-admin-only org-controllers/edit-organization-form) request))
  (POST "/organizations/:id/update" request ((org-admin-only org-controllers/update-organization) request))
  (POST "/organizations/:id/delete" request ((admin-only org-controllers/delete-organization) request))
  (POST "/organizations/:id/users/:user-id/role" request ((org-admin-only org-controllers/update-user-org-role) request))
  (POST "/organizations/:id/update-plan" request ((admin-only org-controllers/update-org-plan) request))

  ;; Список работников с поиском
  (GET "/workers" request ((auth-required controllers/workers-page) request))

  ;; Форма создания работника
  (GET "/workers/new" request ((manager-or-admin controllers/new-worker-form) request))

  ;; Форма редактирования работника
  (GET "/workers/:id/edit" request ((manager-or-admin controllers/edit-worker-form) request))

  ;; Создание работника
  (POST "/workers/create" request ((manager-or-admin controllers/create-worker) request))

  ;; Обновление работника
  (POST "/workers/:id/update" request ((manager-or-admin controllers/update-worker) request))

  ;; Удаление работника
  (POST "/workers/:id/delete" request ((admin-only controllers/delete-worker) request))

  ;; Страница зарплаты работника
  (GET "/workers/:id/salary" request ((auth-required controllers/worker-salary-page) request))

  ;; Страница учета рабочего времени
  (GET "/workers/:id/work-time" request ((auth-required controllers/worker-work-time-page) request))

  ;; Форма редактирования записи учета времени
  (GET "/work-time/:id/edit" request ((manager-or-admin controllers/edit-work-time-form) request))

  ;; Обновление записи учета времени
  (POST "/work-time/:id/update" request ((manager-or-admin controllers/update-work-time) request))

  ;; Просмотр всех таблиц (только админ)
  (GET "/db" request ((admin-only controllers/all-tables-page) request))

  ;; ======================================================================
  ;; Справочники (manager-or-admin)
  ;; ======================================================================
  (GET "/shops" request ((manager-or-admin ref-controllers/list-shops) request))
  (GET "/shops/new" request ((manager-or-admin ref-controllers/new-shop-form) request))
  (POST "/shops/create" request ((manager-or-admin ref-controllers/create-shop) request))
  (GET "/shops/:id/edit" request ((manager-or-admin ref-controllers/edit-shop-form) request))
  (POST "/shops/:id/update" request ((manager-or-admin ref-controllers/update-shop) request))
  (POST "/shops/:id/delete" request ((manager-or-admin ref-controllers/delete-shop) request))

  (GET "/ranks" request ((manager-or-admin ref-controllers/list-ranks) request))
  (GET "/ranks/new" request ((manager-or-admin ref-controllers/new-rank-form) request))
  (POST "/ranks/create" request ((manager-or-admin ref-controllers/create-rank) request))
  (GET "/ranks/:id/edit" request ((manager-or-admin ref-controllers/edit-rank-form) request))
  (POST "/ranks/:id/update" request ((manager-or-admin ref-controllers/update-rank) request))
  (POST "/ranks/:id/delete" request ((manager-or-admin ref-controllers/delete-rank) request))

  (GET "/salary-systems" request ((manager-or-admin ref-controllers/list-systems) request))
  (GET "/salary-systems/new" request ((manager-or-admin ref-controllers/new-system-form) request))
  (POST "/salary-systems/create" request ((manager-or-admin ref-controllers/create-system) request))
  (GET "/salary-systems/:id/edit" request ((manager-or-admin ref-controllers/edit-system-form) request))
  (POST "/salary-systems/:id/update" request ((manager-or-admin ref-controllers/update-system) request))
  (POST "/salary-systems/:id/delete" request ((manager-or-admin ref-controllers/delete-system) request))

  (GET "/categories" request ((manager-or-admin ref-controllers/list-categories) request))
  (GET "/categories/new" request ((manager-or-admin ref-controllers/new-category-form) request))
  (POST "/categories/create" request ((manager-or-admin ref-controllers/create-category) request))
  (GET "/categories/:id/edit" request ((manager-or-admin ref-controllers/edit-category-form) request))
  (POST "/categories/:id/update" request ((manager-or-admin ref-controllers/update-category) request))
  (POST "/categories/:id/delete" request ((manager-or-admin ref-controllers/delete-category) request))

  (GET "/work-modes" request ((manager-or-admin ref-controllers/list-modes) request))
  (GET "/work-modes/new" request ((manager-or-admin ref-controllers/new-mode-form) request))
  (POST "/work-modes/create" request ((manager-or-admin ref-controllers/create-mode) request))
  (GET "/work-modes/:id/edit" request ((manager-or-admin ref-controllers/edit-mode-form) request))
  (POST "/work-modes/:id/update" request ((manager-or-admin ref-controllers/update-mode) request))
  (POST "/work-modes/:id/delete" request ((manager-or-admin ref-controllers/delete-mode) request))

  (GET "/salary-grades" request ((manager-or-admin ref-controllers/list-salary-grades) request))
  (GET "/salary-grades/new" request ((manager-or-admin ref-controllers/new-salary-grade-form) request))
  (POST "/salary-grades/create" request ((manager-or-admin ref-controllers/create-salary-grade) request))
  (GET "/salary-grades/:id/edit" request ((manager-or-admin ref-controllers/edit-salary-grade-form) request))
  (POST "/salary-grades/:id/update" request ((manager-or-admin ref-controllers/update-salary-grade) request))
  (POST "/salary-grades/:id/delete" request ((manager-or-admin ref-controllers/delete-salary-grade) request))

  (GET "/hourly-rates" request ((manager-or-admin ref-controllers/list-hourly-rates) request))
  (GET "/hourly-rates/new" request ((manager-or-admin ref-controllers/new-hourly-rate-form) request))
  (POST "/hourly-rates/create" request ((manager-or-admin ref-controllers/create-hourly-rate) request))
  (GET "/hourly-rates/:id/edit" request ((manager-or-admin ref-controllers/edit-hourly-rate-form) request))
  (POST "/hourly-rates/:id/update" request ((manager-or-admin ref-controllers/update-hourly-rate) request))
  (POST "/hourly-rates/:id/delete" request ((manager-or-admin ref-controllers/delete-hourly-rate) request))

  (GET "/tariffs" request ((manager-or-admin ref-controllers/list-tariffs) request))
  (GET "/tariffs/new" request ((manager-or-admin ref-controllers/new-tariff-form) request))
  (POST "/tariffs/create" request ((manager-or-admin ref-controllers/create-tariff) request))
  (GET "/tariffs/:id/edit" request ((manager-or-admin ref-controllers/edit-tariff-form) request))
  (POST "/tariffs/:id/update" request ((manager-or-admin ref-controllers/update-tariff) request))
  (POST "/tariffs/:id/delete" request ((manager-or-admin ref-controllers/delete-tariff) request))

  ;; Страница не найдена
  (route/not-found (fn [request] (controllers/not-found-page request))))

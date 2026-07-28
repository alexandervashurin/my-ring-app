(ns my-ring-app.routes
  (:require [compojure.core :refer [defroutes GET POST PUT DELETE]]
            [compojure.route :as route]
            [ring.util.response :as resp]
            [my-ring-app.controllers :as controllers]
            [my-ring-app.controllers.auth :as auth-controllers]
            [my-ring-app.controllers.organizations :as org-controllers]
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

(defn- manager-or-admin
  "Обёртка для маршрутов, доступных менеджерам и администраторам"
  [handler]
  (-> handler auth/require-authentication (auth/require-role "admin" "manager")))

(defroutes app-routes
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
  (PUT "/api/organizations/:id" request ((admin-only api-organizations/update-organization) request))
  (DELETE "/api/organizations/:id" request ((admin-only api-organizations/deactivate-organization) request))

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

  ;; ======================================================================
  ;; Основные страницы
  ;; ======================================================================
  ;; Главная страница (публичная)
  (GET "/" [] (controllers/home-page))

  ;; Дашборд с аналитикой
  (GET "/dashboard" request ((auth-required controllers/dashboard-page) request))

  ;; Организации (только админ)
  (GET "/organizations" request ((admin-only org-controllers/organizations-page) request))
  (GET "/organizations/new" request ((admin-only org-controllers/new-organization-form) request))
  (POST "/organizations/create" request ((admin-only org-controllers/create-organization) request))
  (GET "/organizations/:id" request ((admin-only org-controllers/organization-detail) request))
  (GET "/organizations/:id/edit" request ((admin-only org-controllers/edit-organization-form) request))
  (POST "/organizations/:id/update" request ((admin-only org-controllers/update-organization) request))
  (POST "/organizations/:id/delete" request ((admin-only org-controllers/delete-organization) request))

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

  ;; Страница не найдена
  (route/not-found (fn [request] (controllers/not-found-page request))))

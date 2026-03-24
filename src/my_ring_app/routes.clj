(ns my-ring-app.routes
  (:require [compojure.core :refer [defroutes GET POST PUT DELETE]]
            [compojure.route :as route]
            [ring.util.response :as resp]
            [my-ring-app.controllers :as controllers]
            [my-ring-app.controllers.auth :as auth-controllers]
            [my-ring-app.api.workers :as api-workers]
            [my-ring-app.api.dashboard :as api-dashboard]
            [my-ring-app.api.salary :as api-salary]
            [my-ring-app.api.export :as api-export]
            [my-ring-app.api.audit :as api-audit]
            [my-ring-app.api.reports :as api-reports]
            [my-ring-app.api.notifications :as api-notifications]
            [my-ring-app.api.onec :as api-onec]
            [my-ring-app.api.monitoring :as api-monitoring]))

(defroutes app-routes
  ;; ======================================================================
  ;; REST API - Мониторинг и метрики
  ;; ======================================================================
  (GET "/api/health" [] api-monitoring/health-check)
  (GET "/api/ready" [] api-monitoring/ready-check)
  (GET "/api/live" [] api-monitoring/live-check)
  (GET "/api/metrics" [] api-monitoring/prometheus-metrics)
  (GET "/api/stats" [] api-monitoring/app-statistics)

  ;; ======================================================================
  ;; REST API - Интеграция с 1С
  ;; ======================================================================
  (GET "/api/1c/workers" [] api-onec/get-workers-export)
  (GET "/api/1c/salary" [] api-onec/get-salary-export)
  (POST "/api/1c/workers/import" [] api-onec/import-workers-from-1c)
  (GET "/api/1c/docs" [] api-onec/get-1c-documentation)

  ;; ======================================================================
  ;; Переключение языка
  ;; ======================================================================
  (GET "/lang/:lang" [lang] (-> (resp/redirect "/")
                                 (resp/status 302)
                                 (assoc :session {:lang lang})))

  ;; ======================================================================
  ;; REST API - Email уведомления
  ;; ======================================================================
  (GET "/api/notifications/test" [] api-notifications/test-email-api)
  (POST "/api/notifications/new-worker" [] api-notifications/notify-new-worker-api)
  (POST "/api/notifications/birthday" [] api-notifications/notify-birthday-api)
  (POST "/api/notifications/anniversary" [] api-notifications/notify-anniversary-api)

  ;; ======================================================================
  ;; REST API - PDF отчёты
  ;; ======================================================================
  (GET "/api/reports/worker/:id/pdf" [] api-reports/export-worker-pdf)
  (GET "/api/reports/workers/pdf" [] api-reports/export-workers-list-pdf)
  (GET "/api/reports/salary/pdf" [] api-reports/export-salary-report-pdf)

  ;; ======================================================================
  ;; REST API - Аудит
  ;; ======================================================================
  (GET "/api/audit" [] api-audit/get-audit-log-api)
  (GET "/api/audit/stats" [] api-audit/get-audit-stats-api)
  (GET "/api/audit/:entity-type/:entity-id" [] api-audit/get-audit-by-entity-api)
  (GET "/api/audit/user/:username" [] api-audit/get-audit-by-user-api)

  ;; ======================================================================
  ;; REST API - Экспорт данных
  ;; ======================================================================
  (GET "/api/export/workers.csv" [] api-export/export-workers-csv)
  (GET "/api/export/salary.csv" [] api-export/export-salary-csv)
  (GET "/api/export/workers.xlsx" [] api-export/export-workers-excel)

  ;; ======================================================================
  ;; REST API - Дашборд и аналитика
  ;; ======================================================================
  (GET "/api/dashboard" [] api-dashboard/get-dashboard)
  (GET "/api/dashboard/stats" [] api-dashboard/get-dashboard-stats)
  (GET "/api/analytics/workers-by-shop" [] api-dashboard/get-workers-by-shop)
  (GET "/api/analytics/workers-by-category" [] api-dashboard/get-workers-by-category)
  (GET "/api/analytics/salary-distribution" [] api-dashboard/get-salary-distribution)

  ;; ======================================================================
  ;; REST API - Зарплата и учёт времени
  ;; ======================================================================
  (GET "/api/salary/:worker-id" [] api-salary/get-worker-salary)
  (GET "/api/work-time/:worker-id" [] api-salary/get-worker-work-time)
  (PUT "/api/work-time/:id" [] api-salary/update-work-time)

  ;; ======================================================================
  ;; REST API - Работники
  ;; ======================================================================
  (GET "/api/workers" [] api-workers/get-workers)
  (GET "/api/workers/search" [] api-workers/search-workers-api)
  (GET "/api/workers/:id" [] api-workers/get-worker-by-id)
  (POST "/api/workers" [] api-workers/create-worker)
  (PUT "/api/workers/:id" [] api-workers/update-worker)
  (DELETE "/api/workers/:id" [] api-workers/delete-worker)

  ;; ======================================================================
  ;; Аутентификация
  ;; ======================================================================
  (GET "/login" request (auth-controllers/login-page request))
  (POST "/login" request (auth-controllers/login-submit request))
  (GET "/logout" request (auth-controllers/logout request))
  (GET "/profile" request (auth-controllers/profile-page request))
  (POST "/change-password" request (auth-controllers/change-password request))
  (GET "/access-denied" request (auth-controllers/access-denied request))

  ;; ======================================================================
  ;; Основные страницы
  ;; ======================================================================
  ;; Главная страница
  (GET "/" [] (controllers/home-page))

  ;; Дашборд с аналитикой
  (GET "/dashboard" [] (controllers/dashboard-page))

  ;; Список работников с поиском
  (GET "/workers" request (controllers/workers-page (:params request)))

  ;; Форма создания работника
  (GET "/workers/new" request (controllers/new-worker-form (:params request)))

  ;; Форма редактирования работника
  (GET "/workers/:id/edit" [id :as request] (controllers/edit-worker-form id (:params request)))

  ;; Создание работника
  (POST "/workers/create" request (controllers/create-worker (:params request)))

  ;; Обновление работника
  (POST "/workers/:id/update" [id :as request] (controllers/update-worker id (:params request)))

  ;; Удаление работника
  (POST "/workers/:id/delete" [id] (controllers/delete-worker id))

  ;; Страница зарплаты работника
  (GET "/workers/:id/salary" [id] (controllers/worker-salary-page id))

  ;; Страница учета рабочего времени
  (GET "/workers/:id/work-time" [id] (controllers/worker-work-time-page id))

  ;; Форма редактирования записи учета времени
  (GET "/work-time/:id/edit" [id] (controllers/edit-work-time-form id))

  ;; Обновление записи учета времени
  (POST "/work-time/:id/update" [id :as request] (controllers/update-work-time id (:params request)))

  ;; Просмотр всех таблиц
  (GET "/db" [] (controllers/all-tables-page))

  ;; Страница не найдена
  (route/not-found (fn [request] (controllers/not-found-page request))))
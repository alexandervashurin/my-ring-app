(ns my-ring-app.routes
  (:require [compojure.core :refer [defroutes GET POST DELETE]]
            [compojure.route :as route]
            [my-ring-app.controllers :as controllers]))

(defroutes app-routes
  ;; Главная страница
  (GET "/" [] (controllers/home-page))
  
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
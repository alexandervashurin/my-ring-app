(ns my-ring-app.core
  (:gen-class)
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.file-info :refer [wrap-file-info]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [my-ring-app.routes :refer [app-routes]]
            [my-ring-app.logger :as logger]
            [my-ring-app.config :as config]))

;; Middleware для обработки ошибок
(defn wrap-error-handler
  "Перехватывает необработанные исключения и возвращает 500"
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (logger/log-error ^Throwable e "Необработанная ошибка при обработке запроса")
        {:status 500
         :body "Внутренняя ошибка сервера"
         :headers {"Content-Type" "text/html; charset=utf-8"}}))))

;; Middleware для добавления заголовков безопасности
(defn wrap-security-headers
  "Добавляет заголовки безопасности"
  [handler]
  (fn [request]
    (let [response (handler request)]
      (-> response
          (assoc-in [:headers "X-Frame-Options"] "DENY")
          (assoc-in [:headers "X-Content-Type-Options"] "nosniff")
          (assoc-in [:headers "X-XSS-Protection"] "1; mode=block")
          (assoc-in [:headers "Content-Security-Policy"] "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'")))))

;; Middleware для обработки параметров
(def app
  (-> app-routes
      wrap-security-headers
      wrap-error-handler
      wrap-content-type
      wrap-file-info
      wrap-keyword-params
      wrap-params
      (wrap-json-body {:keywords? true})
      wrap-json-response))

(defn -main
  "Точка входа приложения"
  [& args]
  (logger/log-info "========================================")
  (logger/log-info "Запуск приложения 'Система управления персоналом'")
  (logger/log-info "========================================")

  (let [port (:port config/app-config)]
    (logger/log-info (format "Сервер запускается на порту %d" port))
    (jetty/run-jetty app {:port port})
    (logger/log-info "Сервер остановлен")))
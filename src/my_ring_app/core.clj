(ns my-ring-app.core
  (:gen-class)
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.file-info :refer [wrap-file-info]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :as cookie]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [my-ring-app.routes :refer [app-routes]]
            [my-ring-app.logger :as logger]
            [my-ring-app.config :as config]
            [my-ring-app.auth :as auth]))

;; ======================================================================
;; Middleware
;; ======================================================================

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

(defn wrap-security-headers
  "Добавляет заголовки безопасности"
  [handler]
  (fn [request]
    (let [response (handler request)]
      (-> response
          (assoc-in [:headers "X-Frame-Options"] "DENY")
          (assoc-in [:headers "X-Content-Type-Options"] "nosniff")
          (assoc-in [:headers "X-XSS-Protection"] "1; mode=block")
          (assoc-in [:headers "Strict-Transport-Security"] "max-age=31536000; includeSubDomains")
          (assoc-in [:headers "Referrer-Policy"] "strict-origin-when-cross-origin")
          (assoc-in [:headers "Permissions-Policy"] "camera=(), microphone=(), geolocation=()")
          (assoc-in [:headers "Content-Security-Policy"]
                    "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'")))))

(defn wrap-csrf-error
  "Обработка ошибок CSRF"
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch clojure.lang.ExceptionInfo e
        (if (= (:type (ex-data e)) :ring.middleware.anti-forgery/invalid-token)
          (do
            (logger/log-warn "CSRF token validation failed")
            {:status 403
             :body "CSRF проверка не пройдена. Пожалуйста, обновите страницу."
             :headers {"Content-Type" "text/html; charset=utf-8"}})
          (throw e))))))

(defn wrap-logging
  "Middleware для логирования запросов"
  [handler]
  (fn [request]
    (let [method (:request-method request)
          uri (:uri request)
          response (handler request)
          status (:status response)]
      (logger/log-request request)
      (logger/log-response status uri)
      response)))

;; ======================================================================
;; Основное приложение
;; ======================================================================

(def app
  (-> app-routes
      (wrap-session {:cookie-attrs {:http-only true
                                     :secure true
                                     :same-site :lax
                                     :path "/"}
                     :cookie-name "session-id"
                     :store (cookie/cookie-store {:key (or (System/getenv "SESSION_SECRET")
                                                           (do (logger/log-warn "SESSION_SECRET env var not set - using insecure default. Set SESSION_SECRET in production!")
                                                               "d3v-s3cr3t-k3y!1"))})})
      auth/wrap-authentication
      wrap-anti-forgery
      wrap-csrf-error
      wrap-security-headers
      wrap-error-handler
      wrap-logging
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

  ;; Инициализация таблицы пользователей
  (auth/init-db!)

  (let [port (:port config/app-config)]
    (logger/log-info (format "Сервер запускается на порту %d" port))
    (jetty/run-jetty app {:port port})
    (logger/log-info "Сервер остановлен")))
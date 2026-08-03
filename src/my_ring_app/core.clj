(ns my-ring-app.core
  (:gen-class)
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :as cookie]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [clojure.string :as str]
            [my-ring-app.routes :refer [app-routes]]
            [my-ring-app.logger :as logger]
            [my-ring-app.config :as config]
            [my-ring-app.auth :as auth]
            [my-ring-app.migration :as migration]
            [my-ring-app.cache :as cache]
            [my-ring-app.tariff :as tariff]
            [my-ring-app.rate-limit :as rate-limit]
            [my-ring-app.api-version :as api-version]
            [my-ring-app.views.layout :as layout]))

;; ======================================================================
;; Middleware
;; ======================================================================

(defn wrap-user-context
  "Связывает текущего аутентифицированного пользователя с шаблонами
   через динамическую переменную layout/*user*."
  [handler]
  (fn [request]
    (binding [layout/*user* (:identity request)]
      (handler request))))

(defn wrap-error-handler
  "Перехватывает необработанные исключения и возвращает 500"
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Throwable t
        (logger/log-error ^Throwable t "Необработанная ошибка при обработке запроса")
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
          (assoc-in [:headers "Cache-Control"] "no-store, no-cache, must-revalidate")
          (assoc-in [:headers "Pragma"] "no-cache")
          (assoc-in [:headers "Content-Security-Policy"]
                    "default-src 'self'; script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; img-src 'self' data:; font-src 'self'; connect-src 'self' https://cdn.jsdelivr.net")))))

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

(defn wrap-csrf-exempt
  "Пропускает CSRF проверку для POST на /login"
  [handler]
  (let [csrf (wrap-anti-forgery handler)]
    (fn [request]
      (if (and (= :post (:request-method request)) (= "/login" (:uri request)))
        (handler request)
        (csrf request)))))

(defn wrap-logging
  "Middleware для логирования запросов"
  [handler]
  (fn [request]
    (let [method (:request-method request)
          uri (:uri request)
          start-time (System/currentTimeMillis)
          response (handler request)
          duration (- (System/currentTimeMillis) start-time)
          status (:status response)]
      (logger/log-request request)
      (logger/log-response status uri)
      (logger/log-info (format "Запрос %s %s выполнен за %d мс" (str/upper-case (name method)) uri duration))
      response)))

;; ======================================================================
;; Основное приложение
;; ======================================================================

(def app
  (-> app-routes
      wrap-user-context
      auth/wrap-org-context
      auth/wrap-authentication
      (wrap-session {:cookie-attrs {:http-only true
                                     :secure (= "production" (:env config/app-config))
                                     :same-site :lax
                                     :max-age 28800
                                     :path "/"}
                     :cookie-name "session-id"
                     :store (cookie/cookie-store {:key (let [secret (System/getenv "SESSION_SECRET")]
                                                         (if secret
                                                           (.getBytes secret)
                                                           (do
                                                             (when (= "production" (:env config/app-config))
                                                               (throw (IllegalStateException. "SESSION_SECRET env var is required in production")))
                                                             (.getBytes "d3v-s3cr3t-k3y!1"))))})})
      (wrap-csrf-exempt)
      wrap-csrf-error
      wrap-security-headers
      wrap-error-handler
      (rate-limit/wrap-rate-limit)
      (api-version/wrap-api-v1-rewrite)
      (api-version/wrap-api-version)
      wrap-logging
      (wrap-resource "public")
      wrap-content-type
      wrap-keyword-params
      wrap-params
      (wrap-json-body {:keywords? true})
      wrap-json-response))

(defn load-env!
  "Загружает переменные из .env файла"
  []
  (try
    (with-open [rdr (clojure.java.io/reader ".env")]
      (doseq [line (line-seq rdr)]
        (when-let [[_ k v] (re-matches #"^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$" line)]
          (System/setProperty k v)
          (logger/log-info (str "Загружена переменная: " k)))))
    (catch java.io.FileNotFoundException _
      (logger/log-warn ".env файл не найден"))
    (catch Exception e
      (logger/log-warn (str "Ошибка загрузки .env: " (.getMessage e))))))

(defn -main
  "Точка входа приложения"
  [& args]
  (load-env!)
  (logger/log-info "========================================")
  (logger/log-info "Запуск приложения 'Система управления персоналом'")
  (logger/log-info "========================================")

  ;; Применение миграций
  (migration/run-migrations!)

  ;; Инициализация таблицы пользователей
  (auth/init-db!)

  ;; Загрузка кэша справочников
  (cache/load-all!)

  ;; Загрузка тарифных планов
  (tariff/load-plans!)

  (let [port (:port config/app-config)]
    (logger/log-info (format "Сервер запускается на порту %d" port))
    (jetty/run-jetty app {:port port})
    (logger/log-info "Сервер остановлен")))
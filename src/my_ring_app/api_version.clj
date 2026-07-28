(ns my-ring-app.api-version
  "API versioning middleware.
   Поддерживает /api/v1/ префикс с обратной совместимостью /api/.
   /api/v1/* запросы перезаписываются в /api/* перед обработкой."
  (:require [clojure.string :as str]
            [my-ring-app.logger :as logger]))

(def ^:private current-api-version "v1")

(defn wrap-api-version
  "Middleware добавляет заголовок X-API-Version ко всем ответам"
  [handler]
  (fn [request]
    (let [response (handler request)]
      (assoc-in response [:headers "X-API-Version"] current-api-version))))

(defn wrap-api-v1-rewrite
  "Middleware: /api/v1/* → /api/* (обратная совместимость)"
  [handler]
  (fn [request]
    (let [uri (:uri request)
          new-uri (if (str/starts-with? uri "/api/v1/")
                    (str "/api/" (subs uri 7))
                    uri)]
      (handler (assoc request :uri new-uri)))))

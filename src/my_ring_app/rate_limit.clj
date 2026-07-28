(ns my-ring-app.rate-limit
  "Rate limiting middleware — скользящее окно по IP-адресу.
   Защита от DDoS и абуза API."
  (:require [clojure.string :as str]
            [my-ring-app.logger :as logger]))

;; ======================================================================
;; Хранилище запросов (Atom -> {ip [timestamp ...]})
;; ======================================================================

(defonce ^:private request-store (atom {}))

(defonce ^:private last-cleanup-time (atom 0))

;; ======================================================================
;; Конфигурация
;; ======================================================================

(def ^:private default-config
  "Настройки по умолчанию: 100 запросов в окне 60 секунд"
  {:window-ms 60000
   :max-requests 100
   :api-window-ms 60000
   :api-max-requests 30
   :cleanup-interval-ms 300000})

;; ======================================================================
;; Очистка старых записей
;; ======================================================================

(defn- cleanup!
  "Удаление записей старше window-ms"
  [window-ms]
  (let [cutoff (- (System/currentTimeMillis) window-ms)]
    (swap! request-store
           (fn [store]
             (persistent!
              (reduce-kv (fn [acc ip timestamps]
                           (let [recent (filterv #(> % cutoff) timestamps)]
                             (if (seq recent)
                               (assoc! acc ip (vec recent))
                               acc)))
                         (transient {})
                         store))))))

(defn- maybe-cleanup!
  "Очистка каждые cleanup-interval-ms"
  [config]
  (let [now (System/currentTimeMillis)
        last-cleanup @last-cleanup-time]
    (when (> (- now last-cleanup) (:cleanup-interval-ms config))
      (cleanup! (:window-ms config))
      (cleanup! (:api-window-ms config))
      (reset! last-cleanup-time now))))

;; ======================================================================
;; Проверка лимита
;; ======================================================================

(defn- check-rate!
  "Проверяет, не превышен ли лимит для данного IP.
   Возвращает true если запрос разрешён, false если превышен."
  [ip window-ms max-requests]
  (let [now (System/currentTimeMillis)
        cutoff (- now window-ms)]
    (let [new-store
          (swap! request-store
                 (fn [store]
                   (let [timestamps (get store ip [])
                         recent (filterv #(> % cutoff) timestamps)]
                     (if (< (count recent) max-requests)
                       (assoc store ip (conj (vec recent) now))
                       (assoc store ip (vec recent))))))]
      (< (count (get new-store ip [])) (inc max-requests)))))

;; ======================================================================
;; Middleware
;; ======================================================================

(defn wrap-rate-limit
  "Middleware для ограничения количества запросов.
   :api-routes? — true для API маршрутов (строже), false для страниц."
  [handler & [{:keys [api-routes?]}]]
  (fn [request]
    (let [ip (or (get-in request [:headers "x-forwarded-for"])
                 (get-in request [:headers "x-real-ip"])
                 (:remote-addr request))
          is-api? (or api-routes? (str/starts-with? (:uri request) "/api/"))
          window (if is-api? (:api-window-ms default-config) (:window-ms default-config))
          max-req (if is-api? (:api-max-requests default-config) (:max-requests default-config))]
      (maybe-cleanup! default-config)
      (if (check-rate! ip window max-req)
        (let [response (handler request)
              remaining (let [cutoff (- (System/currentTimeMillis) window)]
                          (let [timestamps (get @request-store ip [])]
                            (- max-req (count (filterv #(> % cutoff) timestamps)))))]
          (assoc response
                 :headers (merge (:headers response)
                                 {"X-RateLimit-Limit" (str max-req)
                                  "X-RateLimit-Remaining" (str (max 0 remaining))
                                  "X-RateLimit-Reset" (str (+ (quot (System/currentTimeMillis) 1000)
                                                               (quot window 1000)))})))
        (do
          (logger/log-warn (format "Rate limit exceeded for IP %s (API=%s)" ip is-api?))
          {:status 429
           :headers {"Content-Type" "application/json; charset=utf-8"
                     "Retry-After" (str (quot window 1000))
                     "X-RateLimit-Limit" (str max-req)
                     "X-RateLimit-Remaining" "0"}
           :body (str "{\"error\":\"Too Many Requests\","
                      "\"message\":\"Превышен лимит запросов. Попробуйте через "
                      (quot window 1000) " секунд.\"}")})))))

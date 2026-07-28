(ns my-ring-app.cache
  "Кэширование справочников (справочные таблицы загружаются в Atoms при старте)
   и обновляются по требованию или по таймеру (~1 раз в день).
   
   Справочники:
   - Цех, Система_оплаты, Категория_работника
   - Разряд, Режим_работы, Оклад, Почасовые_ставки"
  (:require [my-ring-app.model :as model]
            [my-ring-app.logger :as logger]))

;; ======================================================================
;; Atoms
;; ======================================================================

(defonce ^:private cache (atom {}))

(defonce ^:private last-refresh (atom 0))

(def ^:private refresh-interval-ms
  "Интервал автобновления кэша — 24 часа"
  (* 24 60 60 1000))

;; ======================================================================
;; Table definitions
;; ======================================================================

(def ^:private reference-tables
  "Вектор пар [ключ-кэша имя-таблицы]"
  [[:shops "Цех"]
   [:salary-systems "Система_оплаты"]
   [:worker-categories "Категория_работника"]
   [:qualifications "Разряд"]
   [:work-modes "Режим_работы"]
   [:salary-grades "Оклад"]
   [:hourly-rates "Почасовые_ставки"]])

;; ======================================================================
;; Public API
;; ======================================================================

(defn load-all!
  "Загружает все справочники из БД в кэш. Вызывать при старте приложения."
  []
  (try
    (let [new-cache (reduce (fn [acc [k table-name]]
                              (assoc acc k (model/get-table-data table-name)))
                            {} reference-tables)]
      (reset! cache new-cache)
      (reset! last-refresh (System/currentTimeMillis))
      (logger/log-info (format "Кэш справочников загружен: %d таблиц" (count new-cache))))
    (catch Exception e
      (logger/log-error e "Ошибка загрузки кэша справочников"))))

(defn refresh!
  "Принудительное обновление кэша справочников"
  []
  (load-all!))

(defn maybe-refresh!
  "Обновить кэш, если прошло более refresh-interval-ms с последнего обновления"
  []
  (when (> (- (System/currentTimeMillis) @last-refresh) refresh-interval-ms)
    (logger/log-info "Автообновление кэша справочников (прошло >24ч)")
    (refresh!)))

(defn cache-age-ms
  "Возраст кэша в миллисекундах"
  []
  (- (System/currentTimeMillis) @last-refresh))

(defn cache-status
  "Информация о состоянии кэша"
  []
  {:tables (count @cache)
   :age-ms (cache-age-ms)
   :last-refresh @last-refresh
   :loaded-at (java.time.Instant/ofEpochMilli @last-refresh)})

;; ======================================================================
;; Cached getters — заменяют прямые вызовы model/get-table-data
;; ======================================================================

(defn get-shops [] (:shops @cache))
(defn get-salary-systems [] (:salary-systems @cache))
(defn get-worker-categories [] (:worker-categories @cache))
(defn get-qualifications [] (:qualifications @cache))
(defn get-work-modes [] (:work-modes @cache))
(defn get-salary-grades [] (:salary-grades @cache))
(defn get-hourly-rates [] (:hourly-rates @cache))

(defn get-table-cached
  "Получить данные справочника по ключу (ключ — ключ в map)"
  [key]
  (get @cache key))

(defn get-all-cached
  "Получить все кэшированные справочники"
  []
  @cache)

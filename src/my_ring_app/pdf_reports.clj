(ns my-ring-app.pdf-reports
  "Генерация PDF отчётов"
  (:require [clj-pdf.core :as pdf]
            [my-ring-app.model :as model]
            [my-ring-app.logger :as logger]
            [clojure.string :as str]
            [clojure.java-time :as time]))

;; ======================================================================
;; Вспомогательные функции
;; ======================================================================

(defn- format-currency [amount]
  "Форматирование суммы"
  (if (nil? amount)
    "0 ₽"
    (format "%,.2f ₽" (double amount))))

(defn- format-date [date-str]
  "Форматирование даты"
  (if date-str
    (str date-str)
    "-"))

(defn- safe-str [s]
  "Безопасная строка для PDF"
  (or (str s) "-"))

;; ======================================================================
;; Шаблоны отчётов
;; ======================================================================

(defn- worker-report-template [worker salary work-time]
  "Шаблон отчёта по работнику"
  [{:font [{:size 18 :style :bold} "Отчёт по работнику"]}
   {:spacer 10}
   {:line}
   {:spacer 10}
   
   ;; Основная информация
   [{:font [{:size 14 :style :bold} "Основная информация"]}]
   {:spacer 5}
   [{:table {:border [:solid :black 1]
             :width 100
             :padding 5
             :rows [["ФИО:" (str (:фамилия worker) " " (:имя worker) " " (safe-str (:отчество worker)))]
                    ["Дата приёма:" (format-date (:дата_приема worker))]
                    ["Цех:" (safe-str (:цех worker))]
                    ["Система оплаты:" (safe-str (:система worker))]
                    ["Категория:" (safe-str (:категория worker))]
                    ["Разряд:" (safe-str (:разряд worker))]
                    ["Режим работы:" (safe-str (:режим worker))]]}}]
   
   {:spacer 15}
   
   ;; Зарплата
   [{:font [{:size 14 :style :bold} "Заработная плата"]}
    {:spacer 5}
   [{:table {:border [:solid :black 1]
             :width 100
             :padding 5
             :rows [["Общая зарплата:" (format-currency (:общая_зарплата salary))]
                    ["Больничные дни:" (or (:больничные_дни work-time) 0)]
                    ["Командировочные дни:" (or (:командировочные_дни work-time) 0)]
                    ["Отработано часов:" (or (:всего_отработанных_часов work-time) 0)]]}}]
   
   {:spacer 15}
   
   ;; Подвал
   {:line}
   {:spacer 5}
   [{:font [{:size 10}] (str "Отчёт сформирован: " (time/local-date-time))}]])

(defn- workers-list-template [workers]
  "Шаблон списка работников"
  (concat
   [{:font [{:size 18 :style :bold} "Список работников"]}
    {:spacer 10}
    {:line}
    {:spacer 10}
    [{:font [{:size 10}] (str "Всего работников: " (count workers))}]
    {:spacer 10}]
   
   ;; Таблица работников
   [{:table {:border [:solid :black 1]
             :width 100
             :padding 3
             :header {:font [{:style :bold}]}
             :rows (cons ["ФИО" "Дата приёма" "Цех" "Система" "Зарплата"]
                         (for [w workers]
                           [(str (:фамилия w) " " (:имя w))
                            (format-date (:дата_приема w))
                            (safe-str (:цех w))
                            (safe-str (:система w))
                            (format-currency (:общая_зарплата w))]))}}]
   
   [{:spacer 10}
    {:line}
    {:spacer 5}
    [{:font [{:size 10}] (str "Отчёт сформирован: " (time/local-date-time))}]]))

(defn- salary-report-template [salary-data period]
  "Шаблон отчёта по зарплате"
  (let [total (reduce + (map :общая_зарплата salary-data))
        avg (if (seq salary-data) (/ total (count salary-data)) 0)]
    [{:font [{:size 18 :style :bold} "Отчёт по заработной плате"]}
     {:spacer 10}
     {:line}
     {:spacer 10}
     
     [{:font [{:size 12}] (str "Период: " period)}]
     {:spacer 10}
     
     ;; Сводка
     [{:table {:border [:solid :black 1]
               :width 100
               :padding 5
               :rows [["Всего работников:" (count salary-data)]
                      ["Фонд оплаты труда:" (format-currency total)]
                      ["Средняя зарплата:" (format-currency avg)]]}}]
     
     {:spacer 15}
     
     ;; Детализация
     [{:font [{:size 14 :style :bold} "Детализация по работникам"]}]
     {:spacer 5}
     
     [{:table {:border [:solid :black 1]
               :width 100
               :padding 3
               :header {:font [{:style :bold}]}
               :rows (cons ["ФИО" "Цех" "Зарплата" "Больничные" "Командировочные"]
                           (for [s salary-data]
                             [(str (:фамилия s) " " (:имя s))
                              (safe-str (:цех s))
                              (format-currency (:общая_зарплата s))
                              (or (:зарплата_за_больничные_дни s) 0)
                              (or (:зарплата_за_командировочные_дни s) 0)]))}}]
     
     {:spacer 10}
     {:line}
     {:spacer 5}
     [{:font [{:size 10}] (str "Отчёт сформирован: " (time/local-date-time))}]]))

;; ======================================================================
;; Публичные функции
;; ======================================================================

(defn generate-worker-pdf
  "Генерация PDF отчёта по работнику"
  [worker-id output-path]
  (try
    (let [worker (model/get-record-by-id "Работник" (str worker-id))
          salary (model/get-worker-salary worker-id 2025 10)
          work-time (first (model/get-worker-work-time worker-id))]
      (if worker
        (do
          (pdf/pdf (worker-report-template worker salary work-time) output-path)
          (logger/log-info (format "Сгенерирован PDF отчёт по работнику ID=%s" worker-id))
          {:success true :path output-path})
        {:success false :message "Работник не найден"})))
    (catch Exception e
      (logger/log-error e "Ошибка при генерации PDF отчёта работника")
      {:success false :message (str "Ошибка: " (.getMessage e))}))))

(defn generate-workers-list-pdf
  "Генерация PDF списка работников"
  [output-path]
  (try
    (let [workers (model/get-workers-with-details)
          salary-data (map #(merge % (model/get-worker-salary (:id %) 2025 10)) workers)]
      (pdf/pdf (workers-list-template salary-data) output-path)
      (logger/log-info (format "Сгенерирован PDF список работников (%d записей)" (count workers)))
      {:success true :path output-path})
    (catch Exception e
      (logger/log-error e "Ошибка при генерации PDF списка работников")
      {:success false :message (str "Ошибка: " (.getMessage e))}))))

(defn generate-salary-report-pdf
  "Генерация PDF отчёта по зарплате"
  [output-path year month]
  (try
    (let [workers (model/get-workers-with-details)
          salary-data (map #(merge % (model/get-worker-salary (:id %) year month)) workers)
          period (str month "/" year)]
      (pdf/pdf (salary-report-template salary-data period) output-path)
      (logger/log-info (format "Сгенерирован PDF отчёт по зарплате за %s" period))
      {:success true :path output-path})
    (catch Exception e
      (logger/log-error e "Ошибка при генерации PDF отчёта по зарплате")
      {:success false :message (str "Ошибка: " (.getMessage e))}))))

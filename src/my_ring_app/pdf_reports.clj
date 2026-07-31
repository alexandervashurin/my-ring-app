(ns my-ring-app.pdf-reports
  "Генерация PDF отчётов"
  (:require [clojure.string :as str]
            [clj-pdf.core :refer [pdf]]
            [clojure.java.io :as io]
            [my-ring-app.model :as model]
            [my-ring-app.logger :as logger]))

(def ^:private font-candidates
  "Известные пути к шрифтам с поддержкой кириллицы"
  ["/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
   "/usr/share/fonts/dejavu/DejaVuSans.ttf"
   "/usr/share/fonts/TTF/DejaVuSans.ttf"
   "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf"
   "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"
   "/usr/share/fonts/notosans/NotoSans-Regular.ttf"
   "/System/Library/Fonts/Supplemental/Arial.ttf"
   "/Library/Fonts/Arial.ttf"
   "/Windows/Fonts/arial.ttf"])

(defn- find-font-path
  "Путь к шрифту с кириллицей: PDF_FONT → pdf.font → известные пути"
  []
  (or (not-empty (System/getenv "PDF_FONT"))
      (not-empty (System/getProperty "pdf.font"))
      (first (filter #(.exists (io/file ^String %)) font-candidates))))

(defn- doc-meta
  "Метаданные PDF-документа с шрифтом, поддерживающим кириллицу"
  [title subject & [{:keys [landscape?]}]]
  (let [font-path (find-font-path)]
    (when-not font-path
      (logger/log-warn "PDF: не найден шрифт с кириллицей, текст может отображаться некорректно. Задайте PDF_FONT."))
    (cond-> {:title (str title)
             :author "Система управления персоналом"
             :subject (str (or subject ""))
             :size :a4}
      (true? landscape?) (assoc :orientation :landscape)
      font-path (assoc :font {:ttf-name font-path :encoding :unicode :size 9}))))

(defn- format-number
  "Форматирование числа с разделителями разрядов"
  [n]
  (->> (str (long (or n 0)))
       reverse
       (partition-all 3)
       (map (fn [chunk] (apply str chunk)))
       (interpose " ")
       (apply str)
       reverse
       (apply str)))

(def ^:private month-names
  "Названия месяцев"
  {1 "январь" 2 "февраль" 3 "март" 4 "апрель" 5 "май" 6 "июнь"
   7 "июль" 8 "август" 9 "сентябрь" 10 "октябрь" 11 "ноябрь" 12 "декабрь"})

(defn- month-name
  "Название месяца"
  [month]
  (or (get month-names (int month)) (str month)))

(defn- header-cells
  "Строка-заголовок таблицы (жирные ячейки)"
  [headers]
  [(vec (map (fn [h] [:chunk {:style :bold} (str h)]) headers))])

(defn- pdf-table
  "Формирование :pdf-table элемента. rows — последовательность строк (векторы значений)"
  [widths headers rows]
  (into [:pdf-table
         {:header (header-cells headers)
          :width-percent 100}
         (vec widths)]
        (map (fn [row] (mapv (fn [v] [:pdf-cell (str (or v ""))]) row)) rows)))

(defn- worker-info-rows
  "Общие сведения о работнике для отчёта"
  [worker]
  [["Фамилия" (:фамилия worker)]
   ["Имя" (:имя worker)]
   ["Отчество" (or (:отчество worker) "—")]
   ["Дата приёма" (str (or (:дата_приема worker) "—"))]
   ["Цех" (or (:цех worker) "—")]
   ["Система оплаты" (or (:система worker) "—")]
   ["Категория" (or (:категория worker) "—")]
   ["Разряд" (or (:разряд worker) "—")]
   ["Режим работы" (or (:режим worker) "—")]
   ["Оклад (руб/мес)" (when-let [o (:оклад_в_месяц worker)] (format-number o))]
   ["Ставка (руб/час)" (when-let [p (:ставка_в_час worker)] (format-number p))]])

(defn- salary-history-table
  "Таблица истории зарплаты работника"
  [history]
  (pdf-table [10 10 30 20 20]
             ["Год" "Месяц" "Общая зарплата" "Больничные" "Командировочные"]
             (map (fn [r]
                    [(:год r)
                     (:месяц r)
                     (format-number (:общая_зарплата r))
                     (format-number (:зарплата_за_больничные_дни r))
                     (format-number (:зарплата_за_командировочные_дни r))])
                  history)))

(defn- work-time-table
  "Таблица учёта рабочего времени работника"
  [work-time]
  (pdf-table [10 10 16 16 16 16 16]
             ["Год" "Месяц" "Часов по плану" "Часов по факту" "Дней отработано" "Больничных дней" "Командировочных дней"]
             (map (fn [w]
                    [(:год w)
                     (:месяц w)
                     (or (:всего_часов_за_месяц_по_плану w) "—")
                     (or (:всего_часов_в_месяц_по_факту w) "—")
                     (or (:количество_отработанных_дней w) "—")
                     (or (:больничные_дни w) "—")
                     (or (:командировочные_дни w) "—")])
                  work-time)))

(defn generate-worker-pdf
  "Генерация PDF отчёта по работнику. Если org-id не nil — фильтрует по организации."
  ([worker-id output-path]
   (generate-worker-pdf worker-id output-path nil))
  ([worker-id output-path org-id]
   (try
     (let [worker (model/get-worker-by-id worker-id org-id)]
       (if-not worker
         (do
           (logger/log-warn (format "PDF: работник ID=%s не найден (org: %s)" worker-id (str org-id)))
           {:success false :message "Работник не найден"})
         (let [salary-history (model/get-worker-salary-history worker-id org-id)
               work-time (model/get-worker-work-time worker-id org-id)
               fio (str/join " " (remove str/blank? [(:фамилия worker) (:имя worker) (:отчество worker)]))]
           (pdf [(doc-meta (format "Отчёт по работнику: %s" fio) fio)
                 [:paragraph {:style :bold :size 16} "Отчёт по работнику"]
                 [:paragraph {:size 11} fio]
                 [:paragraph {:style :bold :size 12} "Общие сведения"]
                 (pdf-table [35 65] ["Параметр" "Значение"] (worker-info-rows worker))
                 [:paragraph {:style :bold :size 12} "Зарплата"]
                 (if (seq salary-history)
                   (salary-history-table salary-history)
                   [:paragraph {:size 10} "Данных о зарплате нет"])
                 [:paragraph {:style :bold :size 12} "Учёт рабочего времени"]
                 (if (seq work-time)
                   (work-time-table work-time)
                   [:paragraph {:size 10} "Данных об учёте рабочего времени нет"])]
                 output-path)
           (logger/log-info (format "PDF: сформирован отчёт по работнику ID=%s (org: %s)" worker-id (str org-id)))
           {:success true :message "PDF отчёт сформирован"})))
     (catch Exception e
       (logger/log-error e (format "Ошибка при генерации PDF отчёта по работнику ID=%s" worker-id)
                         {:worker-id worker-id})
       {:success false :message "Внутренняя ошибка при генерации PDF"}))))

(defn generate-workers-list-pdf
  "Генерация PDF списка работников. Если org-id не nil — фильтрует по организации."
  ([output-path]
   (generate-workers-list-pdf output-path nil))
  ([output-path org-id]
   (try
     (let [workers (model/get-workers-with-details org-id)]
       (pdf [(doc-meta "Список работников" (format "Всего: %d" (count workers)) {:landscape? true})
             [:paragraph {:style :bold :size 16} "Список работников"]
             [:paragraph {:size 11} (format "Всего работников: %d" (count workers))]
             (if (seq workers)
               (pdf-table [6 18 13 16 14 14 18 12 7 14]
                          ["ID" "Фамилия" "Имя" "Отчество" "Дата приёма" "Цех"
                           "Система оплаты" "Категория" "Разряд" "Режим работы"]
                          (map (fn [w]
                                 [(:id w)
                                  (:фамилия w)
                                  (:имя w)
                                  (or (:отчество w) "")
                                  (:дата_приема w)
                                  (or (:цех w) "")
                                  (or (:система w) "")
                                  (or (:категория w) "")
                                  (or (:разряд w) "")
                                  (or (:режим w) "")])
                               workers))
               [:paragraph "Нет данных для отчёта"])]
             output-path)
       (logger/log-info (format "PDF: сформирован список работников (%d записей, org: %s)" (count workers) (str org-id)))
       {:success true :message "PDF отчёт сформирован"})
     (catch Exception e
       (logger/log-error e "Ошибка при генерации PDF списка работников")
       {:success false :message "Внутренняя ошибка при генерации PDF"}))))

(defn generate-salary-report-pdf
  "Генерация PDF отчёта по зарплате за указанный год и месяц.
  Если org-id не nil — фильтрует по организации."
  ([output-path year month]
   (generate-salary-report-pdf output-path year month nil))
  ([output-path year month org-id]
   (try
     (if (not (and (number? year) (number? month) (<= 1 month 12)))
       (do
         (logger/log-warn (format "PDF: некорректные параметры отчёта по зарплате: %s-%s" (str year) (str month)))
         {:success false :message "Некорректные параметры отчёта"})
       (let [records (filter (fn [r]
                               (and (= (int year) (int (:год r)))
                                    (= (int month) (int (:месяц r)))))
                             (model/get-salary-with-details org-id))
             total (reduce + (map (fn [r] (long (or (:общая_зарплата r) 0))) records))
             period (format "%s %d года" (month-name month) (int year))]
         (pdf [(doc-meta (format "Отчёт по зарплате за %s" period) period {:landscape? true})
               [:paragraph {:style :bold :size 16} "Отчёт по зарплате"]
               [:paragraph {:size 11} (str "Период: " period)]
               [:paragraph {:size 11} (format "Количество записей: %d" (count records))]
               (if (seq records)
                 (into (pdf-table [8 30 10 10 20 15 15]
                                  ["ID" "Работник" "Год" "Месяц" "Общая зарплата"
                                   "Больничные" "Командировочные"]
                                  (map (fn [r]
                                         [(:id r)
                                          (str (:фамилия r) " " (:имя r))
                                          (:год r)
                                          (:месяц r)
                                          (format-number (:общая_зарплата r))
                                          (format-number (:зарплата_за_больничные_дни r))
                                          (format-number (:зарплата_за_командировочные_дни r))])
                                       records))
                       [[[:pdf-cell [:chunk {:style :bold} "ИТОГО"]]
                         [:pdf-cell {:colspan 3 :align :right} ""]
                         [:pdf-cell [:chunk {:style :bold} (format-number total)]]
                         [:pdf-cell ""]
                         [:pdf-cell ""]]])
                 [:paragraph "Нет данных за указанный период"])]
               output-path)
         (logger/log-info (format "PDF: сформирован отчёт по зарплате за %s-%s (%d записей, org: %s)" (str year) (str month) (count records) (str org-id)))
         {:success true :message "PDF отчёт сформирован"}))
     (catch Exception e
       (logger/log-error e (format "Ошибка при генерации PDF отчёта по зарплате за %s-%s" (str year) (str month))
                         {:year year :month month})
       {:success false :message "Внутренняя ошибка при генерации PDF"}))))

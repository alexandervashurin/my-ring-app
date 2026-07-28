(ns my-ring-app.api.export
  "REST API для экспорта данных (CSV, Excel)"
  (:require [ring.util.response :as resp]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [my-ring-app.model :as model]
            [my-ring-app.logger :as logger]
            [my-ring-app.util :as util])
  (:import (java.io StringWriter ByteArrayOutputStream)
           (org.apache.poi.ss.usermodel WorkbookFactory CellStyle CreationHelper)
           (org.apache.poi.xssf.usermodel XSSFWorkbook)))

;; ======================================================================
;; Вспомогательные функции
;; ======================================================================

(def ^:private success-response util/success-response)
(def ^:private error-response util/error-response)

;; ======================================================================
;; CSV экспорт
;; ======================================================================

(defn- workers-to-csv
  "Конвертация работников в CSV формат"
  [workers]
  (let [sw (StringWriter.)
        headers ["ID" "Фамилия" "Имя" "Отчество" "Дата приема" "Цех" "Система оплаты" "Категория" "Разряд" "Режим работы"]]
    (csv/write-csv sw (cons headers
                             (map (fn [w]
                                    [(:id w)
                                     (:фамилия w)
                                     (:имя w)
                                     (or (:отчество w) "")
                                     (:дата_приема w)
                                     (or (:цех w) "")
                                     (or (:система_оплаты w) "")
                                     (or (:категория w) "")
                                     (or (:разряд w) "")
                                     (or (:режим w) "")])
                                  workers)))
    (str sw)))

(defn- salary-to-csv
  "Конвертация зарплаты в CSV формат"
  [salary-records]
  (let [sw (StringWriter.)
        headers ["ID" "Работник" "Год" "Месяц" "Общая зарплата" "Больничные" "Командировочные"]]
    (csv/write-csv sw (cons headers
                            (map (fn [s]
                                   [(:id s)
                                    (str (:фамилия s) " " (:имя s))
                                    (:год s)
                                    (:месяц s)
                                    (:общая_зарплата s)
                                    (or (:зарплата_за_больничные_дни s) 0)
                                    (or (:зарплата_за_командировочные_дни s) 0)])
                                 salary-records)))
    (str sw)))

(defn export-workers-csv
  "GET /api/export/workers.csv — экспорт работников в CSV"
  [request]
  (try
    (let [workers (model/get-workers-with-details)
          csv-content (workers-to-csv workers)
          filename (format "workers_%s.csv" (java.time.LocalDate/now))]
      (logger/log-info (format "API: GET /api/export/workers.csv — экспортировано %d работников" (count workers)))
      (-> (resp/response csv-content)
          (resp/header "Content-Type" "text/csv; charset=utf-8")
          (resp/header "Content-Disposition" (str "attachment; filename=\"" filename "\""))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при экспорте работников в CSV")
      (-> (resp/response (error-response "EXPORT_ERROR" "Ошибка при экспорте в CSV"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

(defn export-salary-csv
  "GET /api/export/salary.csv — экспорт зарплаты в CSV"
  [request]
  (try
    (let [enriched-data (model/get-salary-with-details)
          csv-content (salary-to-csv enriched-data)
          filename (format "salary_%s.csv" (java.time.LocalDate/now))]
      (logger/log-info (format "API: GET /api/export/salary.csv — экспортировано %d записей" (count enriched-data)))
      (-> (resp/response csv-content)
          (resp/header "Content-Type" "text/csv; charset=utf-8")
          (resp/header "Content-Disposition" (str "attachment; filename=\"" filename "\""))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при экспорте зарплаты в CSV")
      (-> (resp/response (error-response "EXPORT_ERROR" "Ошибка при экспорте в CSV"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

;; ======================================================================
;; Excel экспорт
;; ======================================================================

(defn- create-header-style
  "Создание стиля для заголовков"
  [wb]
  (let [style (.createCellStyle wb)
        font (.createFont wb)]
    (.setBold font true)
    (.setFontHeightInPoints font 12)
    (.setFontName font "Arial")
    (.setFont style font)
    (.setAlignment style org.apache.poi.ss.usermodel.HorizontalAlignment/CENTER)
    (.setVerticalAlignment style org.apache.poi.ss.usermodel.VerticalAlignment/CENTER)
    (.setFillForegroundColor style (.getIndex org.apache.poi.ss.usermodel.IndexedColors/LIGHT_BLUE))
    (.setFillPattern style org.apache.poi.ss.usermodel.FillPatternType/SOLID_FOREGROUND)
    style))

(defn- create-row-style
  "Создание стиля для строки (чередование цветов)"
  [wb ^long row-num]
  (let [style (.createCellStyle wb)]
    (if (even? row-num)
      (.setFillForegroundColor style (.getIndex org.apache.poi.ss.usermodel.IndexedColors/WHITE))
      (.setFillForegroundColor style (.getIndex org.apache.poi.ss.usermodel.IndexedColors/LIGHT_YELLOW)))
    (.setFillPattern style org.apache.poi.ss.usermodel.FillPatternType/SOLID_FOREGROUND)
    style))

(defn- workers-to-excel
  "Конвертация работников в Excel формат"
  [workers]
  (with-open [wb (XSSFWorkbook.)]
    (let [sheet (.createSheet wb "Работники")
          header-style (create-header-style wb)
          ;; Заголовки
          headers ["ID" "Фамилия" "Имя" "Отчество" "Дата приема" "Цех" "Система оплаты" "Категория" "Разряд" "Режим работы"]
          header-row (.createRow sheet 0)]
      ;; Создаем заголовки
      (doseq [[i h] (map-indexed vector headers)]
        (let [cell (.createCell header-row i)]
          (.setCellValue cell h)
          (.setCellStyle cell header-style)))
      
      ;; Данные
      (doseq [[row-idx w] (map-indexed vector workers)]
        (let [row (.createRow sheet (inc row-idx))
              row-style (create-row-style wb row-idx)
              values [(:id w)
                      (:фамилия w)
                      (:имя w)
                      (or (:отчество w) "")
                      (:дата_приема w)
                      (or (:цех w) "")
                      (or (:система_оплаты w) "")
                      (or (:категория w) "")
                      (or (:разряд w) "")
                      (or (:режим w) "")]]
          (doseq [[col-idx v] (map-indexed vector values)]
            (let [cell (.createCell row col-idx)]
              (cond
                (number? v) (.setCellValue cell (double v))
                :else (.setCellValue cell (str v)))
              (.setCellStyle cell row-style)))))
      
      ;; Авто-размер колонок
      (doseq [i (range (count headers))]
        (.autoSizeColumn sheet i))
      
      ;; Запись в ByteArrayOutputStream
      (let [baos (ByteArrayOutputStream.)]
        (.write wb baos)
        (.flush baos)
        (.toByteArray baos)))))

(defn export-workers-excel
  "GET /api/export/workers.xlsx — экспорт работников в Excel"
  [request]
  (try
    (let [workers (model/get-workers-with-details)
          excel-bytes (workers-to-excel workers)
          filename (format "workers_%s.xlsx" (java.time.LocalDate/now))]
      (logger/log-info (format "API: GET /api/export/workers.xlsx — экспортировано %d работников" (count workers)))
      (-> (resp/response excel-bytes)
          (resp/header "Content-Type" "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
          (resp/header "Content-Disposition" (str "attachment; filename=\"" filename "\""))))
    (catch Exception e
      (logger/log-error e "API: Ошибка при экспорте работников в Excel")
      (-> (resp/response (error-response "EXPORT_ERROR" "Ошибка при экспорте в Excel"))
          (resp/status 500)
          (resp/content-type "application/json; charset=utf-8")))))

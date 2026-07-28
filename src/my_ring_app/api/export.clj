(ns my-ring-app.api.export
  "REST API для экспорта данных (CSV, Excel)"
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [my-ring-app.model :as model]
            [my-ring-app.logger :as logger]
            [my-ring-app.util :as util])
  (:import (java.io StringWriter ByteArrayOutputStream)
           (org.apache.poi.ss.usermodel WorkbookFactory CellStyle CreationHelper)
           (org.apache.poi.xssf.usermodel XSSFWorkbook)))

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
      (util/file-download csv-content "text/csv; charset=utf-8" filename))
    (catch Exception e
      (logger/log-error e "API: Ошибка при экспорте работников в CSV")
      (util/json-error 500 "EXPORT_ERROR" "Ошибка при экспорте в CSV"))))

(defn export-salary-csv
  "GET /api/export/salary.csv — экспорт зарплаты в CSV"
  [request]
  (try
    (let [enriched-data (model/get-salary-with-details)
          csv-content (salary-to-csv enriched-data)
          filename (format "salary_%s.csv" (java.time.LocalDate/now))]
      (logger/log-info (format "API: GET /api/export/salary.csv — экспортировано %d записей" (count enriched-data)))
      (util/file-download csv-content "text/csv; charset=utf-8" filename))
    (catch Exception e
      (logger/log-error e "API: Ошибка при экспорте зарплаты в CSV")
      (util/json-error 500 "EXPORT_ERROR" "Ошибка при экспорте в CSV"))))

(defn- create-header-style
  "Создание стиля для заголовков"
  [^org.apache.poi.xssf.usermodel.XSSFWorkbook wb]
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
  [^org.apache.poi.xssf.usermodel.XSSFWorkbook wb ^long row-num]
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
          headers ["ID" "Фамилия" "Имя" "Отчество" "Дата приема" "Цех" "Система оплаты" "Категория" "Разряд" "Режим работы"]
          ^org.apache.poi.xssf.usermodel.XSSFRow header-row (.createRow sheet 0)]
      (doseq [[i h] (map-indexed vector headers)]
        (let [^org.apache.poi.xssf.usermodel.XSSFCell cell (.createCell header-row (int i))]
          (.setCellValue cell (str h))
          (.setCellStyle cell header-style)))
      
      (doseq [[row-idx w] (map-indexed vector workers)]
        (let [^org.apache.poi.xssf.usermodel.XSSFRow row (.createRow sheet (int (inc row-idx)))
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
            (let [^org.apache.poi.xssf.usermodel.XSSFCell cell (.createCell row (int col-idx))]
              (cond
                (number? v) (.setCellValue cell (double v))
                :else (.setCellValue cell (str v)))
              (.setCellStyle cell row-style)))))
      
      (doseq [i (range (count headers))]
        (.autoSizeColumn sheet i))
      
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
      (util/file-download excel-bytes
                          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                          filename))
    (catch Exception e
      (logger/log-error e "API: Ошибка при экспорте работников в Excel")
      (util/json-error 500 "EXPORT_ERROR" "Ошибка при экспорте в Excel"))))

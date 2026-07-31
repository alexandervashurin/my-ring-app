(ns my-ring-app.pdf-reports-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [my-ring-app.pdf-reports :as pdf-reports]
            [my-ring-app.test-helper :as helper]))

(use-fixtures :once
  (fn [f]
    (helper/setup-db f)))

(defn- assert-valid-pdf
  "Проверка, что файл является корректным PDF"
  [^java.io.File file]
  (is (.exists file) "Файл должен существовать")
  (is (pos? (.length file)) "Файл не должен быть пустым")
  (is (.startsWith (slurp (.getAbsolutePath file) :encoding "ISO-8859-1") "%PDF")
      "Файл должен начинаться с заголовка %PDF"))

(defn- temp-pdf-path
  "Временный путь для PDF-файла"
  []
  (.getAbsolutePath (doto (java.io.File/createTempFile "pdf_test_" ".pdf")
                      (.deleteOnExit))))

(deftest test-generate-worker-pdf
  (testing "Генерация PDF отчёта по существующему работнику"
    (let [output-path (temp-pdf-path)
          result (pdf-reports/generate-worker-pdf 1 output-path)]
      (is (:success result) "Генерация должна завершиться успешно")
      (assert-valid-pdf (io/file output-path)))))

(deftest test-generate-worker-pdf-not-found
  (testing "Генерация PDF отчёта по несуществующему работнику"
    (let [result (pdf-reports/generate-worker-pdf 99999 (temp-pdf-path))]
      (is (false? (:success result)) "Должен вернуть ошибку")
      (is (= "Работник не найден" (:message result)) "Сообщение об ошибке"))))

(deftest test-generate-workers-list-pdf
  (testing "Генерация PDF списка работников"
    (let [output-path (temp-pdf-path)
          result (pdf-reports/generate-workers-list-pdf output-path)]
      (is (:success result) "Генерация должна завершиться успешно")
      (assert-valid-pdf (io/file output-path)))))

(deftest test-generate-salary-report-pdf
  (testing "Генерация PDF отчёта по зарплате с данными (2025-10)"
    (let [output-path (temp-pdf-path)
          result (pdf-reports/generate-salary-report-pdf output-path 2025 10)]
      (is (:success result) "Генерация должна завершиться успешно")
      (assert-valid-pdf (io/file output-path)))))

(deftest test-generate-salary-report-pdf-empty-period
  (testing "Генерация PDF отчёта по зарплате без данных за период"
    (let [output-path (temp-pdf-path)
          result (pdf-reports/generate-salary-report-pdf output-path 1999 1)]
      (is (:success result) "Отчёт без данных должен формироваться")
      (assert-valid-pdf (io/file output-path)))))

(deftest test-generate-salary-report-pdf-invalid-month
  (testing "Генерация PDF отчёта по зарплате с некорректным месяцем"
    (let [result (pdf-reports/generate-salary-report-pdf (temp-pdf-path) 2025 13)]
      (is (false? (:success result)) "Должен вернуть ошибку")
      (is (= "Некорректные параметры отчёта" (:message result)) "Сообщение об ошибке"))))

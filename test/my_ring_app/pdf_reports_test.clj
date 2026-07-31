(ns my-ring-app.pdf-reports-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [my-ring-app.model :as model]
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

(deftest test-generate-worker-pdf-org-filter
  (testing "Работник из другой организации недоступен по org-id"
    (let [result (pdf-reports/generate-worker-pdf 3 (temp-pdf-path) 1)]
      (is (false? (:success result)) "Работник чужой организации не должен находиться")
      (is (= "Работник не найден" (:message result)) "Сообщение об ошибке")))
  (testing "Работник своей организации находится по org-id"
    (let [output-path (temp-pdf-path)
          result (pdf-reports/generate-worker-pdf 3 output-path 2)]
      (is (:success result) "Генерация должна завершиться успешно")
      (assert-valid-pdf (io/file output-path)))))

(deftest test-generate-workers-list-pdf
  (testing "Генерация PDF списка работников"
    (let [output-path (temp-pdf-path)
          result (pdf-reports/generate-workers-list-pdf output-path)]
      (is (:success result) "Генерация должна завершиться успешно")
      (assert-valid-pdf (io/file output-path)))))

(deftest test-generate-workers-list-pdf-org-filter
  (testing "Список работников фильтруется по организации"
    (let [output-path-1 (temp-pdf-path)
          output-path-2 (temp-pdf-path)
          result-1 (pdf-reports/generate-workers-list-pdf output-path-1 1)
          result-2 (pdf-reports/generate-workers-list-pdf output-path-2 2)]
      (is (:success result-1))
      (is (:success result-2))
      (assert-valid-pdf (io/file output-path-1))
      (assert-valid-pdf (io/file output-path-2))
      (is (= 4 (count (model/get-workers-with-details 1))) "Орг 1: 4 работника (1, 2, 5, 6)")
      (is (= 2 (count (model/get-workers-with-details 2))) "Орг 2: 2 работника (3, 7)"))))

(deftest test-generate-salary-report-pdf-org-filter
  (testing "Отчёт по зарплате фильтруется по организации"
    (let [output-path (temp-pdf-path)
          result (pdf-reports/generate-salary-report-pdf output-path 2025 10 1)]
      (is (:success result))
      (assert-valid-pdf (io/file output-path))
      (let [records (filter (fn [r]
                              (and (= 2025 (:год r)) (= 10 (:месяц r))))
                            (model/get-salary-with-details 1))
            total (reduce + (map (fn [r] (long (:общая_зарплата r))) records))]
        (is (= 4 (count records)) "Орг 1: 4 записи начисления")
        (is (= 320082 total) "Орг 1: итоговая сумма 120000 + 85000 + 49680 + 65402")))))

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

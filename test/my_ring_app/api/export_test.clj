(ns my-ring-app.api.export-test
  "Тесты для REST API экспорта данных"
  (:require [clojure.test :refer :all]
            [my-ring-app.api.export :refer :all]
            [my-ring-app.views.layout :refer [html-escape]]
            [my-ring-app.model :as model]
            [my-ring-app.test-helper :refer [make-request]]))

;; ======================================================================
;; Тесты для CSV экспорта
;; ======================================================================

(deftest test-export-workers-csv
  (testing "Экспорт работников в CSV"
    (let [request (make-request :get "/api/export/workers.csv")
          response (export-workers-csv request)]
      (is (= 200 (:status response)))
      (is (= "text/csv; charset=utf-8" (get-in response [:headers "Content-Type"])))
      (is (re-find #"workers_.*\.csv" (get-in response [:headers "Content-Disposition"])))
      (is (string? (:body response)))
      (is (re-find #"ID,Фамилия,Имя" (:body response))))))

(deftest test-export-salary-csv
  (testing "Экспорт зарплаты в CSV"
    (let [request (make-request :get "/api/export/salary.csv")
          response (export-salary-csv request)]
      (is (= 200 (:status response)))
      (is (= "text/csv; charset=utf-8" (get-in response [:headers "Content-Type"])))
      (is (re-find #"salary_.*\.csv" (get-in response [:headers "Content-Disposition"])))
      (is (string? (:body response))))))

;; ======================================================================
;; Тесты для Excel экспорта
;; ======================================================================

(deftest test-export-workers-excel
  (testing "Экспорт работников в Excel"
    (let [request (make-request :get "/api/export/workers.xlsx")
          response (export-workers-excel request)]
      (is (= 200 (:status response)))
      (is (= "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
             (get-in response [:headers "Content-Type"])))
      (is (re-find #"workers_.*\.xlsx" (get-in response [:headers "Content-Disposition"])))
      (is (bytes? (:body response)))
      (is (pos? (count (:body response)))))))

(deftest test-html-escape
  (testing "Экранирование HTML"
    (is (= "" (html-escape nil)))
    (is (= "test" (html-escape "test")))
    (is (= "&amp;" (html-escape "&")))
    (is (= "&lt;" (html-escape "<")))
    (is (= "&gt;" (html-escape ">")))))

(ns my-ring-app.api.export-test
  "Тесты для REST API экспорта данных"
  (:require [clojure.test :refer :all]
            [my-ring-app.api.export :refer :all]
            [my-ring-app.model :as model]))

;; ======================================================================
;; Вспомогательные функции
;; ======================================================================

(defn- make-request
  "Создание тестового запроса"
  ([method uri]
   {:request-method method
    :uri uri
    :params {}
    :route-params {}
    :identity nil})
  ([method uri params]
   {:request-method method
    :uri uri
    :params params
    :route-params {}
    :identity nil}))

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

;; ======================================================================
;; Тесты вспомогательных функций
;; ======================================================================

(deftest test-html-escape
  (testing "Экранирование HTML"
    (is (= "" (html-escape nil)))
    (is (= "test" (html-escape "test")))
    (is (= "&amp;" (html-escape "&")))
    (is (= "&lt;" (html-escape "<")))
    (is (= "&gt;" (html-escape ">")))))

(deftest test-workers-to-csv
  (testing "Конвертация работников в CSV"
    (let [workers [{:id 1 :фамилия "Иванов" :имя "Иван" :отчество "Иванович"
                    :дата_приема "2024-01-01" :цех "Цех 1" :система_оплаты "Оклад"
                    :категория "Рабочий" :разряд "5" :режим "Односменный"}]
          csv-content (workers-to-csv workers)]
      (is (string? csv-content))
      (is (re-find #"ID,Фамилия,Имя" csv-content))
      (is (re-find #"1,Иванов,Иван" csv-content)))))

(deftest test-salary-to-csv
  (testing "Конвертация зарплаты в CSV"
    (let [salary-records [{:id 1 :фамилия "Иванов" :имя "Иван" :год 2025 :месяц 10
                           :общая_зарплата 50000 :зарплата_за_больничные_дни 0
                           :зарплата_за_командировочные_дни 0}]
          csv-content (salary-to-csv salary-records)]
      (is (string? csv-content))
      (is (re-find #"ID,Работник,Год,Месяц" csv-content))
      (is (re-find #"1,Иванов Иван,2025,10" csv-content)))))

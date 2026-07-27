(ns my-ring-app.util-test
  "Тесты для общих вспомогательных функций"
  (:require [clojure.test :refer :all]
            [my-ring-app.util :as util]))

;; ======================================================================
;; Тесты parse-int
;; ======================================================================

(deftest test-parse-int
  (testing "Корректные значения"
    (is (= 42 (util/parse-int "42" 0)))
    (is (= 0 (util/parse-int "0" 10)))
    (is (= -5 (util/parse-int "-5" 0)))
    (is (= 100 (util/parse-int " 100 " 0))))

  (testing "Некорректные значения возвращают default"
    (is (= 0 (util/parse-int nil 0)))
    (is (= 0 (util/parse-int "" 0)))
    (is (= 0 (util/parse-int " " 0)))
    (is (= 0 (util/parse-int "abc" 0)))
    (is (= 0 (util/parse-int "12.5" 0)))
    (is (= 42 (util/parse-int "abc" 42)))))

;; ======================================================================
;; Тесты validate-id
;; ======================================================================

(deftest test-validate-id
  (testing "Корректные ID"
    (is (= 1 (util/validate-id "1")))
    (is (= 123 (util/validate-id "123")))
    (is (= 1 (util/validate-id 1)))
    (is (= 123 (util/validate-id 123))))

  (testing "ID с мусорными символами (оставляются только цифры)"
    (is (= 123 (util/validate-id "abc123")))
    (is (= 123 (util/validate-id "123abc")))
    (is (= 1 (util/validate-id "abc1def"))))

  (testing "Некорректные ID возвращают nil"
    (is (nil? (util/validate-id nil)))
    (is (nil? (util/validate-id "")))
    (is (nil? (util/validate-id " ")))
    (is (nil? (util/validate-id "abc")))))

;; ======================================================================
;; Тесты success-response
;; ======================================================================

(deftest test-success-response
  (testing "Одиночная арность"
    (let [resp (util/success-response {:id 1})]
      (is (true? (:success resp)))
      (is (= {:id 1} (:data resp)))
      (is (= "Операция выполнена успешно" (:message resp)))))

  (testing "Двойная арность"
    (let [resp (util/success-response {:id 1} "Создано")]
      (is (true? (:success resp)))
      (is (= {:id 1} (:data resp)))
      (is (= "Создано" (:message resp))))))

;; ======================================================================
;; Тесты error-response
;; ======================================================================

(deftest test-error-response
  (testing "Двойная арность"
    (let [resp (util/error-response "NOT_FOUND" "Не найдено")]
      (is (false? (:success resp)))
      (is (= "NOT_FOUND" (get-in resp [:error :code])))
      (is (= "Не найдено" (get-in resp [:error :message])))))

  (testing "Тройная арность с деталями"
    (let [resp (util/error-response "VALIDATION_ERROR" "Ошибка" {:field "name"})]
      (is (false? (:success resp)))
      (is (= "VALIDATION_ERROR" (get-in resp [:error :code])))
      (is (= {:field "name"} (get-in resp [:error :details]))))))

;; ======================================================================
;; Тесты validate-email
;; ======================================================================

(deftest test-validate-email
  (testing "Корректные email"
    (is (true? (util/validate-email "user@example.com")))
    (is (true? (util/validate-email "test.name@domain.org")))
    (is (true? (util/validate-email "user+tag@example.com"))))

  (testing "Некорректные email"
    (is (not (util/validate-email nil)))
    (is (not (util/validate-email "")))
    (is (not (util/validate-email "not-an-email")))
    (is (not (util/validate-email "@domain.com")))
    (is (not (util/validate-email "user@")))
    (is (not (util/validate-email "user@domain")))
    (is (not (util/validate-email "user domain@example.com")))))

;; ======================================================================
;; Тесты validate-year-month
;; ======================================================================

(deftest test-validate-year-month
  (testing "Корректные значения"
    (is (nil? (util/validate-year-month 2025 1)))
    (is (nil? (util/validate-year-month 2000 12)))
    (is (nil? (util/validate-year-month 2100 6))))

  (testing "Некорректный год"
    (let [err (util/validate-year-month 1999 6)]
      (is (some? err))
      (is (= "INVALID_PARAMS" (:error err))))

    (let [err (util/validate-year-month 2101 6)]
      (is (some? err))
      (is (= "INVALID_PARAMS" (:error err)))))

  (testing "Некорректный месяц"
    (let [err (util/validate-year-month 2025 0)]
      (is (some? err))
      (is (= "INVALID_PARAMS" (:error err))))

    (let [err (util/validate-year-month 2025 13)]
      (is (some? err))
      (is (= "INVALID_PARAMS" (:error err))))))

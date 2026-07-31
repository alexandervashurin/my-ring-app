(ns my-ring-app.email-test
  (:require [clojure.test :refer :all]
            [my-ring-app.email :as email]))

(deftest test-send-email
  (testing "send-email возвращает статус отключённой функциональности"
    (let [result (email/send-email "test@example.com" "Тема" "Тело")]
      (is (false? (:success result)))
      (is (= "Email функциональность отключена" (:message result))))))

(deftest test-notify-new-worker
  (testing "notify-new-worker возвращает статус отключённой функциональности"
    (let [result (email/notify-new-worker {:фамилия "Тестов"} ["hr@example.com"])]
      (is (false? (:success result)))
      (is (= "Email функциональность отключена" (:message result))))))

(deftest test-notify-birthday
  (testing "notify-birthday возвращает статус отключённой функциональности"
    (let [result (email/notify-birthday {:фамилия "Тестов"} 30 ["hr@example.com"])]
      (is (false? (:success result)))
      (is (= "Email функциональность отключена" (:message result))))))

(deftest test-notify-work-anniversary
  (testing "notify-work-anniversary возвращает статус отключённой функциональности"
    (let [result (email/notify-work-anniversary {:фамилия "Тестов"} 5 ["hr@example.com"])]
      (is (false? (:success result)))
      (is (= "Email функциональность отключена" (:message result))))))

(deftest test-test-email-connection
  (testing "test-email-connection возвращает статус отключённой функциональности"
    (let [result (email/test-email-connection)]
      (is (false? (:success result)))
      (is (= "Email функциональность отключена" (:message result))))))

(ns my-ring-app.email-test
  (:require [clojure.test :refer :all]
            [my-ring-app.email :as email]
            [my-ring-app.config :as config]
            [postal.core :as postal]))

(defn- with-disabled-smtp
  "SMTP отключён (значение по умолчанию в тестах)"
  [f]
  (with-redefs [config/smtp-config (constantly {:enabled false :host nil :port 587
                                                :user nil :password nil :from "noreply@localhost"})]
    (f)))

(defn- with-enabled-smtp
  "SMTP включён (мок: postal/send-message перехватывается)"
  [f]
  (with-redefs [config/smtp-config (constantly {:enabled true :host "smtp.example.com" :port 587
                                                :user "user" :password "pass" :from "hr@example.com"
                                                :tls true :ssl false})
                postal/send-message (fn [msg] nil)]
    (f)))

(deftest test-smtp-disabled-by-default
  (testing "По умолчанию (без SMTP_HOST) почта отключена"
    (is (false? (email/smtp-enabled?)))))

(deftest test-send-email-disabled
  (testing "send-email при отключённом SMTP возвращает статус отключения"
    (with-disabled-smtp
      #(let [result (email/send-email "test@example.com" "Тема" "Тело")]
         (is (false? (:success result)))
         (is (= 0 (:sent result)))
         (is (re-find #"SMTP_HOST" (:message result)))))))

(deftest test-send-email-enabled-success
  (testing "send-email при включённом SMTP успешно отправляет (mock postal)"
    (with-enabled-smtp
      #(let [result (email/send-email "test@example.com" "Тема" "Тело")]
         (is (true? (:success result)))
         (is (= 1 (:sent result)))))))

(deftest test-send-email-enabled-failure
  (testing "send-email возвращает ошибку, если postal вернул ошибку"
    (with-redefs [config/smtp-config (constantly {:enabled true :host "smtp.example.com" :port 587
                                                  :user "user" :password "pass" :from "hr@example.com"
                                                  :tls true :ssl false})
                  postal/send-message (fn [msg] {:error :error :message "550 Access denied"})]
      (let [result (email/send-email "test@example.com" "Тема" "Тело")]
        (is (false? (:success result)))
        (is (= 0 (:sent result)))
        (is (re-find #"550" (:message result)))))))

(deftest test-send-email-message-build
  (testing "send-email формирует корректное сообщение для postal"
    (let [captured (atom nil)]
      (with-redefs [config/smtp-config (constantly {:enabled true :host "smtp.example.com" :port 587
                                                    :user "user" :password "pass" :from "hr@example.com"
                                                    :tls true :ssl false})
                    postal/send-message (fn [msg] (reset! captured msg) nil)]
        (email/send-email "to@example.com" "Тема" "Привет" "cc@example.com")
        (let [msg @captured]
          (is (= "hr@example.com" (:from msg)))
          (is (= "to@example.com" (:to msg)))
          (is (= "cc@example.com" (:cc msg)))
          (is (= "Тема" (:subject msg)))
          (is (= [:alternative] (vec (take 1 (:body msg))))))))))

(deftest test-notify-new-worker
  (testing "notify-new-worker при отключённом SMTP возвращает статус отключения"
    (with-disabled-smtp
      #(let [result (email/notify-new-worker {:фамилия "Тестов" :имя "Тест" :отчество "Тестович"} ["hr@example.com"])]
         (is (false? (:success result)))
         (is (= 0 (:sent result)))))))

(deftest test-notify-new-worker-enabled
  (testing "notify-new-worker отправляет письмо каждому получателю (mock postal)"
    (with-enabled-smtp
      #(let [result (email/notify-new-worker {:id 7 :фамилия "Тестов" :имя "Тест" :отчество "Тестович" :дата_приема "2026-08-01"}
                                             ["hr1@example.com" "hr2@example.com"])]
         (is (true? (:success result)))
         (is (= 2 (:sent result)))))))

(deftest test-notify-birthday
  (testing "notify-birthday при отключённом SMTP возвращает статус отключения"
    (with-disabled-smtp
      #(let [result (email/notify-birthday {:фамилия "Тестов"} 30 ["hr@example.com"])]
         (is (false? (:success result)))
         (is (= 0 (:sent result)))))))

(deftest test-notify-birthday-enabled
  (testing "notify-birthday отправляет письмо (mock postal)"
    (with-enabled-smtp
      #(let [result (email/notify-birthday {:фамилия "Тестов" :имя "Тест"} 30 ["hr@example.com"])]
         (is (true? (:success result)))
         (is (= 1 (:sent result)))))))

(deftest test-notify-work-anniversary
  (testing "notify-work-anniversary при отключённом SMTP возвращает статус отключения"
    (with-disabled-smtp
      #(let [result (email/notify-work-anniversary {:фамилия "Тестов"} 5 ["hr@example.com"])]
         (is (false? (:success result)))
         (is (= 0 (:sent result)))))))

(deftest test-notify-work-anniversary-enabled
  (testing "notify-work-anniversary отправляет письмо (mock postal)"
    (with-enabled-smtp
      #(let [result (email/notify-work-anniversary {:фамилия "Тестов" :имя "Тест"} 5 ["hr@example.com"])]
         (is (true? (:success result)))
         (is (= 1 (:sent result)))))))

(deftest test-test-email-connection-disabled
  (testing "test-email-connection при отключённом SMTP возвращает статус отключения"
    (with-disabled-smtp
      #(let [result (email/test-email-connection)]
         (is (false? (:success result)))
         (is (re-find #"SMTP_HOST" (:message result)))))))

(ns my-ring-app.api.notifications-test
  "Тесты для REST API email уведомлений (защита от IDOR)"
  (:require [clojure.test :refer :all]
            [my-ring-app.api.notifications :refer :all]
            [my-ring-app.model :as model]
            [my-ring-app.test-helper :as helper]))

(use-fixtures :once helper/setup-db)

(defn make-request
  "Создание тестового запроса с org-id"
  [params org-id]
  {:request-method :post
   :params params
   :route-params {}
   :org-id org-id})

(defn- get-body
  [response]
  (:body response))

;; ======================================================================
;; Тесты IDOR: работник другой организации недоступен
;; ======================================================================

(deftest test-notify-new-worker-own-org
  (testing "Уведомление о работнике своей организации доступно"
    (let [request (make-request {:worker_id "1" :recipients "test@example.com"} 1)
          response (notify-new-worker-api request)]
      (is (= 200 (:status response))))))

(deftest test-notify-new-worker-org-scoped
  (testing "Уведомление о работнике другой организации недоступно (IDOR)"
    (let [request (make-request {:worker_id "3" :recipients "test@example.com"} 1)
          response (notify-new-worker-api request)
          body (get-body response)]
      (is (= 404 (:status response)))
      (is (= "NOT_FOUND" (get-in body [:error :code]))))))

(deftest test-notify-birthday-org-scoped
  (testing "День рождения работника другой организации недоступен (IDOR)"
    (let [request (make-request {:worker_id "3" :age "30" :recipients "test@example.com"} 1)
          response (notify-birthday-api request)
          body (get-body response)]
      (is (= 404 (:status response)))
      (is (= "NOT_FOUND" (get-in body [:error :code]))))))

(deftest test-notify-birthday-own-org
  (testing "День рождения работника своей организации доступен"
    (let [request (make-request {:worker_id "1" :age "30" :recipients "test@example.com"} 1)
          response (notify-birthday-api request)]
      (is (= 200 (:status response))))))

(deftest test-notify-anniversary-org-scoped
  (testing "Годовщина работника другой организации недоступна (IDOR)"
    (let [request (make-request {:worker_id "3" :years "5" :recipients "test@example.com"} 1)
          response (notify-anniversary-api request)
          body (get-body response)]
      (is (= 404 (:status response)))
      (is (= "NOT_FOUND" (get-in body [:error :code]))))))

(deftest test-notify-anniversary-own-org
  (testing "Годовщина работника своей организации доступна"
    (let [request (make-request {:worker_id "1" :years "5" :recipients "test@example.com"} 1)
          response (notify-anniversary-api request)]
      (is (= 200 (:status response))))))

(deftest test-notify-anniversary-missing-worker
  (testing "Несуществующий работник — 404"
    (let [request (make-request {:worker_id "999" :years "5" :recipients "test@example.com"} 1)
          response (notify-anniversary-api request)]
      (is (= 404 (:status response))))))

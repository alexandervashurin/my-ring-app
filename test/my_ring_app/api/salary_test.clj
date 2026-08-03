(ns my-ring-app.api.salary-test
  "Тесты для REST API зарплаты и учёта рабочего времени"
  (:require [clojure.test :refer :all]
            [clojure.edn :as edn]
            [my-ring-app.api.salary :refer :all]
            [my-ring-app.test-helper :as helper]))

(use-fixtures :once helper/setup-db)

(defn- make-request
  "Создание тестового запроса с identity и org-id"
  [method uri params route-params identity org-id]
  {:request-method method
   :uri uri
   :params params
   :route-params route-params
   :identity identity
   :org-id org-id})

(defn- get-body
  [response]
  (let [body (:body response)]
    (if (string? body)
      (edn/read-string body)
      body)))

;; ======================================================================
;; Тесты для GET /api/salary/:worker-id (защита от IDOR)
;; ======================================================================

(deftest test-get-worker-salary-own-org
  (testing "Зарплата работника своей организации доступна"
    (let [request (make-request :get "/api/salary/1" {:year "2025" :month "10"}
                                {:worker-id "1"}
                                {:role "manager" :organization_id 1}
                                1)
          response (get-worker-salary request)]
      (is (= 200 (:status response))))))

(deftest test-get-worker-salary-org-scoped
  (testing "Зарплата работника другой организации недоступна (IDOR)"
    (let [request (make-request :get "/api/salary/3" {:year "2025" :month "10"}
                                {:worker-id "3"}
                                {:role "manager" :organization_id 1}
                                1)
          response (get-worker-salary request)]
      (is (= 404 (:status response))))))

(deftest test-get-worker-work-time-org-scoped
  (testing "Учёт времени работника другой организации недоступен (IDOR)"
    (let [request (make-request :get "/api/work-time/3" {}
                                {:worker-id "3"}
                                {:role "manager" :organization_id 1}
                                1)
          response (get-worker-work-time request)]
      (is (= 200 (:status response)))
      (is (empty? (get-in (get-body response) [:data]))))))

;; ======================================================================
;; Тесты для PUT /api/work-time/:id (защита от IDOR)
;; ======================================================================

(deftest test-update-work-time-org-scoped
  (testing "Обновление учёта времени чужой организации запрещено (IDOR)"
    (let [request (make-request :put "/api/work-time/3" {:всего_отработанных_часов 160}
                                {:id "3"}
                                {:role "manager" :organization_id 1}
                                1)
          response (update-work-time request)
          body (get-body response)]
      (is (= 404 (:status response)))
      (is (= "NOT_FOUND" (get-in body [:error :code]))))))

(deftest test-update-work-time-own-org
  (testing "Обновление учёта времени своей организации доступно"
    (let [request (make-request :put "/api/work-time/1"
                                {:год "2025" :месяц "10"
                                 :всего_часов_за_месяц_по_плану 160
                                 :всего_часов_в_месяц_по_факту 160
                                 :количество_отработанных_дней 20
                                 :количество_рабочих_часов_в_день 8
                                 :всего_отработанных_часов 160
                                 :сколько_должны_отработать 160
                                 :больничные_дни 0
                                 :командировочные_дни 0}
                                {:id "1"}
                                {:role "manager" :organization_id 1}
                                1)
          response (update-work-time request)]
      (is (= 200 (:status response))))))

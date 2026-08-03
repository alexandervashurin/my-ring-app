(ns my-ring-app.api.dashboard-test
  "Тесты для REST API дашборда и аналитики (org-scoping)"
  (:require [clojure.test :refer :all]
            [clojure.edn :as edn]
            [my-ring-app.api.dashboard :refer :all]
            [my-ring-app.test-helper :as helper]))

(use-fixtures :once helper/setup-db)

(defn- get-body
  "Получение тела ответа (map или JSON)"
  [response]
  (let [body (:body response)]
    (if (string? body)
      (edn/read-string body)
      body)))

(defn- org-req
  "Запрос с привязкой к организации"
  [org-id]
  {:request-method :get
   :uri "/api/dashboard"
   :params {}
   :route-params {}
   :identity {:role "manager" :organization_id org-id}
   :org-id org-id})

(deftest test-get-dashboard
  (testing "Получение данных дашборда"
    (let [response (get-dashboard (org-req 1))
          body (get-body response)]
      (is (= 200 (:status response)))
      (is (true? (:success body)))
      (is (map? (:data body))))))

(deftest test-get-dashboard-stats
  (testing "Основная статистика дашборда"
    (let [response (get-dashboard-stats (org-req 1))
          body (get-body response)]
      (is (= 200 (:status response)))
      (is (true? (:success body)))
      (is (contains? (:data body) :total-workers)))))

(deftest test-get-dashboard-stats-org-scoped
  (testing "Статистика ограничена своей организацией"
    (let [stats-org1 (get-body (get-dashboard-stats (org-req 1)))
          stats-org2 (get-body (get-dashboard-stats (org-req 2)))
          workers-org1 (get-in stats-org1 [:data :total-workers])
          workers-org2 (get-in stats-org2 [:data :total-workers])]
      (is (= 4 workers-org1))
      (is (= 2 workers-org2)))))

(deftest test-get-workers-by-shop-org-scoped
  (testing "Распределение по цехам ограничено своей организацией"
    (let [data-org1 (:data (get-body (get-workers-by-shop (org-req 1))))
          data-org2 (:data (get-body (get-workers-by-shop (org-req 2))))
          sum1 (reduce + (map :count data-org1))
          sum2 (reduce + (map :count data-org2))]
      (is (= 4 sum1))
      (is (= 2 sum2)))))

(deftest test-get-workers-by-category-org-scoped
  (testing "Распределение по категориям ограничено своей организацией"
    (let [data-org1 (:data (get-body (get-workers-by-category (org-req 1))))
          data-org2 (:data (get-body (get-workers-by-category (org-req 2))))
          sum1 (reduce + (map :count data-org1))
          sum2 (reduce + (map :count data-org2))]
      (is (= 4 sum1))
      (is (= 2 sum2)))))

(deftest test-get-salary-distribution
  (testing "Распределение по зарплате"
    (let [response (get-salary-distribution (org-req 1))
          body (get-body response)]
      (is (= 200 (:status response)))
      (is (true? (:success body)))
      (is (coll? (:data body))))))

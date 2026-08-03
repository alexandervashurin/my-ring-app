(ns my-ring-app.sse-test
  (:require [clojure.test :refer :all]
            [my-ring-app.sse :as sse]
            [my-ring-app.model :as model]
            [my-ring-app.cache :as cache]))

(defn- stub-stats
  "Заглушка для model/get-dashboard-stats (org-aware)"
  [total-workers total-payroll avg-salary]
  (fn [org-id]
    {:total-workers total-workers
     :total-shops 5
     :avg-salary avg-salary
     :total-payroll total-payroll}))

(deftest test-dashboard-poll
  (testing "Первый вызов: changed=true, корректная структура ответа"
    (with-redefs [model/get-dashboard-stats (stub-stats 80 1000000 50000)
                  cache/cache-status (constantly {:age-ms 100})]
      (let [response (sse/dashboard-poll {:org-id 1})
            body (:body response)]
        (is (= 200 (:status response)))
        (is (.contains (get-in response [:headers "Content-Type"]) "application/json"))
        (is (true? (:changed body)))
        (is (= 80 (get-in body [:stats :total_workers])))
        (is (= 1000000 (get-in body [:stats :total_payroll])))
        (is (= 100 (get-in body [:stats :cache_age_ms]))))))
  (testing "Данные не изменились: changed=false"
    (with-redefs [model/get-dashboard-stats (stub-stats 80 1000000 50000)
                  cache/cache-status (constantly {:age-ms 100})]
      (sse/dashboard-poll {:org-id 1})
      (let [response (sse/dashboard-poll {:org-id 1})]
        (is (false? (:changed (:body response)))))))
  (testing "Изменилось количество работников: changed=true"
    (with-redefs [model/get-dashboard-stats (stub-stats 80 1000000 50000)
                  cache/cache-status (constantly {:age-ms 100})]
      (sse/dashboard-poll {:org-id 1})
      (with-redefs [model/get-dashboard-stats (stub-stats 81 1000000 50000)]
        (let [response (sse/dashboard-poll {:org-id 1})]
          (is (true? (:changed (:body response))))
          (is (= 81 (get-in (:body response) [:stats :total_workers]))))))))

(deftest test-dashboard-poll-org-isolation
  (testing "Diff-кэш изолирован по организациям"
    (with-redefs [model/get-dashboard-stats (stub-stats 80 1000000 50000)
                  cache/cache-status (constantly {:age-ms 100})]
      (sse/dashboard-poll {:org-id 1})
      (with-redefs [model/get-dashboard-stats (stub-stats 50 500000 30000)]
        (let [response-org1 (sse/dashboard-poll {:org-id 1})
              response-org2 (sse/dashboard-poll {:org-id 2})]
          (is (true? (:changed (:body response-org1))))
          (is (true? (:changed (:body response-org2))))
          (is (= 50 (get-in (:body response-org2) [:stats :total_workers]))))))))

(deftest test-dashboard-poll-error
  (testing "Ошибка при вычислении статистики: 500"
    (with-redefs [model/get-dashboard-stats (fn [org-id] (throw (Exception. "boom")))
                  cache/cache-status (constantly {:age-ms 0})]
      (let [response (sse/dashboard-poll {})]
        (is (= 500 (:status response)))
        (is (= "Internal server error" (:error (:body response))))))))

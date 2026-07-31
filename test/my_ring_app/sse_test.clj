(ns my-ring-app.sse-test
  (:require [clojure.test :refer :all]
            [my-ring-app.sse :as sse]
            [my-ring-app.model :as model]
            [my-ring-app.cache :as cache]))

(defn- stub-stats
  "Заглушка для model/get-dashboard-stats"
  [total-workers total-payroll avg-salary]
  (fn []
    {:total-workers total-workers
     :total-shops 5
     :avg-salary avg-salary
     :total-payroll total-payroll}))

(deftest test-dashboard-poll
  (testing "Первый вызов: changed=true, корректная структура ответа"
    (with-redefs [model/get-dashboard-stats (stub-stats 80 1000000 50000)
                  cache/cache-status (constantly {:age-ms 100})]
      (let [response (sse/dashboard-poll {})
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
      (sse/dashboard-poll {})
      (let [response (sse/dashboard-poll {})]
        (is (false? (:changed (:body response)))))))
  (testing "Изменилось количество работников: changed=true"
    (with-redefs [model/get-dashboard-stats (stub-stats 80 1000000 50000)
                  cache/cache-status (constantly {:age-ms 100})]
      (sse/dashboard-poll {})
      (with-redefs [model/get-dashboard-stats (stub-stats 81 1000000 50000)]
        (let [response (sse/dashboard-poll {})]
          (is (true? (:changed (:body response))))
          (is (= 81 (get-in (:body response) [:stats :total_workers]))))))))

(deftest test-dashboard-poll-error
  (testing "Ошибка при вычислении статистики: 500"
    (with-redefs [model/get-dashboard-stats (fn [] (throw (Exception. "boom")))
                  cache/cache-status (constantly {:age-ms 0})]
      (let [response (sse/dashboard-poll {})]
        (is (= 500 (:status response)))
        (is (= "Internal server error" (:error (:body response))))))))

(ns my-ring-app.rate-limit-test
  "Тесты для rate limiting middleware"
  (:require [clojure.test :refer :all]
            [my-ring-app.rate-limit :as rl]))

(defn- make-request
  "Создание тестового запроса"
  ([uri]
   (make-request uri "127.0.0.1"))
  ([uri ip]
   {:request-method :get
    :uri uri
    :headers {}
    :remote-addr ip}))

(defn- dummy-handler
  "Заглушка для handler"
  [request]
  {:status 200 :body "ok"})

(deftest test-rate-limit-allows-normal-traffic
  (testing "Обычный трафик проходит"
    (let [handler (rl/wrap-rate-limit dummy-handler)
          response (handler (make-request "/"))]
      (is (= 200 (:status response))))))

(deftest test-rate-limit-adds-headers
  (testing "Ответ содержит заголовки rate limit"
    (let [handler (rl/wrap-rate-limit dummy-handler)
          response (handler (make-request "/"))]
      (is (get-in response [:headers "X-RateLimit-Limit"]))
      (is (get-in response [:headers "X-RateLimit-Remaining"]))
      (is (get-in response [:headers "X-RateLimit-Reset"])))))

(deftest test-rate-limit-different-ips
  (testing "Разные IP считаются раздельно"
    (let [handler (rl/wrap-rate-limit dummy-handler)
          resp1 (handler (make-request "/" "1.1.1.1"))
          resp2 (handler (make-request "/" "2.2.2.2"))]
      (is (= 200 (:status resp1)))
      (is (= 200 (:status resp2))))))

(deftest test-rate-limit-api-routes
  (testing "API маршруты используют строгие лимиты"
    (let [handler (rl/wrap-rate-limit dummy-handler {:api-routes? true})
          response (handler (make-request "/test"))]
      (is (= 200 (:status response))))))

(deftest test-rate-limit-detects-api-by-uri
  (testing "API определяется по URI /api/"
    (let [handler (rl/wrap-rate-limit dummy-handler)
          response (handler (make-request "/api/test"))]
      (is (= 200 (:status response))))))

(deftest test-rate-limit-x-forwarded-for
  (testing "IP из X-Forwarded-For заголовка"
    (let [handler (rl/wrap-rate-limit dummy-handler)
          response (handler {:request-method :get
                             :uri "/"
                             :headers {"x-forwarded-for" "10.0.0.1"}
                             :remote-addr "127.0.0.1"})]
      (is (= 200 (:status response))))))

(deftest test-rate-limit-remaining-decrements
  (testing "X-RateLimit-Remaining уменьшается"
    (let [handler (rl/wrap-rate-limit dummy-handler)
          resp1 (handler (make-request "/" "9.9.9.9"))
          resp2 (handler (make-request "/" "9.9.9.9"))
          remaining1 (Integer/parseInt (get-in resp1 [:headers "X-RateLimit-Remaining"]))
          remaining2 (Integer/parseInt (get-in resp2 [:headers "X-RateLimit-Remaining"]))]
      (is (< remaining2 remaining1)))))

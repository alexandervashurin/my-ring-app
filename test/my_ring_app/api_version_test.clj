(ns my-ring-app.api-version-test
  (:require [clojure.test :refer :all]
            [my-ring-app.api-version :as api-version]))

(deftest test-wrap-api-version
  (testing "Добавляет заголовок X-API-Version ко всем ответам"
    (let [handler (api-version/wrap-api-version (fn [_] {:status 200 :body "ok"}))
          response (handler {})]
      (is (= "v1" (get-in response [:headers "X-API-Version"])))))
  (testing "Не ломает существующие заголовки ответа"
    (let [handler (api-version/wrap-api-version (fn [_] {:status 200 :headers {"Content-Type" "text/plain"}}))
          response (handler {})]
      (is (= "text/plain" (get-in response [:headers "Content-Type"])))
      (is (= "v1" (get-in response [:headers "X-API-Version"]))))))

(deftest test-wrap-api-v1-rewrite
  (testing "Переписывает /api/v1/* → /api/*"
    (let [seen (atom nil)
          handler (api-version/wrap-api-v1-rewrite
                   (fn [request] (reset! seen (:uri request)) {:status 200}))
          response (handler {:uri "/api/v1/workers" :request-method :get})]
      (is (= 200 (:status response)))
      (is (= "/api/workers" @seen))))
  (testing "Оставляет без изменений пути вне /api/v1/"
    (let [seen (atom [])
          handler (api-version/wrap-api-v1-rewrite
                   (fn [request] (swap! seen conj (:uri request)) {:status 200}))]
      (handler {:uri "/api/workers"})
      (handler {:uri "/login"})
      (handler {:uri "/api/v10/workers"})
      (handler {:uri "/api/v1"})
      (is (= ["/api/workers" "/login" "/api/v10/workers" "/api/v1"] @seen)))))

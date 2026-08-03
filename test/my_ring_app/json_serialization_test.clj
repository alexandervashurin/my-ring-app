(ns my-ring-app.json-serialization-test
  "Тесты сериализации дат в JSON-ответах.
   java.sql.Date/Timestamp не должны превращаться в UTC ISO (со сдвигом на день)."
  (:require [cheshire.core :as json]
            [clojure.test :refer :all]
            [my-ring-app.core :as core]))

(defn- run-through-middleware
  "Прогон ответа через wrap-json-response и парсинг тела"
  [body]
  (let [handler (core/wrap-json-response (fn [_] {:status 200 :body body}))
        response (handler {})
        parsed (json/parse-string (:body response) true)]
    {:response response :parsed parsed}))

(deftest test-json-date-serialization
  (testing "java.sql.Date сериализуется как YYYY-MM-DD без сдвига"
    (let [{:keys [parsed]} (run-through-middleware
                            {:дата_приема (java.sql.Date/valueOf "2023-02-27")})]
      (is (= "2023-02-27" (:дата_приема parsed))))))

(deftest test-json-timestamp-serialization
  (testing "java.sql.Timestamp сериализуется строкой в локальном времени"
    (let [ts (java.sql.Timestamp/valueOf (java.time.LocalDateTime/of 2026 8 3 16 44 37))]
      (let [{:keys [parsed]} (run-through-middleware {:login_time ts})]
        (is (string? (:login_time parsed)))
        (is (= "2026-08-03T16:44:37" (:login_time parsed)))))))

(deftest test-json-nested-dates
  (testing "Даты во вложенных структурах нормализуются"
    (let [body {:data {:day (java.sql.Date/valueOf "2026-08-03")}
                :items [{:dt (java.sql.Date/valueOf "2020-03-15")}]
                :count 5
                :name "ok"}
          {:keys [parsed]} (run-through-middleware body)]
      (is (= "2026-08-03" (get-in parsed [:data :day])))
      (is (= "2020-03-15" (get-in parsed [:items 0 :dt])))
      (is (= 5 (:count parsed)))
      (is (= "ok" (:name parsed))))))

(deftest test-json-content-type
  (testing "Ответ получает Content-Type application/json"
    (let [{:keys [response]} (run-through-middleware {:a (java.sql.Date/valueOf "2023-02-27")})]
      (is (= "application/json; charset=utf-8" (get-in response [:headers "Content-Type"]))))))

(ns my-ring-app.config-test
  (:require [clojure.test :refer :all]
            [my-ring-app.config :refer [db-spec with-db url]]))

(deftest test-url-helper
  (testing "url helper с пустым base-url"
    (is (= "/workers" (url "/workers")))))

(deftest test-with-db-swaps-db-spec
  (testing "with-db переопределяет db-spec внутри f"
    (let [original db-spec
          fake-db {:classname "org.sqlite.JDBC" :subprotocol "sqlite" :subname ":memory:"}]
      (with-db fake-db
        (fn []
          (is (= fake-db db-spec) "db-spec должен быть fake-db внутри with-db")))
      (is (= original db-spec) "db-spec должен восстановиться после with-db"))))

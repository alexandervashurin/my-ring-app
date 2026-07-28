(ns my-ring-app.migration-test
  (:require [clojure.test :refer :all]
            [my-ring-app.migration :as migration]))

(deftest test-migration-status
  (testing "migration-status возвращает вектор с миграциями"
    (let [status (migration/migration-status)]
      (is (vector? status))
      (is (>= (count status) 1) "Должна быть хотя бы одна миграция")
      (is (contains? (first status) :version))
      (is (contains? (first status) :filename))
      (is (contains? (first status) :applied)))))

(deftest test-migration-status-applied
  (testing "Первая миграция должна быть применена"
    (let [status (migration/migration-status)
          v1 (first (filter #(clojure.string/starts-with? (:version %) "001") status))]
      (is (some? v1) "Миграция 001 должна существовать")
      (is (true? (:applied v1)) "Миграция 001 должна быть применена"))))

(deftest test-run-migrations-idempotent
  (testing "Повторный запуск миграций не падает"
    (migration/run-migrations!)
    (migration/run-migrations!)
    (let [status (migration/migration-status)
          applied (count (filter :applied status))]
      (is (= (count status) applied) "Должны быть применены все миграции"))))

(deftest test-rollback-migration
  (testing "Откат миграции работает"
    (migration/run-migrations!)
    (migration/rollback-migration!)
    (let [status (migration/migration-status)
          pending (count (filter #(not (:applied %)) status))]
      (is (>= pending 1) "Должна появиться неприменённая миграция"))
    (migration/run-migrations!)))

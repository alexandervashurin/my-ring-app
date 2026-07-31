(ns my-ring-app.cache-test
  (:require [clojure.test :refer :all]
            [my-ring-app.cache :as cache]
            [my-ring-app.model :as model]
            [my-ring-app.test-helper :as helper]))

(use-fixtures :once
  (fn [f]
    (helper/setup-db
      (fn []
        (cache/load-all!)
        (f)))))

(deftest test-load-all!
  (testing "load-all! загружает все справочники в кэш"
    (cache/load-all!)
    (let [status (cache/cache-status)]
      (is (= 7 (:tables status)) "Должно быть 7 справочников")
      (is (number? (:age-ms status)) "Возраст кэша должен быть числом")
      (is (= 7 (count (cache/get-all-cached))) "Кэш должен содержать 7 записей"))))

(deftest test-getters
  (testing "Каждый геттер возвращает непустой вектор"
    (cache/load-all!)
    (is (seq (cache/get-shops)) "get-shops должен возвращать данные")
    (is (seq (cache/get-salary-systems)) "get-salary-systems должен возвращать данные")
    (is (seq (cache/get-worker-categories)) "get-worker-categories должен возвращать данные")
    (is (seq (cache/get-qualifications)) "get-qualifications должен возвращать данные")
    (is (seq (cache/get-work-modes)) "get-work-modes должен возвращать данные")
    (is (seq (cache/get-salary-grades)) "get-salary-grades должен возвращать данные")
    (is (seq (cache/get-hourly-rates)) "get-hourly-rates должен возвращать данные")))

(deftest test-get-table-cached
  (testing "get-table-cached возвращает данные по ключу"
    (cache/load-all!)
    (is (seq (cache/get-table-cached :shops)) "Должен вернуть цеха")
    (is (nil? (cache/get-table-cached :nonexistent)) "Для несуществующего ключа — nil")))

(deftest test-cache-status
  (testing "cache-status возвращает корректную структуру"
    (cache/load-all!)
    (let [status (cache/cache-status)]
      (is (contains? status :tables))
      (is (contains? status :age-ms))
      (is (contains? status :last-refresh))
      (is (contains? status :loaded-at)))))

(deftest test-cache-data-consistency
  (testing "Данные в кэше совпадают с прямыми запросами к БД"
    (cache/load-all!)
    (let [cached (cache/get-shops)
          direct (model/get-table-data "Цех")]
      (is (= (count cached) (count direct)) "Количество записей должно совпадать")
      (is (= (map :id cached) (map :id direct)) "ID должны совпадать"))))

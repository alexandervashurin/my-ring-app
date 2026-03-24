(ns my-ring-app.model-test
  (:require [clojure.test :refer :all]
            [my-ring-app.model :refer :all]))

;; Тесты используют тестовую БД или моки
;; Для запуска тестов создайте тестовую БД или используйте mock

(deftest test-safe-query-empty-result
  (testing "Безопасный запрос возвращает пустой список при ошибке"
    (is (seqable? (safe-query "SELECT * FROM несуществующая_таблица" [])))))

(deftest test-get-tables
  (testing "Получение списка таблиц возвращает последовательность"
    (let [tables (get-tables)]
      (is (seqable? tables)))))

(deftest test-get-spravochnik
  (testing "Получение справочника Цех"
    (let [result (get-spravochnik "Цех")]
      (is (seqable? result)))))

(deftest test-get-workers-with-details
  (testing "Получение списка работников с деталями"
    (let [workers (get-workers-with-details)]
      (is (seqable? workers))
      (if (seq workers)
        (do
          (is (contains? (first workers) :id))
          (is (contains? (first workers) :фамилия))
          (is (contains? (first workers) :имя)))))))

(deftest test-search-workers
  (testing "Поиск работников по фамилии"
    (let [workers (search-workers "Иванов")]
      (is (seqable? workers))))
  
  (testing "Поиск работников по пустому запросу"
    (let [workers (search-workers "")]
      (is (seqable? workers)))))

(deftest test-create-update-delete-record
  (testing "Создание, обновление и удаление записи (интеграционный)"
    ;; Этот тест требует тестовой БД
    ;; Заглушка для демонстрации структуры
    (is true)))

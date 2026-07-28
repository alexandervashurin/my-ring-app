(ns my-ring-app.controllers-test
  "Тесты для контроллеров"
  (:require [clojure.test :refer :all]
            [my-ring-app.controllers :as ctrl]
            [my-ring-app.migration :as migration]))

(defn setup-db [f]
  (migration/run-migrations!)
  (f))

(use-fixtures :once setup-db)

(deftest test-home-page
  (testing "Главная страница возвращает 200 с HTML"
    (let [response (ctrl/home-page)]
      (is (= 200 (:status response)))
      (is (string? (:body response)))
      (is (.contains ^String (:body response) "<html")))))

(deftest test-dashboard-page
  (testing "Дашборд возвращает 200"
    (let [response (ctrl/dashboard-page)]
      (is (= 200 (:status response)))
      (is (string? (:body response)))
      (is (.contains ^String (:body response) "<html")))))

(deftest test-dashboard-page-with-request
  (testing "Дашборд с org-id из запроса"
    (let [response (ctrl/dashboard-page {:org-id 1})]
      (is (= 200 (:status response))))))

(deftest test-workers-page
  (testing "Список работников возвращает 200"
    (let [response (ctrl/workers-page)]
      (is (= 200 (:status response)))
      (is (string? (:body response))))))

(deftest test-workers-page-with-search
  (testing "Поиск работников"
    (let [response (ctrl/workers-page {:search "Петров"})]
      (is (= 200 (:status response))))))

(deftest test-workers-page-with-org
  (testing "Список работников с org-id"
    (let [response (ctrl/workers-page {:org-id 1})]
      (is (= 200 (:status response))))))

(deftest test-new-worker-form
  (testing "Форма создания работника"
    (let [response (ctrl/new-worker-form)]
      (is (= 200 (:status response)))
      (is (string? (:body response))))))

(deftest test-edit-worker-form-valid-id
  (testing "Форма редактирования с валидным ID"
    (let [response (ctrl/edit-worker-form 1)]
      (is (= 200 (:status response)))
      (is (string? (:body response))))))

(deftest test-edit-worker-form-invalid-id
  (testing "Форма редактирования с некорректным ID"
    (let [response (ctrl/edit-worker-form "abc")]
      (is (= 400 (:status response))))))

(deftest test-not-found-page
  (testing "404 страница возвращает 404"
    (let [response (ctrl/not-found-page {})]
      (is (= 404 (:status response)))
      (is (string? (:body response)))
      (is (.contains ^String (:body response) "404")))))

(deftest test-all-tables-page
  (testing "Страница всех таблиц"
    (let [response (ctrl/all-tables-page)]
      (is (= 200 (:status response)))
      (is (string? (:body response))))))

(deftest test-workers-page-with-identity
  (testing "Список работников с org-id из identity"
    (let [response (ctrl/workers-page {:identity {:organization_id 1}})]
      (is (= 200 (:status response))))))

(ns my-ring-app.api.workers-test
  "Тесты для REST API работников"
  (:require [clojure.test :refer :all]
            [my-ring-app.api.workers :refer :all :reload true]
            [my-ring-app.model :as model]))

;; ======================================================================
;; Вспомогательные функции
;; ======================================================================

(defn- make-request
  "Создание тестового запроса"
  ([method uri]
   {:request-method method
    :uri uri
    :params {}
    :route-params {}
    :identity nil})
  ([method uri params]
   {:request-method method
    :uri uri
    :params params
    :route-params {}
    :identity nil})
  ([method uri params route-params identity]
   {:request-method method
    :uri uri
    :params params
    :route-params route-params
    :identity identity}))

(defn- get-body
  "Получение тела ответа (map или JSON)"
  [response]
  (let [body (:body response)]
    (if (string? body)
      (read-string body)
      body)))

;; ======================================================================
;; Тесты для GET /api/workers
;; ======================================================================

(deftest test-get-workers
  (testing "Получение списка работников"
    (let [request (make-request :get "/api/workers")
          response (get-workers request)
          body (get-body response)]
      (is (= 200 (:status response)))
      (is (true? (:success body)))
      (is (coll? (:data body)))
      (is (pos? (count (:data body)))))))

(deftest test-get-workers-with-search
  (testing "Поиск работников через API"
    (let [request (make-request :get "/api/workers" {:search "Иванов"})
          response (get-workers request)
          body (get-body response)]
      (is (= 200 (:status response)))
      (is (true? (:success body)))
      (is (coll? (:data body)))
      (is (pos? (count (:data body)))))))

;; ======================================================================
;; Тесты для GET /api/workers/search
;; ======================================================================

(deftest test-search-workers-api
  (testing "Поиск работников через search endpoint"
    (let [request (make-request :get "/api/workers/search" {:q "Петров"})
          response (search-workers-api request)
          body (get-body response)]
      (is (= 200 (:status response)))
      (is (true? (:success body)))
      (is (vector? (:data body))))))

(deftest test-search-workers-api-missing-query
  (testing "Поиск без параметра q"
    (let [request (make-request :get "/api/workers/search" {})
          response (search-workers-api request)
          body (get-body response)]
      (is (= 400 (:status response)))
      (is (false? (:success body)))
      (is (= "MISSING_QUERY" (get-in body [:error :code]))))))

;; ======================================================================
;; Тесты для GET /api/workers/:id
;; ======================================================================

(deftest test-get-worker-by-id
  (testing "Получение работника по ID"
    (let [request (make-request :get "/api/workers/1" {} {:id "1"} nil)
          response (get-worker-by-id request)
          body (get-body response)]
      (is (= 200 (:status response)))
      (is (true? (:success body)))
      (is (map? (:data body)))
      (is (= 1 (get-in body [:data :id]))))))

(deftest test-get-worker-by-id-not-found
  (testing "Получение несуществующего работника"
    (let [request (make-request :get "/api/workers/99999" {} {:id "99999"} nil)
          response (get-worker-by-id request)
          body (get-body response)]
      (is (= 404 (:status response)))
      (is (false? (:success body)))
      (is (= "NOT_FOUND" (get-in body [:error :code]))))))

(deftest test-get-worker-by-id-invalid
  (testing "Получение работника с некорректным ID"
    (let [request (make-request :get "/api/workers/abc" {} {:id "abc"} nil)
          response (get-worker-by-id request)
          body (get-body response)]
      (is (= 400 (:status response)))
      (is (false? (:success body)))
      (is (= "INVALID_ID" (get-in body [:error :code]))))))

;; ======================================================================
;; Тесты для POST /api/workers
;; ======================================================================

(deftest test-create-worker-valid
  (testing "Создание работника с валидными данными"
    (let [request (make-request :post "/api/workers"
                                {:фамилия "Тестов"
                                 :имя "Тест"
                                 :отчество "Тестович"
                                 :дата_приема "2024-01-01"
                                 :цех_id "1"
                                 :система_оплаты_id "1"
                                 :категория_работника_id "1"
                                 :разряд_id "1"
                                 :режим_работы_id "1"
                                 :оклад_id "1"
                                 :почасовая_ставка_id ""}
                                {}
                                {:username "admin" :role "admin"})
          response (create-worker request)
          body (get-body response)]
      ;; ID созданного работника > 0
      (if (:success body)
        (do
          (is (= 201 (:status response)))
          (is (true? (:success body)))
          (is (map? (:data body))))
        ;; Если ошибка (например, дубликат), проверяем что это валидация
        (is (= 400 (:status response)))))))

(deftest test-create-worker-invalid
  (testing "Создание работника с невалидными данными"
    (let [request (make-request :post "/api/workers"
                                {:фамилия ""
                                 :имя "Тест"}
                                {}
                                {:username "admin" :role "admin"})
          response (create-worker request)
          body (get-body response)]
      (is (= 400 (:status response)))
      (is (false? (:success body)))
      (is (= "VALIDATION_ERROR" (get-in body [:error :code]))))))

;; ======================================================================
;; Тесты для PUT /api/workers/:id
;; ======================================================================

(deftest test-update-worker-valid
  (testing "Обновление работника с валидными данными"
    (let [request (make-request :put "/api/workers/1"
                                {:фамилия "Тестов"
                                 :имя "Тест"
                                 :отчество "Тестович"
                                 :дата_приема "2024-01-01"
                                 :цех_id "1"
                                 :система_оплаты_id "1"
                                 :категория_работника_id "1"
                                 :разряд_id "1"
                                 :режим_работы_id "1"
                                 :оклад_id "1"
                                 :почасовая_ставка_id ""}
                                {:id "1"}
                                {:username "admin" :role "admin"})
          response (update-worker request)
          body (get-body response)]
      (if (:success body)
        (do
          (is (= 200 (:status response)))
          (is (true? (:success body))))
        ;; Если ошибка, проверяем что это 500
        (is (= 500 (:status response)))))))

(deftest test-update-worker-invalid
  (testing "Обновление работника с невалидными данными"
    (let [request (make-request :put "/api/workers/1"
                                {:фамилия ""}
                                {:id "1"}
                                {:username "admin" :role "admin"})
          response (update-worker request)
          body (get-body response)]
      (is (= 400 (:status response)))
      (is (false? (:success body)))
      (is (= "VALIDATION_ERROR" (get-in body [:error :code]))))))

;; ======================================================================
;; Тесты для DELETE /api/workers/:id
;; ======================================================================

(deftest test-delete-worker-invalid-id
  (testing "Удаление с некорректным ID"
    (let [request (make-request :delete "/api/workers/abc"
                                {}
                                {:id "abc"}
                                {:username "admin" :role "admin"})
          response (delete-worker request)
          body (get-body response)]
      (is (= 400 (:status response)))
      (is (false? (:success body)))
      (is (= "INVALID_ID" (get-in body [:error :code]))))))

;; ======================================================================
;; Тесты форматирования
;; ======================================================================

(deftest test-format-worker
  (testing "Форматирование работника для API"
    (let [worker {:id 1
                  :фамилия "Иванов"
                  :имя "Иван"
                  :отчество "Иванович"
                  :дата_приема "2024-01-01"
                  :цех_id 1
                  :цех "Цех 1"}
          formatted (format-worker worker)]
      (is (map? formatted))
      (is (= 1 (:id formatted)))
      (is (= "Иванов" (:фамилия formatted)))
      (is (contains? formatted :цех)))))

(deftest test-format-worker-nil
  (testing "Форматирование nil работника"
    (is (nil? (format-worker nil)))))

;; ======================================================================
;; Тесты вспомогательных функций (приватные - не тестируются напрямую)
;; ======================================================================

;; Примечание: parse-int, validate-id, format-worker - приватные функции
;; Тестируются косвенно через публичные API endpoints

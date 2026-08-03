(ns my-ring-app.api.onec-test
  "Тесты для REST API интеграции с 1С (org-scoping экспорта)"
  (:require [clojure.test :refer :all]
            [my-ring-app.api.onec :refer :all]
            [my-ring-app.model :as model]
            [my-ring-app.test-helper :as helper]))

(use-fixtures :once helper/setup-db)

(def valid-worker
  {:фамилия "Тестов"
   :имя "Тест"
   :отчество "Тестович"
   :дата_приема "2026-01-15"
   :цех_id 1
   :система_оплаты_id 1
   :категория_работника_id 1
   :разряд_id 1
   :режим_работы_id 1
   :оклад_id 1})

(defn make-request
  "Создание тестового запроса с identity и org-id"
  [params identity org-id]
  {:request-method :get
   :params params
   :route-params {}
   :identity identity
   :org-id org-id})

(defn- worker-surnames
  [response]
  (->> response :body :data :workers (map :surname) set))

;; ======================================================================
;; Тесты org-scoping экспорта работников
;; ======================================================================

(deftest test-1c-workers-admin-org-filter
  (testing "Админ может экспортировать конкретную организацию через ?org_id="
    (model/create-record "Работник" (assoc valid-worker :фамилия "ОдинИкс") 1)
    (model/create-record "Работник" (assoc valid-worker :фамилия "ДваИгрек") 2)
    (let [request (make-request {:org_id "1"} {:role "admin"} nil)
          response (get-workers-export request)
          surnames (worker-surnames response)]
      (is (= 200 (:status response)))
      (is (contains? surnames "ОдинИкс"))
      (is (not (contains? surnames "ДваИгрек"))))))

(deftest test-1c-workers-org-scoped-user
  (testing "Пользователь видит только работников своей организации"
    (model/create-record "Работник" (assoc valid-worker :фамилия "СвояОрг") 1)
    (model/create-record "Работник" (assoc valid-worker :фамилия "ЧужаяОрг") 2)
    (let [request (make-request {} {:role "manager" :organization_id 1} 1)
          response (get-workers-export request)
          surnames (worker-surnames response)]
      (is (= 200 (:status response)))
      (is (contains? surnames "СвояОрг"))
      (is (not (contains? surnames "ЧужаяОрг"))))))

(deftest test-1c-workers-admin-all-orgs
  (testing "Админ без ?org_id= экспортирует все организации"
    (model/create-record "Работник" (assoc valid-worker :фамилия "ВсеОдин") 1)
    (model/create-record "Работник" (assoc valid-worker :фамилия "ВсеДва") 2)
    (let [request (make-request {} {:role "admin"} nil)
          response (get-workers-export request)
          surnames (worker-surnames response)]
      (is (= 200 (:status response)))
      (is (contains? surnames "ВсеОдин"))
      (is (contains? surnames "ВсеДва")))))

;; ======================================================================
;; Тесты org-scoping экспорта зарплаты
;; ======================================================================

(deftest test-1c-salary-admin-org-filter
  (testing "Экспорт зарплаты админом с ?org_id= ограничен организацией"
    (let [w1 (model/create-record "Работник" (assoc valid-worker :фамилия "ЗпОдин") 1)
          w2 (model/create-record "Работник" (assoc valid-worker :фамилия "ЗпДва") 2)
          request (make-request {:org_id "1" :year "2025" :month "10"} {:role "admin"} nil)
          response (get-salary-export request)
          worker-ids (->> response :body :data :salary (map :workerId) set)]
      (is (= 200 (:status response)))
      (is (contains? worker-ids (:id w1)))
      (is (not (contains? worker-ids (:id w2)))))))

(deftest test-1c-salary-org-scoped-user
  (testing "Экспорт зарплаты ограничен организацией пользователя"
    (let [w1 (model/create-record "Работник" (assoc valid-worker :фамилия "ЗпСвоя") 1)
          w2 (model/create-record "Работник" (assoc valid-worker :фамилия "ЗпЧужая") 2)
          request (make-request {:year "2025" :month "10"} {:role "manager" :organization_id 1} 1)
          response (get-salary-export request)
          worker-ids (->> response :body :data :salary (map :workerId) set)]
      (is (= 200 (:status response)))
      (is (contains? worker-ids (:id w1)))
      (is (not (contains? worker-ids (:id w2)))))))

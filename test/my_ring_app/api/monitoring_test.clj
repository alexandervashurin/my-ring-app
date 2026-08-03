(ns my-ring-app.api.monitoring-test
  "Тесты для REST API мониторинга (org-scoping статистики)"
  (:require [clojure.test :refer :all]
            [my-ring-app.api.monitoring :refer :all]
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
  "Создание тестового запроса с org-id"
  [org-id]
  {:request-method :get
   :params {}
   :route-params {}
   :org-id org-id})

;; ======================================================================
;; Тесты org-scoping /api/stats
;; ======================================================================

(deftest test-app-statistics-org-scoped
  (testing "Статистика ограничена организацией пользователя"
    (model/create-record "Работник" (assoc valid-worker :фамилия "СтатОдин") 1)
    (model/create-record "Работник" (assoc valid-worker :фамилия "СтатДва") 2)
    (let [org1-count (get-in (app-statistics (make-request 1)) [:body :business :total-workers])
          org2-count (get-in (app-statistics (make-request 2)) [:body :business :total-workers])]
      (is (pos? org1-count))
      (is (pos? org2-count))
      (is (not= org1-count org2-count)))))

(deftest test-app-statistics-org-consistency
  (testing "total-workers в статистике совпадает с COUNT по организации"
    (let [expected (model/count-workers 1)
          actual (get-in (app-statistics (make-request 1)) [:body :business :total-workers])]
      (is (= expected actual)))))

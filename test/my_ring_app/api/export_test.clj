(ns my-ring-app.api.export-test
  "Тесты для REST API экспорта данных"
  (:require [clojure.test :refer :all]
            [my-ring-app.api.export :refer :all]
            [my-ring-app.views.layout :refer [html-escape]]
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
  "Создание тестового запроса с identity"
  ([method uri] (make-request method uri {} nil))
  ([method uri params identity]
   {:request-method method
    :uri uri
    :params params
    :route-params {}
    :identity identity}))

(defn create-test-worker-with-salary!
  "Создание работника с учётом времени и начислением зарплаты в заданной организации"
  [org-id фамилия]
  (let [worker (model/create-record "Работник" (assoc valid-worker :фамилия фамилия :имя "Икс") org-id)
        wt (model/create-record "Учет_рабочего_времени"
                                {:работник_id (:id worker)
                                 :год 2026
                                 :месяц 6
                                 :всего_часов_за_месяц_по_плану 160
                                 :всего_часов_в_месяц_по_факту 160
                                 :количество_отработанных_дней 20
                                 :количество_рабочих_часов_в_день 8
                                 :всего_отработанных_часов 160
                                 :сколько_должны_отработать 160
                                 :больничные_дни 0
                                 :командировочные_дни 0}
                                org-id)
        _ (model/create-record "Начисление_заработной_платы"
                               {:учет_рабочего_времени_id (:id wt)
                                :год 2026
                                :месяц 6
                                :зарплата_за_больничные_дни 0
                                :зарплата_за_командировочные_дни 0
                                :общая_зарплата 50000}
                               org-id)]
    worker))

;; ======================================================================
;; Тесты для CSV экспорта
;; ======================================================================

(deftest test-export-workers-csv
  (testing "Экспорт работников в CSV"
    (let [request (make-request :get "/api/export/workers.csv")
          response (export-workers-csv request)]
      (is (= 200 (:status response)))
      (is (= "text/csv; charset=utf-8" (get-in response [:headers "Content-Type"])))
      (is (re-find #"workers_.*\.csv" (get-in response [:headers "Content-Disposition"])))
      (is (string? (:body response)))
      (is (re-find #"ID,Фамилия,Имя" (:body response))))))

(deftest test-export-salary-csv
  (testing "Экспорт зарплаты в CSV"
    (let [request (make-request :get "/api/export/salary.csv")
          response (export-salary-csv request)]
      (is (= 200 (:status response)))
      (is (= "text/csv; charset=utf-8" (get-in response [:headers "Content-Type"])))
      (is (re-find #"salary_.*\.csv" (get-in response [:headers "Content-Disposition"])))
      (is (string? (:body response))))))

;; ======================================================================
;; Тесты org-scoping экспорта
;; ======================================================================

(deftest test-export-workers-csv-org-scoped
  (testing "Пользователь видит только работников своей организации"
    (model/create-record "Работник" (assoc valid-worker :фамилия "ОргОдин" :имя "Икс") 1)
    (model/create-record "Работник" (assoc valid-worker :фамилия "ОргДва" :имя "Игрек") 2)
    (let [request (make-request :get "/api/export/workers.csv"
                                {}
                                {:role "manager" :organization_id 1})
          response (export-workers-csv request)]
      (is (= 200 (:status response)))
      (is (re-find #"ОргОдин" (:body response)))
      (is (not (re-find #"ОргДва" (:body response)))))))

(deftest test-export-workers-csv-admin-org-filter
  (testing "Админ может экспортировать конкретную организацию через ?org_id="
    (model/create-record "Работник" (assoc valid-worker :фамилия "АдминОдин" :имя "Икс") 1)
    (model/create-record "Работник" (assoc valid-worker :фамилия "АдминДва" :имя "Игрек") 2)
    (let [request (make-request :get "/api/export/workers.csv"
                                {:org_id "1"}
                                {:role "admin"})
          response (export-workers-csv request)]
      (is (= 200 (:status response)))
      (is (re-find #"АдминОдин" (:body response)))
      (is (not (re-find #"АдминДва" (:body response)))))))

(deftest test-export-workers-csv-admin-all-orgs
  (testing "Админ без ?org_id= экспортирует все организации"
    (model/create-record "Работник" (assoc valid-worker :фамилия "АллОдин" :имя "Икс") 1)
    (model/create-record "Работник" (assoc valid-worker :фамилия "АллДва" :имя "Игрек") 2)
    (let [request (make-request :get "/api/export/workers.csv"
                                {}
                                {:role "admin"})
          response (export-workers-csv request)]
      (is (= 200 (:status response)))
      (is (re-find #"АллОдин" (:body response)))
      (is (re-find #"АллДва" (:body response))))))

(deftest test-export-salary-csv-org-scoped
  (testing "Экспорт зарплаты ограничен организацией пользователя"
    (create-test-worker-with-salary! 1 "ЗарплатОдин")
    (create-test-worker-with-salary! 2 "ЗарплатДва")
    (let [request (make-request :get "/api/export/salary.csv"
                                {}
                                {:role "manager" :organization_id 1})
          response (export-salary-csv request)]
      (is (= 200 (:status response)))
      (is (re-find #"ЗарплатОдин Икс" (:body response)))
      (is (not (re-find #"ЗарплатДва" (:body response)))))))

;; ======================================================================
;; Тесты для Excel экспорта
;; ======================================================================

(deftest test-export-workers-excel
  (testing "Экспорт работников в Excel"
    (let [request (make-request :get "/api/export/workers.xlsx")
          response (export-workers-excel request)]
      (is (= 200 (:status response)))
      (is (= "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
             (get-in response [:headers "Content-Type"])))
      (is (re-find #"workers_.*\.xlsx" (get-in response [:headers "Content-Disposition"])))
      (is (bytes? (:body response)))
      (is (pos? (count (:body response)))))))

(deftest test-export-salary-excel
  (testing "Экспорт зарплаты в Excel"
    (let [request (make-request :get "/api/export/salary.xlsx")
          response (export-salary-excel request)]
      (is (= 200 (:status response)))
      (is (= "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
             (get-in response [:headers "Content-Type"])))
      (is (re-find #"salary_.*\.xlsx" (get-in response [:headers "Content-Disposition"])))
      (is (bytes? (:body response)))
      (is (pos? (count (:body response)))))))

(deftest test-html-escape
  (testing "Экранирование HTML"
    (is (= "" (html-escape nil)))
    (is (= "test" (html-escape "test")))
    (is (= "&amp;" (html-escape "&")))
    (is (= "&lt;" (html-escape "<")))
    (is (= "&gt;" (html-escape ">")))))

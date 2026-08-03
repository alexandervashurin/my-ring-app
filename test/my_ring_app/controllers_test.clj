(ns my-ring-app.controllers-test
  (:require [clojure.test :refer :all]
            [clojure.set :as set]
            [my-ring-app.controllers :as ctrl]
            [my-ring-app.model :as model]
            [my-ring-app.tariff :as tariff]
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
  "Создание тестового запроса с params и org-id"
  [params org-id]
  {:params params
   :org-id org-id
   :identity {:organization_id org-id}})

(defn create-test-worker!
  ([] (create-test-worker! 1))
  ([org-id]
   (let [data (assoc valid-worker :цех_id 1 :система_оплаты_id 1 :категория_работника_id 1 :разряд_id 1 :режим_работы_id 1 :оклад_id 1)]
     (model/create-record "Работник" data org-id))))

(defn- assert-redirect [response expected-status]
  (is (= 302 (:status response)) (str "Должен быть редирект, получен: " (:status response)))
  (when (= 302 (:status response))
    (is (string? (get-in response [:headers "Location"])) "Должен быть Location заголовок")))

(defn- assert-error-page [response]
  (is (some #{(:status response)} [200 400]) (str "Статус: " (:status response)))
  (is (string? (:body response)) "Тело ответа должно быть строкой"))

;; ======================================================================
;; Главная страница
;; ======================================================================

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

(deftest test-dashboard-real-trends
  (testing "Дашборд не содержит фейковых трендов, а показывает реальные"
    (let [response (ctrl/dashboard-page {:org-id 1})]
      (is (= 200 (:status response)))
      (is (not (re-find #"\+2\.5%" (:body response))))
      (is (not (re-find #"Стабильно" (:body response))))
      (is (not (re-find #"Все активны" (:body response))))
      (is (re-find #"нет данных за прошлый месяц" (:body response))))))

;; ======================================================================
;; Работники — страницы
;; ======================================================================

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

;; ======================================================================
;; CREATE worker
;; ======================================================================

(deftest test-create-worker-success
  (testing "Создание работника с валидными данными"
    (let [result (create-test-worker! 1)]
      (is (:success result))
      (is (pos? (:id result))))))

(deftest test-create-worker-via-controller
  (testing "Контроллер create-worker возвращает редирект"
    (tariff/update-org-plan! 2 3 nil)
    (let [request (make-request valid-worker 2)
          response (ctrl/create-worker request)]
      (assert-redirect response 302))))

(deftest test-create-worker-invalid-data
  (testing "Создание с некорректными данными — страница с ошибкой"
    (let [response (ctrl/create-worker (make-request {:имя "Тест"} 1))]
      (assert-error-page response)
      (when (= 200 (:status response))
        (is (.contains ^String (:body response) "error") "Должен содержать сообщение об ошибке")))))

(deftest test-create-worker-empty
  (testing "Создание с пустыми данными — страница с ошибкой"
    (let [response (ctrl/create-worker (make-request {} 1))]
      (assert-error-page response))))

(deftest test-create-worker-nil
  (testing "Создание с nil — страница с ошибкой"
    (let [response (ctrl/create-worker (make-request nil 1))]
      (assert-error-page response))))

;; ======================================================================
;; UPDATE worker
;; ======================================================================

(deftest test-update-worker-success
  (testing "Обновление работника с валидными данными"
    (let [created (create-test-worker!)
          worker-id (:id created)
          request (make-request (assoc valid-worker :фамилия "Обновлен") 1)
          response (ctrl/update-worker worker-id request)]
      (assert-redirect response 302)
      (let [updated (model/get-record-by-id "Работник" (str worker-id))]
        (when updated
          (is (= "Обновлен" (:фамилия updated))))))))

(deftest test-update-worker-invalid-id
  (testing "Обновление с некорректным ID"
    (let [response (ctrl/update-worker "abc" (make-request valid-worker 1))]
      (is (= 400 (:status response))))))

(deftest test-update-worker-invalid-data
  (testing "Обновление с некорректными данными — страница с ошибкой"
    (let [created (create-test-worker!)
          request (make-request {:фамилия "" :имя ""} 1)
          response (ctrl/update-worker (:id created) request)]
      (assert-error-page response))))

;; ======================================================================
;; DELETE worker
;; ======================================================================

(deftest test-delete-worker-success
  (testing "Удаление работника"
    (let [created (create-test-worker!)
          worker-id (:id created)
          response (ctrl/delete-worker worker-id)]
      (assert-redirect response 302)
      (let [deleted (model/get-record-by-id "Работник" (str worker-id))]
        (is (nil? deleted) "Работник должен быть удалён")))))

(deftest test-delete-worker-invalid-id
  (testing "Удаление с некорректным ID"
    (let [response (ctrl/delete-worker "abc")]
      (is (= 400 (:status response))))))

;; ======================================================================
;; Страница зарплаты
;; ======================================================================

(deftest test-worker-salary-page-valid-id
  (testing "Страница зарплаты с валидным ID"
    (let [response (ctrl/worker-salary-page 1)]
      (is (some #{(:status response)} [200 302]) (str "Статус: " (:status response))))))

(deftest test-worker-salary-page-invalid-id
  (testing "Страница зарплаты с некорректным ID"
    (let [response (ctrl/worker-salary-page "abc")]
      (is (= 400 (:status response))))))

;; ======================================================================
;; Страница учета времени
;; ======================================================================

(deftest test-worker-work-time-page-valid-id
  (testing "Страница учета времени с валидным ID"
    (let [response (ctrl/worker-work-time-page 1)]
      (is (some #{(:status response)} [200 302]) (str "Статус: " (:status response))))))

(deftest test-worker-work-time-page-invalid-id
  (testing "Страница учета времени с некорректным ID"
    (let [response (ctrl/worker-work-time-page "abc")]
      (is (= 400 (:status response))))))

;; ======================================================================
;; Форма редактирования учета времени
;; ======================================================================

(deftest test-edit-work-time-form-valid-id
  (testing "Форма редактирования учета времени с валидным ID"
    (let [response (ctrl/edit-work-time-form 1)]
      (is (some #{(:status response)} [200 302]) (str "Статус: " (:status response))))))

(deftest test-edit-work-time-form-invalid-id
  (testing "Форма редактирования учета времени с некорректным ID"
    (let [response (ctrl/edit-work-time-form "abc")]
      (is (= 400 (:status response))))))

;; ======================================================================
;; UPDATE work-time
;; ======================================================================

(deftest test-update-work-time-invalid-id
  (testing "Обновление учета времени с некорректным ID"
    (let [response (ctrl/update-work-time "abc" {})]
      (is (= 400 (:status response))))))

;; ======================================================================
;; 404
;; ======================================================================

(deftest test-not-found-page
  (testing "404 страница"
    (let [response (ctrl/not-found-page {})]
      (is (= 404 (:status response)))
      (is (string? (:body response)))
      (is (.contains ^String (:body response) "404")))))

;; ======================================================================
;; Все таблицы
;; ======================================================================

(deftest test-all-tables-page
  (testing "Страница всех таблиц"
    (let [response (ctrl/all-tables-page)]
      (is (= 200 (:status response)))
      (is (string? (:body response))))))

(deftest test-workers-page-with-identity
  (testing "Список работников с org-id из identity"
    (let [response (ctrl/workers-page {:identity {:organization_id 1}})]
      (is (= 200 (:status response))))))

;; ======================================================================
;; Пагинация
;; ======================================================================

(deftest test-workers-page-pagination
  (testing "HTML-страница работников принимает page и per_page"
    (let [response (ctrl/workers-page {:page "2" :per_page "2"})]
      (is (= 200 (:status response)))
      (is (string? (:body response)))))
  (testing "Некорректные параметры пагинации не приводят к ошибке"
    (let [response (ctrl/workers-page {:page "abc" :per_page "-1"})]
      (is (= 200 (:status response)))))
  (testing "На странице рендерятся ссылки пагинации"
    (let [body (:body (ctrl/workers-page {:per_page "2"}))]
      (is (.contains ^String body "pagination"))
      (is (.contains ^String body "page-link"))))
  (testing "Поиск сохраняется между страницами"
    (doseq [i (range 3)]
      (model/create-record "Работник" (assoc valid-worker :фамилия (str "Пагин" i)) 1))
    (let [body (:body (ctrl/workers-page {:per_page "2" :search "Пагин"}))]
      (is (.contains ^String body "search=Пагин")))))

(deftest test-model-get-workers-page
  (testing "get-workers-page возвращает items и total"
    (let [result (model/get-workers-page nil 1 5)
          items (:items result)
          total (:total result)]
      (is (vector? items))
      (is (integer? total))
      (is (= total (count (model/get-workers-with-details))))
      (is (<= (count items) 5))))
  (testing "get-workers-page с поиском"
    (let [result (model/get-workers-page nil 1 5 "Иванов")]
      (is (vector? (:items result)))
      (is (integer? (:total result)))))
  (testing "get-workers-page по организации"
    (let [result (model/get-workers-page 1 1 5)]
      (is (vector? (:items result)))
      (is (integer? (:total result)))))
  (testing "Пустая БД — 0 работников"
    (let [result (model/get-workers-page 999 1 5)]
      (is (empty? (:items result)))
      (is (= 0 (:total result))))))

(deftest test-model-count-workers
  (testing "Подсчёт работников"
    (is (nat-int? (model/count-workers nil)))
    (is (nat-int? (model/count-workers 1)))
    (is (nat-int? (model/count-workers nil "Иванов")))))

(deftest test-model-pagination-pages-distinct
  (testing "Разные страницы не пересекаются"
    (let [total (model/count-workers nil)
          per-page 2]
      (when (> total 2)
        (let [p1 (model/get-workers-page nil 1 per-page)
              p2 (model/get-workers-page nil 2 per-page)
              ids1 (set (map :id (:items p1)))
              ids2 (set (map :id (:items p2)))]
          (is (empty? (set/intersection ids1 ids2))))))))

;; ======================================================================
;; Ограничение строк в get-table-data
;; ======================================================================

(deftest test-get-table-data-limit
  (testing "Лимит строк в get-table-data"
    (let [rows (model/get-table-data "Работник" 1)]
      (is (<= (count rows) 1)))))

(deftest test-count-table-rows
  (testing "Подсчёт записей в таблице"
    (is (nat-int? (model/count-table-rows "Работник")))
    (is (nat-int? (model/count-table-rows "Цех")))))

(deftest test-all-tables-page-with-count
  (testing "Страница всех таблиц показывает счётчик"
    (let [response (ctrl/all-tables-page)]
      (is (= 200 (:status response)))
      (is (string? (:body response)))
      (is (.contains ^String (:body response) "записей")))))

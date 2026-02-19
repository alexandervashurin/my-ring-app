(ns my-ring-app.core-test
  (:require [clojure.test :refer :all]
            [my-ring-app.validation :refer :all]
            [my-ring-app.model :as model]))

(deftest test-worker-validation-comprehensive
  (testing "Комплексная проверка валидации работника"
    ;; Валидные данные
    (is (:valid? (validate-worker {:фамилия "Иванов"
                                   :имя "Иван"
                                   :дата_приема (str (java.time.LocalDate/now))
                                   :цех_id "1"
                                   :система_оплаты_id "1"
                                   :категория_работника_id "1"
                                   :разряд_id "1"
                                   :режим_работы_id "1"
                                   :оклад_id "1"})))
    
    ;; Пустые данные
    (is (not (:valid? (validate-worker {}))))))

(deftest test-model-functions
  (testing "Функции модели работают корректно"
    ;; Получение таблиц
    (is (seqable? (model/get-tables)))
    
    ;; Получение справочников
    (is (seqable? (model/get-spravochnik "Цех")))
    
    ;; Получение работников
    (is (seqable? (model/get-workers-with-details)))))

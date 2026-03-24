(ns my-ring-app.validation-test
  (:require [clojure.test :refer :all]
            [my-ring-app.validation :refer :all]))

(deftest test-validate-worker-valid
  (testing "Валидные данные работника"
    (is (:valid? (validate-worker {:фамилия "Иванов"
                                   :имя "Иван"
                                   :отчество "Иванович"
                                   :дата_приема "2024-01-01"
                                   :цех_id "1"
                                   :система_оплаты_id "1"
                                   :категория_работника_id "1"
                                   :разряд_id "1"
                                   :режим_работы_id "1"
                                   :оклад_id "1"
                                   :почасовая_ставка_id ""}))))
  
  (testing "Валидные данные без отчества"
    (is (:valid? (validate-worker {:фамилия "Петров"
                                   :имя "Петр"
                                   :отчество ""
                                   :дата_приема "2024-01-01"
                                   :цех_id "1"
                                   :система_оплаты_id "1"
                                   :категория_работника_id "1"
                                   :разряд_id "1"
                                   :режим_работы_id "1"
                                   :оклад_id "1"
                                   :почасовая_ставка_id ""})))))

(deftest test-validate-worker-missing-required
  (testing "Отсутствие фамилии"
    (let [result (validate-worker {:имя "Иван"
                                   :дата_приема "2024-01-01"
                                   :цех_id "1"
                                   :система_оплаты_id "1"
                                   :категория_работника_id "1"
                                   :разряд_id "1"
                                   :режим_работы_id "1"})]
      (is (not (:valid? result)))
      (is (some #(re-find #"Фамилия обязательна" %) (:errors result)))))
  
  (testing "Отсутствие имени"
    (let [result (validate-worker {:фамилия "Иванов"
                                   :дата_приема "2024-01-01"
                                   :цех_id "1"
                                   :система_оплаты_id "1"
                                   :категория_работника_id "1"
                                   :разряд_id "1"
                                   :режим_работы_id "1"})]
      (is (not (:valid? result)))
      (is (some #(re-find #"Имя обязательно" %) (:errors result)))))
  
  (testing "Отсутствие даты приема"
    (let [result (validate-worker {:фамилия "Иванов"
                                   :имя "Иван"
                                   :цех_id "1"
                                   :система_оплаты_id "1"
                                   :категория_работника_id "1"
                                   :разряд_id "1"
                                   :режим_работы_id "1"})]
      (is (not (:valid? result)))
      (is (some #(re-find #"Дата приема обязательна" %) (:errors result))))))

(deftest test-validate-worker-date-format
  (testing "Неверный формат даты"
    (let [result (validate-worker {:фамилия "Иванов"
                                   :имя "Иван"
                                   :дата_приема "01-01-2024"
                                   :цех_id "1"
                                   :система_оплаты_id "1"
                                   :категория_работника_id "1"
                                   :разряд_id "1"
                                   :режим_работы_id "1"})]
      (is (not (:valid? result)))
      (is (some #(re-find #"Неверный формат даты" %) (:errors result)))))
  
  (testing "Дата в будущем"
    (let [result (validate-worker {:фамилия "Иванов"
                                   :имя "Иван"
                                   :дата_приема "2099-01-01"
                                   :цех_id "1"
                                   :система_оплаты_id "1"
                                   :категория_работника_id "1"
                                   :разряд_id "1"
                                   :режим_работы_id "1"})]
      (is (not (:valid? result)))
      (is (some #(re-find #"не может быть в будущем" %) (:errors result))))))

(deftest test-validate-worker-length-limits
  (testing "Фамилия слишком длинная"
    (let [result (validate-worker {:фамилия (apply str (repeat 51 "а"))
                                   :имя "Иван"
                                   :дата_приема "2024-01-01"
                                   :цех_id "1"
                                   :система_оплаты_id "1"
                                   :категория_работника_id "1"
                                   :разряд_id "1"
                                   :режим_работы_id "1"})]
      (is (not (:valid? result)))
      (is (some #(re-find #"Фамилия не должна превышать 50 символов" %) (:errors result)))))
  
  (testing "Имя слишком длинное"
    (let [result (validate-worker {:фамилия "Иванов"
                                   :имя (apply str (repeat 51 "а"))
                                   :дата_приема "2024-01-01"
                                   :цех_id "1"
                                   :система_оплаты_id "1"
                                   :категория_работника_id "1"
                                   :разряд_id "1"
                                   :режим_работы_id "1"})]
      (is (not (:valid? result)))
      (is (some #(re-find #"Имя не должно превышать 50 символов" %) (:errors result))))))

(deftest test-validate-worker-missing-references
  (testing "Отсутствие цеха"
    (let [result (validate-worker {:фамилия "Иванов"
                                   :имя "Иван"
                                   :дата_приема "2024-01-01"
                                   :цех_id ""
                                   :система_оплаты_id "1"
                                   :категория_работника_id "1"
                                   :разряд_id "1"
                                   :режим_работы_id "1"})]
      (is (not (:valid? result)))
      (is (some #(re-find #"Необходимо выбрать цех" %) (:errors result)))))
  
  (testing "Отсутствие системы оплаты"
    (let [result (validate-worker {:фамилия "Иванов"
                                   :имя "Иван"
                                   :дата_приема "2024-01-01"
                                   :цех_id "1"
                                   :система_оплаты_id ""
                                   :категория_работника_id "1"
                                   :разряд_id "1"
                                   :режим_работы_id "1"})]
      (is (not (:valid? result)))
      (is (some #(re-find #"Необходимо выбрать систему оплаты" %) (:errors result))))))

(deftest test-validate-worker-salary-system
  (testing "Окладная система без оклада"
    (let [result (validate-worker {:фамилия "Иванов"
                                   :имя "Иван"
                                   :дата_приема "2024-01-01"
                                   :цех_id "1"
                                   :система_оплаты_id "1"
                                   :категория_работника_id "1"
                                   :разряд_id "1"
                                   :режим_работы_id "1"
                                   :оклад_id ""
                                   :почасовая_ставка_id ""})]
      (is (not (:valid? result)))
      (is (some #(re-find #"Для окладной системы необходимо выбрать оклад" %) (:errors result)))))
  
  (testing "Почасовая система без ставки"
    (let [result (validate-worker {:фамилия "Иванов"
                                   :имя "Иван"
                                   :дата_приема "2024-01-01"
                                   :цех_id "1"
                                   :система_оплаты_id "2"
                                   :категория_работника_id "1"
                                   :разряд_id "1"
                                   :режим_работы_id "1"
                                   :оклад_id ""
                                   :почасовая_ставка_id ""})]
      (is (not (:valid? result)))
      (is (some #(re-find #"Для почасовой системы необходимо выбрать ставку" %) (:errors result))))))

;; ======================================================================
;; Тесты для валидации учета рабочего времени
;; ======================================================================

(deftest test-validate-work-time-valid
  (testing "Валидные данные учета времени"
    (is (:valid? (validate-work-time {:год "2024"
                                      :месяц "10"
                                      :всего_часов_за_месяц_по_плану "160"
                                      :всего_часов_в_месяц_по_факту "158"
                                      :количество_отработанных_дней "21"
                                      :количество_рабочих_часов_в_день "8"
                                      :всего_отработанных_часов "158"
                                      :сколько_должны_отработать "168"
                                      :больничные_дни "2"
                                      :командировочные_дни "1"})))))

(deftest test-validate-work-time-required
  (testing "Отсутствие года"
    (let [result (validate-work-time {:месяц "10"
                                      :всего_часов_за_месяц_по_плану "160"
                                      :всего_часов_в_месяц_по_факту "158"})]
      (is (not (:valid? result)))
      (is (some #(re-find #"Год обязателен" %) (:errors result)))))
  
  (testing "Отсутствие месяца"
    (let [result (validate-work-time {:год "2024"
                                      :всего_часов_за_месяц_по_плану "160"
                                      :всего_часов_в_месяц_по_факту "158"})]
      (is (not (:valid? result)))
      (is (some #(re-find #"Месяц обязателен" %) (:errors result))))))

(deftest test-validate-work-time-format
  (testing "Неверный формат года"
    (let [result (validate-work-time {:год "202"
                                      :месяц "10"
                                      :всего_часов_за_месяц_по_плану "160"
                                      :всего_часов_в_месяц_по_факту "158"})]
      (is (not (:valid? result)))
      (is (some #(re-find #"Неверный формат года" %) (:errors result)))))
  
  (testing "Неверный диапазон месяца"
    (let [result (validate-work-time {:год "2024"
                                      :месяц "13"
                                      :всего_часов_за_месяц_по_плану "160"
                                      :всего_часов_в_месяц_по_факту "158"})]
      (is (not (:valid? result)))
      (is (some #(re-find #"Месяц должен быть от 1 до 12" %) (:errors result))))))

(deftest test-validate-work-time-numeric
  (testing "Не числовое значение плановых часов"
    (let [result (validate-work-time {:год "2024"
                                      :месяц "10"
                                      :всего_часов_за_месяц_по_плану "abc"
                                      :всего_часов_в_месяц_по_факту "158"})]
      (is (not (:valid? result)))
      (is (some #(re-find #"Плановые часы должны быть числом" %) (:errors result))))))

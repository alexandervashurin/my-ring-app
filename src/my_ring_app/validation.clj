(ns my-ring-app.validation
  (:require [clojure.string :as str]))

(defn validate-worker [data]
  "Валидация данных работника
   Возвращает {:valid? true} или {:valid? false :errors [...]}"

  (let [errors (atom [])]
    
    ;; Проверка фамилии
    (when (or (empty? (:фамилия data))
              (str/blank? (:фамилия data)))
      (swap! errors conj "Фамилия обязательна"))
    
    (when (and (:фамилия data)
               (> (count (:фамилия data)) 50))
      (swap! errors conj "Фамилия не должна превышать 50 символов"))
    
    ;; Проверка имени
    (when (or (empty? (:имя data))
              (str/blank? (:имя data)))
      (swap! errors conj "Имя обязательно"))
    
    (when (and (:имя data)
               (> (count (:имя data)) 50))
      (swap! errors conj "Имя не должно превышать 50 символов"))
    
    ;; Проверка отчества (необязательное, но если есть - проверяем)
    (when (and (seq (:отчество data))
               (> (count (:отчество data)) 50))
      (swap! errors conj "Отчество не должно превышать 50 символов"))
    
    ;; Проверка даты приема
    (when (or (empty? (:дата_приема data))
              (str/blank? (:дата_приема data)))
      (swap! errors conj "Дата приема обязательна"))
    
    (when (and (:дата_приема data)
               (not (re-matches #"^\d{4}-\d{2}-\d{2}$" (:дата_приема data))))
      (swap! errors conj "Неверный формат даты (ожидается ГГГГ-ММ-ДД)"))
    
    (when (and (:дата_приема data)
               (re-matches #"^\d{4}-\d{2}-\d{2}$" (:дата_приема data))
               (.isAfter (java.time.LocalDate/parse (:дата_приема data))
                         (java.time.LocalDate/now)))
      (swap! errors conj "Дата приема не может быть в будущем"))
    
    ;; Проверка цеха
    (when (or (empty? (:цех_id data))
              (= (:цех_id data) ""))
      (swap! errors conj "Необходимо выбрать цех"))
    
    ;; Проверка системы оплаты
    (when (or (empty? (:система_оплаты_id data))
              (= (:система_оплаты_id data) ""))
      (swap! errors conj "Необходимо выбрать систему оплаты"))
    
    ;; Проверка категории работника
    (when (or (empty? (:категория_работника_id data))
              (= (:категория_работника_id data) ""))
      (swap! errors conj "Необходимо выбрать категорию работника"))
    
    ;; Проверка разряда
    (when (or (empty? (:разряд_id data))
              (= (:разряд_id data) ""))
      (swap! errors conj "Необходимо выбрать разряд"))
    
    ;; Проверка режима работы
    (when (or (empty? (:режим_работы_id data))
              (= (:режим_работы_id data) ""))
      (swap! errors conj "Необходимо выбрать режим работы"))
    
    ;; Условная проверка оклада/ставки
    (when (and (= (:система_оплаты_id data) "1")  ; Оклад
               (empty? (:оклад_id data)))
      (swap! errors conj "Для окладной системы необходимо выбрать оклад"))
    
    (when (and (= (:система_оплаты_id data) "2")  ; Почасовая
               (empty? (:почасовая_ставка_id data)))
      (swap! errors conj "Для почасовой системы необходимо выбрать ставку"))
    
    ;; Возвращаем результат
    (if (empty? @errors)
      {:valid? true}
      {:valid? false :errors @errors})))

(defn validate-worker-update [data]
  "Валидация данных при обновлении работника (более мягкая)"
  (validate-worker data))

;; НОВАЯ ФУНКЦИЯ: Валидация учета рабочего времени
(defn validate-work-time [data]
  "Валидация данных учета рабочего времени"
  (let [errors (atom [])]
    
    ;; Проверка года
    (when (or (empty? (:год data))
              (str/blank? (:год data)))
      (swap! errors conj "Год обязателен"))
    
    (when (and (:год data)
               (not (re-matches #"^\d{4}$" (:год data))))
      (swap! errors conj "Неверный формат года (ожидается 4 цифры)"))
    
    ;; Проверка месяца
    (when (or (empty? (:месяц data))
              (str/blank? (:месяц data)))
      (swap! errors conj "Месяц обязателен"))
    
    (when (and (:месяц data)
               (not (re-matches #"^\d{1,2}$" (:месяц data))))
      (swap! errors conj "Неверный формат месяца"))
    
    (when (and (:месяц data)
               (let [month (Integer/parseInt (:месяц data))]
                 (or (< month 1) (> month 12))))
      (swap! errors conj "Месяц должен быть от 1 до 12"))
    
    ;; Проверка плановых часов
    (when (or (empty? (:всего_часов_за_месяц_по_плану data))
              (str/blank? (:всего_часов_за_месяц_по_плану data)))
      (swap! errors conj "Плановые часы обязательны"))
    
    (when (and (:всего_часов_за_месяц_по_плану data)
               (not (re-matches #"^\d+$" (:всего_часов_за_месяц_по_плану data))))
      (swap! errors conj "Плановые часы должны быть числом"))
    
    ;; Проверка фактических часов
    (when (or (empty? (:всего_часов_в_месяц_по_факту data))
              (str/blank? (:всего_часов_в_месяц_по_факту data)))
      (swap! errors conj "Фактические часы обязательны"))
    
    (when (and (:всего_часов_в_месяц_по_факту data)
               (not (re-matches #"^\d+$" (:всего_часов_в_месяц_по_факту data))))
      (swap! errors conj "Фактические часы должны быть числом"))
    
    ;; Проверка отработанных дней
    (when (and (seq (:количество_отработанных_дней data))
               (not (re-matches #"^\d+$" (:количество_отработанных_дней data))))
      (swap! errors conj "Отработанные дни должны быть числом"))
    
    ;; Проверка рабочих часов в день
    (when (and (seq (:количество_рабочих_часов_в_день data))
               (not (re-matches #"^\d+$" (:количество_рабочих_часов_в_день data))))
      (swap! errors conj "Рабочие часы в день должны быть числом"))
    
    ;; Проверка больничных дней
    (when (and (seq (:больничные_дни data))
               (not (re-matches #"^\d+$" (:больничные_дни data))))
      (swap! errors conj "Больничные дни должны быть числом"))
    
    ;; Проверка командировочных дней
    (when (and (seq (:командировочные_дни data))
               (not (re-matches #"^\d+$" (:командировочные_дни data))))
      (swap! errors conj "Командировочные дни должны быть числом"))
    
    ;; Возвращаем результат
    (if (empty? @errors)
      {:valid? true}
      {:valid? false :errors @errors})))
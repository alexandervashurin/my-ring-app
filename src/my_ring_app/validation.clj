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
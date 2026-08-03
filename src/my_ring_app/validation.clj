(ns my-ring-app.validation
  (:require [clojure.string :as str]))

(defn validate-worker
  "Валидация данных работника
   Возвращает {:valid? true} или {:valid? false :errors [...]}"
  [data]

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
    (when (or (nil? (:цех_id data))
              (= (:цех_id data) ""))
      (swap! errors conj "Необходимо выбрать цех"))
    
    ;; Проверка системы оплаты
    (when (or (nil? (:система_оплаты_id data))
              (= (:система_оплаты_id data) ""))
      (swap! errors conj "Необходимо выбрать систему оплаты"))
    
    ;; Проверка категории работника
    (when (or (nil? (:категория_работника_id data))
              (= (:категория_работника_id data) ""))
      (swap! errors conj "Необходимо выбрать категорию работника"))
    
    ;; Проверка разряда
    (when (or (nil? (:разряд_id data))
              (= (:разряд_id data) ""))
      (swap! errors conj "Необходимо выбрать разряд"))
    
    ;; Проверка режима работы
    (when (or (nil? (:режим_работы_id data))
              (= (:режим_работы_id data) ""))
      (swap! errors conj "Необходимо выбрать режим работы"))
    
    ;; Условная проверка оклада/ставки
    (when (and (= (:система_оплаты_id data) "1")  ; Оклад
               (or (nil? (:оклад_id data))
                   (= (:оклад_id data) "")))
      (swap! errors conj "Для окладной системы необходимо выбрать оклад"))
    
    (when (and (= (:система_оплаты_id data) "2")  ; Почасовая
               (or (nil? (:почасовая_ставка_id data))
                   (= (:почасовая_ставка_id data) "")))
      (swap! errors conj "Для почасовой системы необходимо выбрать ставку"))
    
    ;; Возвращаем результат
    (if (empty? @errors)
      {:valid? true}
      {:valid? false :errors @errors})))

;; НОВАЯ ФУНКЦИЯ: Валидация учета рабочего времени
(defn- str-value
  "Приведение значения к строке (nil остаётся nil)"
  [v]
  (when (some? v) (str v)))

(defn validate-work-time
  "Валидация данных учета рабочего времени"
  [data]
  (let [errors (atom [])
        год (str-value (:год data))
        месяц (str-value (:месяц data))
        плановые-часы (str-value (:всего_часов_за_месяц_по_плану data))
        фактические-часы (str-value (:всего_часов_в_месяц_по_факту data))
        отработанные-дни (str-value (:количество_отработанных_дней data))
        часы-в-день (str-value (:количество_рабочих_часов_в_день data))
        всего-отработанных-часов (str-value (:всего_отработанных_часов data))
        сколько-должны (str-value (:сколько_должны_отработать data))
        больничные (str-value (:больничные_дни data))
        командировочные (str-value (:командировочные_дни data))]
    
    ;; Проверка года
    (when (or (empty? год)
              (str/blank? год))
      (swap! errors conj "Год обязателен"))
    
    (when (and год
               (not (re-matches #"^\d{4}$" год)))
      (swap! errors conj "Неверный формат года (ожидается 4 цифры)"))
    
    ;; Проверка месяца
    (when (or (empty? месяц)
              (str/blank? месяц))
      (swap! errors conj "Месяц обязателен"))
    
    (when (and месяц
               (not (re-matches #"^\d{1,2}$" месяц)))
      (swap! errors conj "Неверный формат месяца"))
    
    (when (and месяц
               (let [month (Integer/parseInt месяц)]
                 (or (< month 1) (> month 12))))
      (swap! errors conj "Месяц должен быть от 1 до 12"))
    
    ;; Проверка плановых часов
    (when (or (empty? плановые-часы)
              (str/blank? плановые-часы))
      (swap! errors conj "Плановые часы обязательны"))
    
    (when (and плановые-часы
               (not (re-matches #"^\d+$" плановые-часы)))
      (swap! errors conj "Плановые часы должны быть числом"))
    
    ;; Проверка фактических часов
    (when (or (empty? фактические-часы)
              (str/blank? фактические-часы))
      (swap! errors conj "Фактические часы обязательны"))
    
    (when (and фактические-часы
               (not (re-matches #"^\d+$" фактические-часы)))
      (swap! errors conj "Фактические часы должны быть числом"))
    
    ;; Проверка отработанных дней
    (when (and (seq отработанные-дни)
               (not (re-matches #"^\d+$" отработанные-дни)))
      (swap! errors conj "Отработанные дни должны быть числом"))
    
    ;; Проверка рабочих часов в день
    (when (and (seq часы-в-день)
               (not (re-matches #"^\d+$" часы-в-день)))
      (swap! errors conj "Рабочие часы в день должны быть числом"))

    ;; Проверка отработанных часов
    (when (and (seq всего-отработанных-часов)
               (not (re-matches #"^\d+$" всего-отработанных-часов)))
      (swap! errors conj "Отработанные часы должны быть числом"))
    
    ;; Проверка должны отработать
    (when (and (seq сколько-должны)
               (not (re-matches #"^\d+$" сколько-должны)))
      (swap! errors conj "Значение 'должны отработать' должно быть числом"))
    
    ;; Проверка больничных дней
    (when (and (seq больничные)
               (not (re-matches #"^\d+$" больничные)))
      (swap! errors conj "Больничные дни должны быть числом"))
    
    ;; Проверка командировочных дней
    (when (and (seq командировочные)
               (not (re-matches #"^\d+$" командировочные)))
      (swap! errors conj "Командировочные дни должны быть числом"))
    
    ;; Возвращаем результат
    (if (empty? @errors)
      {:valid? true}
      {:valid? false :errors @errors})))
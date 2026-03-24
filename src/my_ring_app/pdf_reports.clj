(ns my-ring-app.pdf-reports
  "Генерация PDF отчётов - заглушка (clj-pdf недоступен)"
  (:require [my-ring-app.logger :as logger]))

;; Заглушка для PDF функциональности
(defn generate-worker-pdf
  "Генерация PDF отчёта по работнику - заглушка"
  [worker-id output-path]
  (logger/log-warn (format "PDF отчёт для работника ID=%s не сгенерирован (функциональность отключена)" worker-id))
  {:success false :message "PDF функциональность отключена"})

(defn generate-workers-list-pdf
  "Генерация PDF списка работников - заглушка"
  [output-path]
  (logger/log-warn "PDF список работников не сгенерирован (функциональность отключена)")
  {:success false :message "PDF функциональность отключена"})

(defn generate-salary-report-pdf
  "Генерация PDF отчёта по зарплате - заглушка"
  [output-path year month]
  (logger/log-warn (format "PDF отчёт по зарплате за %d-%d не сгенерирован (функциональность отключена)" year month))
  {:success false :message "PDF функциональность отключена"})

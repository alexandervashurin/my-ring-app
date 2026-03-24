(ns my-ring-app.email
  "Email уведомления - заглушка (javax.mail недоступен)"
  (:require [my-ring-app.logger :as logger]))

;; Заглушка для email функциональности
(defn send-email
  "Отправка email через SMTP - заглушка"
  [to subject body & [cc]]
  (logger/log-warn (format "Email не отправлен (SMTP не настроен): %s, тема: %s" to subject))
  {:success false :message "Email функциональность отключена"})

(defn notify-new-worker
  "Уведомление о приёме нового работника - заглушка"
  [worker recipients]
  (logger/log-warn "Уведомление о новом работнике не отправлено")
  {:success false :message "Email функциональность отключена"})

(defn notify-birthday
  "Уведомление о дне рождения - заглушка"
  [worker age recipients]
  (logger/log-warn "Уведомление о дне рождения не отправлено")
  {:success false :message "Email функциональность отключена"})

(defn notify-work-anniversary
  "Уведомление о годовщине работы - заглушка"
  [worker years recipients]
  (logger/log-warn "Уведомление о годовщине не отправлено")
  {:success false :message "Email функциональность отключена"})

(defn test-email-connection
  "Тестирование подключения к SMTP - заглушка"
  []
  {:success false :message "Email функциональность отключена"})

(ns my-ring-app.email
  "Email уведомления на javax.mail"
  (:require [my-ring-app.config :as config]
            [my-ring-app.logger :as logger]
            [clojure.string :as str])
  (:import (javax.mail Session Message Transport Address InternetAddress)
           (javax.mail.internet MimeMessage InternetAddress)
           (java.util Properties)))

;; ======================================================================
;; Конфигурация
;; ======================================================================

(defn- get-email-config
  "Получение конфигурации email из переменных окружения"
  []
  {:host (or (System/getenv "SMTP_HOST") "smtp.gmail.com")
   :port (Integer/parseInt (or (System/getenv "SMTP_PORT") "587"))
   :user (or (System/getenv "SMTP_USER") "")
   :pass (or (System/getenv "SMTP_PASS") "")
   :tls? (Boolean/parseBoolean (or (System/getenv "SMTP_TLS") "true"))
   :from (or (System/getenv "SMTP_FROM") "noreply@example.com")})

;; ======================================================================
;; Шаблоны писем
;; ======================================================================

(defn- new-worker-template [worker]
  "Шаблон письма о приёме нового работника"
  (let [full-name (str (:фамилия worker) " " (:имя worker) " " (or (:отчество worker) ""))]
    {:subject (str "🎉 Принят новый работник: " full-name)
     :body (str "<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>"
                "<h2 style='color: #667eea;'>🎉 Принят новый работник</h2>"
                "<p>Уважаемые коллеги!</p>"
                "<p>Сообщаем вам, что в нашу организацию принят новый работник:</p>"
                "<table style='border-collapse: collapse; width: 100%; margin: 20px 0;'>"
                "<tr><td style='padding: 8px; border: 1px solid #ddd; font-weight: bold;'>ФИО:</td>"
                "<td style='padding: 8px; border: 1px solid #ddd;'>" full-name "</td></tr>"
                "<tr><td style='padding: 8px; border: 1px solid #ddd; font-weight: bold;'>Дата приёма:</td>"
                "<td style='padding: 8px; border: 1px solid #ddd;'>" (:дата_приема worker) "</td></tr>"
                "<tr><td style='padding: 8px; border: 1px solid #ddd; font-weight: bold;'>Цех:</td>"
                "<td style='padding: 8px; border: 1px solid #ddd;'>" (or (:цех worker) "-") "</td></tr>"
                "</table>"
                "<p>Просим оказать новому работнику содействие в адаптации!</p>"
                "<hr style='border: none; border-top: 1px solid #ddd; margin: 20px 0;'>"
                "<p style='color: #666; font-size: 12px;'>Это письмо отправлено автоматически системой управления персоналом.</p>"
                "</body></html>")}))

(defn- birthday-template [worker age]
  "Шаблон письма о дне рождения"
  (let [full-name (str (:фамилия worker) " " (:имя worker) " " (or (:отчество worker) ""))]
    {:subject (str "🎂 День рождения: " full-name)
     :body (str "<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>"
                "<h2 style='color: #f5576c;'>🎂 С Днём Рождения!</h2>"
                "<p>Сегодня свой день рождения отмечает:</p>"
                "<h3 style='color: #667eea; font-size: 24px; margin: 20px 0;'>" full-name "</h3>"
                "<p>Желаем крепкого здоровья, профессиональных успехов и благополучия!</p>"
                "</body></html>")}))

(defn- work-anniversary-template [worker years]
  "Шаблон письма о годовщине работы"
  (let [full-name (str (:фамилия worker) " " (:имя worker) " " (or (:отчество worker) ""))]
    {:subject (str "🎊 Годовщина работы: " full-name " (" years " лет)")
     :body (str "<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>"
                "<h2 style='color: #43e97b;'>🎊 Годовщина работы в компании</h2>"
                "<p>Сегодня исполняется <strong>" years "</strong> лет со дня приёма:</p>"
                "<h3 style='color: #667eea; font-size: 24px; margin: 20px 0;'>" full-name "</h3>"
                "<p>Благодарим за преданность компании!</p>"
                "</body></html>")}))

;; ======================================================================
;; Отправка email
;; ======================================================================

(defn send-email
  "Отправка email через SMTP"
  [to subject body & [cc]]
  (try
    (let [email-config (get-email-config)
          props (doto (Properties.)
                  (.put "mail.smtp.host" (:host email-config))
                  (.put "mail.smtp.port" (str (:port email-config)))
                  (.put "mail.smtp.auth" "true")
                  (.put "mail.smtp.starttls.enable" (str (:tls? email-config))))
          session (Session/getDefaultInstance props)
          message (MimeMessage. session)]
      
      (.setFrom message (InternetAddress. (:from email-config)))
      (.addRecipients message Message/RecipientType/TO
                      (into-array Address (map #(InternetAddress. %) (if (string? to) [to] to))))
      (when cc
        (.addRecipients message Message/RecipientType/CC
                        (into-array Address (map #(InternetAddress. %) (if (string? cc) [cc] cc)))))
      (.setSubject message subject "UTF-8")
      (.setContent message body "text/html; charset=utf-8")
      (.setSentDate message (java.util.Date.))
      
      ;; Отправка только если настроен SMTP
      (if (and (:user email-config) (:pass email-config))
        (do
          (doto (Transport/get session "smtp")
            (.connect (:host email-config) (:port email-config) (:user email-config) (:pass email-config))
            (.sendMessage message (.getAllRecipients message))
            (.close))
          (logger/log-info (format "Email отправлен: %s (тема: %s)" to subject))
          {:success true :message "Email отправлен"})
        (do
          (logger/log-warn (format "SMTP не настроен, email не отправлен: %s" to))
          {:success false :message "SMTP не настроен"}))))
    (catch Exception e
      (logger/log-error e (format "Ошибка при отправке email: %s" to))
      {:success false :message (str "Ошибка: " (.getMessage e))}))))

;; ======================================================================
;; Публичные функции
;; ======================================================================

(defn notify-new-worker
  "Уведомление о приёме нового работника"
  [worker recipients]
  (let [email-data (new-worker-template worker)]
    (doseq [recipient recipients]
      (send-email recipient (:subject email-data) (:body email-data)))
    {:success true :sent (count recipients)}))

(defn notify-birthday
  "Уведомление о дне рождения"
  [worker age recipients]
  (let [email-data (birthday-template worker age)]
    (doseq [recipient recipients]
      (send-email recipient (:subject email-data) (:body email-data)))
    {:success true :sent (count recipients)}))

(defn notify-work-anniversary
  "Уведомление о годовщине работы"
  [worker years recipients]
  (let [email-data (work-anniversary-template worker years)]
    (doseq [recipient recipients]
      (send-email recipient (:subject email-data) (:body email-data)))
    {:success true :sent (count recipients)}))

(defn test-email-connection
  "Тестирование подключения к SMTP"
  []
  (let [email-config (get-email-config)]
    (if (and (:user email-config) (:pass email-config))
      {:success true :message "SMTP конфигурация найдена" :host (:host email-config)}
      {:success false :message "SMTP конфигурация не найдена (требуется настройка переменных окружения)"})))

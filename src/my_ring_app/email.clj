(ns my-ring-app.email
  "Email уведомления через SMTP (postal / javax.mail).
   Включены только если задан SMTP_HOST, иначе функции безопасно возвращают
   статус отключённой функциональности (приложение работает без почты)."
  (:require [clojure.string :as str]
            [postal.core :as postal]
            [my-ring-app.config :as config]
            [my-ring-app.logger :as logger])
  (:import [java.util Properties]
           [javax.mail Session Transport]))

;; ======================================================================
;; SMTP
;; ======================================================================

(defn smtp-enabled?
  "Включена ли отправка почты (SMTP_HOST задан)"
  []
  (:enabled (config/smtp-config)))

(defn- smtp-session
  "Создание javax.mail.Session из конфигурации"
  [cfg]
  (let [props (doto (Properties.)
                (.put "mail.smtp.host" (:host cfg))
                (.put "mail.smtp.port" (str (:port cfg)))
                (.put "mail.smtp.auth" (if (str/blank? (:user cfg)) "false" "true")))]
    (when (:tls cfg)
      (.put props "mail.smtp.starttls.enable" "true"))
    (when (:ssl cfg)
      (.put props "mail.smtp.ssl.enable" "true"))
    (when (:debug cfg)
      (.put props "mail.smtp.debug" "true"))
    (Session/getInstance props)))

(defn- postal-message
  "Формирование сообщения для postal.core/send-message"
  [to subject body & [cc]]
  (let [cfg (config/smtp-config)]
    (merge
     {:host (:host cfg)
      :port (:port cfg)
      :user (:user cfg)
      :pass (:password cfg)
      :tls (:tls cfg)
      :ssl (:ssl cfg)
      :from (:from cfg)
      :to to
      :subject subject
      :body [:alternative (str "<html><body style='font-family: Arial, sans-serif;'>"
                               body
                               "<p style='color: #888; font-size: 12px;'>С уважением, система управления персоналом «Кольцо»</p>"
                               "</body></html>")
             (-> body
                 (str/replace #"<br\s*/?>" "\n")
                 (str/replace #"<[^>]+>" "")
                 (str/replace #"&nbsp;" " "))]}
     (when cc {:cc cc}))))

;; ======================================================================
;; Отправка
;; ======================================================================

(defn send-email
  "Отправка email через SMTP. Возвращает {:success bool :sent int :message str}"
  [to subject body & [cc]]
  (if-not (smtp-enabled?)
    (do
      (logger/log-warn (format "Email не отправлен (SMTP_HOST не задан): %s, тема: %s" to subject))
      {:success false :sent 0 :message "Email функциональность отключена (SMTP_HOST не задан)"})
    (try
      (let [result (postal/send-message (postal-message to subject body cc))]
        (if (nil? result)
          (do
            (logger/log-info (format "Email отправлен: %s, тема: %s" to subject))
            {:success true :sent 1 :message "Email отправлен"})
          (do
            (logger/log-warn (format "Email не отправлен: %s, причина: %s" to (:message result)))
            {:success false :sent 0 :message (or (:message result) "Ошибка отправки email")})))
      (catch Exception e
        (logger/log-error e "Ошибка при отправке email" {:to to})
        {:success false :sent 0 :message (str "Ошибка отправки email: " (.getMessage e))}))))

(defn- send-email-to-many
  "Отправка одного письма нескольким получателям. Возвращает {:sent N}"
  [subject body recipients]
  (loop [emails (seq recipients)
         sent 0]
    (if-not emails
      {:sent sent}
      (let [result (send-email (first emails) subject body)]
        (recur (next emails) (if (:success result) (inc sent) sent))))))

;; ======================================================================
;; Уведомления
;; ======================================================================

(defn- worker-full-name
  [worker]
  (str/join " " (remove str/blank? [(:фамилия worker) (:имя worker) (:отчество worker)])))

(defn notify-new-worker
  "Уведомление о приёме нового работника"
  [worker recipients]
  (if-not (smtp-enabled?)
    (do
      (logger/log-warn "Уведомление о новом работнике не отправлено (SMTP не настроен)")
      {:success false :sent 0 :message "Email функциональность отключена (SMTP_HOST не задан)"})
    (let [name (worker-full-name worker)
          subject (str "Новый работник: " name)
          body (str "<h2 style='color: #667eea;'>Новый работник принят</h2>"
                    "<p><b>" name "</b> принят в систему управления персоналом.</p>"
                    "<p><b>ID:</b> " (:id worker)
                    "<br><b>Дата приёма:</b> " (or (:дата_приема worker) "-") "</p>")
          result (send-email-to-many subject body recipients)]
      (logger/log-info (format "Уведомление о новом работнике отправлено %d получателям" (:sent result)))
      (merge result {:success (pos? (:sent result))}))))

(defn notify-birthday
  "Уведомление о дне рождения"
  [worker age recipients]
  (if-not (smtp-enabled?)
    (do
      (logger/log-warn "Уведомление о дне рождения не отправлено (SMTP не настроен)")
      {:success false :sent 0 :message "Email функциональность отключена (SMTP_HOST не задан)"})
    (let [name (worker-full-name worker)
          subject (str "Сегодня день рождения: " name)
          body (str "<h2 style='color: #667eea;'>С днём рождения!</h2>"
                    "<p><b>" name "</b> сегодня отмечает "
                    "<b>" age "</b> лет!</p>"
                    "<p>Не забудьте поздравить коллегу! 🎉</p>")
          result (send-email-to-many subject body recipients)]
      (logger/log-info (format "Уведомление о дне рождения отправлено %d получателям" (:sent result)))
      (merge result {:success (pos? (:sent result))}))))

(defn- year-suffix
  "Склонение слова 'год/года/лет'"
  [n]
  (let [n (mod (int (or n 0)) 100)
        last-digit (mod n 10)]
    (cond
      (and (>= n 11) (<= n 14)) "лет"
      (= last-digit 1) "год"
      (and (>= last-digit 2) (<= last-digit 4)) "года"
      :else "лет")))

(defn notify-work-anniversary
  "Уведомление о годовщине работы"
  [worker years recipients]
  (if-not (smtp-enabled?)
    (do
      (logger/log-warn "Уведомление о годовщине не отправлено (SMTP не настроен)")
      {:success false :sent 0 :message "Email функциональность отключена (SMTP_HOST не задан)"})
    (let [name (worker-full-name worker)
          subject (str "Годовщина работы: " name)
          body (str "<h2 style='color: #667eea;'>Годовщина работы</h2>"
                    "<p><b>" name "</b> отмечает <b>" years "</b> "
                    (year-suffix years) " работы в компании!</p>"
                    "<p>Поздравляем с юбилеем! 🏆</p>")
          result (send-email-to-many subject body recipients)]
      (logger/log-info (format "Уведомление о годовщине отправлено %d получателям" (:sent result)))
      (merge result {:success (pos? (:sent result))}))))

;; ======================================================================
;; Проверка подключения
;; ======================================================================

(defn test-email-connection
  "Тестирование подключения к SMTP. Возвращает {:success bool :message str}"
  []
  (if-not (smtp-enabled?)
    {:success false :message "Email функциональность отключена (SMTP_HOST не задан)"}
    (try
      (let [cfg (config/smtp-config)
            session (smtp-session cfg)
            transport (.getTransport session "smtp")]
        (try
          (.connect transport (:host cfg) (:port cfg) (:user cfg) (:password cfg))
          (logger/log-info (format "SMTP подключение успешно: %s:%s" (:host cfg) (:port cfg)))
          {:success true :message (str "SMTP подключение успешно: " (:host cfg) ":" (:port cfg))}
          (finally
            (try (.close transport) (catch Exception _)))))
      (catch Exception e
        (logger/log-error e "Ошибка подключения к SMTP")
        {:success false :message (str "Ошибка подключения к SMTP: " (.getMessage e))}))))

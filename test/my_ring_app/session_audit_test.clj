(ns my-ring-app.session-audit-test
  (:require [clojure.test :refer :all]
            [my-ring-app.session-audit :as session-audit]
            [my-ring-app.migration :as migration]
            [my-ring-app.auth :as auth]
            [clojure.java.jdbc :as jdbc]
            [my-ring-app.config :refer [db-spec]])
  (:import [java.time LocalDateTime Duration]))

(def test-username (str "session_test_user_" (System/currentTimeMillis)))
(def test-email (str test-username "@test.com"))

(defn setup-db [f]
  (migration/run-migrations!)
  (auth/init-db!)
  (f))

(defn cleanup [f]
  (try (jdbc/delete! db-spec :Пользователь ["username = ?" test-username]) (catch Exception _))
  (try (jdbc/execute! db-spec ["DELETE FROM Сессия WHERE username = ?" test-username]) (catch Exception _))
  (f)
  (try (jdbc/delete! db-spec :Пользователь ["username = ?" test-username]) (catch Exception _))
  (try (jdbc/execute! db-spec ["DELETE FROM Сессия WHERE username = ?" test-username]) (catch Exception _)))

(use-fixtures :once setup-db)
(use-fixtures :each cleanup)

(deftest test-log-login-success
  (testing "Логирование успешного входа возвращает session-id"
    (let [session-id (session-audit/log-login! 1 test-username "127.0.0.1" "TestAgent/1.0" 1 true)]
      (is (number? session-id) "Должен вернуть числовой ID сессии")
      (is (pos? session-id) "ID сессии должен быть положительным"))))

(deftest test-log-login-failed
  (testing "Логирование неудачного входа возвращает session-id"
    (let [session-id (session-audit/log-login! 1 test-username "192.168.1.1" "Mozilla" 1 false "Неверный пароль")]
      (is (number? session-id) "Должен вернуть числовой ID сессии даже при неудаче"))))

(deftest test-log-logout
  (testing "Логирование выхода обновляет время выхода"
    (let [session-id (session-audit/log-login! 1 test-username "10.0.0.1" "curl" 1 true)]
      (is (number? session-id))
      (session-audit/log-logout! session-id)
      (let [sessions (session-audit/get-user-sessions 1 10)]
        (is (some #(and (= session-id (:id %)) (some? (:logout_time %))) sessions)
            "Сессия должна иметь logout_time после выхода")))))

(deftest test-log-logout-nil
  (testing "log-logout! с nil session-id не вызывает ошибку"
    (session-audit/log-logout! nil)
    (is true "Не должно быть исключения при nil session-id")))

(deftest test-get-user-sessions
  (testing "Получение сессий пользователя"
    (session-audit/log-login! 1 test-username "10.0.0.2" "App/1.0" 1 true)
    (let [sessions (session-audit/get-user-sessions 1 10)]
      (is (sequential? sessions) "Должен возвращать последовательность")
      (is (pos? (count sessions)) "Должны быть записи")
      (is (some #(= test-username (:username %)) sessions) "Должна быть наша тестовая сессия"))))

(deftest test-get-user-sessions-limit
  (testing "Лимит записей работает"
    (session-audit/log-login! 1 test-username "10.0.0.3" "App/2.0" 1 true)
    (session-audit/log-login! 1 test-username "10.0.0.4" "App/3.0" 1 true)
    (let [sessions (session-audit/get-user-sessions 1 1)]
      (is (= 1 (count sessions)) "Должна быть только 1 запись"))))

(deftest test-get-user-sessions-empty
  (testing "Несуществующий user-id возвращает пустой вектор"
    (let [sessions (session-audit/get-user-sessions 999999)]
      (is (sequential? sessions))
      (is (empty? sessions)))))

(deftest test-get-recent-sessions
  (testing "Получение последних сессий"
    (session-audit/log-login! 1 test-username "10.0.0.5" "Browser/1.0" 1 true)
    (let [sessions (session-audit/get-recent-sessions 10)]
      (is (sequential? sessions))
      (is (pos? (count sessions))))))

(deftest test-get-recent-sessions-by-org
  (testing "Фильтрация последних сессий по организации"
    (session-audit/log-login! 1 test-username "10.0.0.6" "Browser/2.0" 1 true)
    (let [sessions (session-audit/get-recent-sessions 10 1)]
      (is (sequential? sessions))
      (is (pos? (count sessions)))
      (is (every? #(= 1 (:organization_id %)) sessions)
          "Все сессии должны принадлежать организации 1"))))

(deftest test-get-failed-logins
  (testing "Получение неудачных попыток входа"
    (session-audit/log-login! 1 test-username "10.0.0.7" "Hacker/1.0" 1 false "Wrong password")
    (let [failed (session-audit/get-failed-logins 10)]
      (is (sequential? failed))
      (is (some #(= 0 (:success %)) failed)
          "Должны быть записи с success=0"))))

(deftest test-get-failed-logins-by-org
  (testing "Фильтрация неудачных попыток по организации"
    (session-audit/log-login! 1 test-username "10.0.0.8" "Hacker/2.0" 1 false "Bad password")
    (let [failed (session-audit/get-failed-logins 10 1)]
      (is (sequential? failed))
      (is (every? #(= 1 (:organization_id %)) failed))
      (is (every? #(zero? (:success %)) failed)))))

(deftest test-get-active-sessions
  (testing "Получение активных сессий (без logout)"
    (let [session-id (session-audit/log-login! 1 test-username "10.0.0.9" "Active/1.0" 1 true)]
      (let [active (session-audit/get-active-sessions)]
        (is (sequential? active))
        (is (some #(= session-id (:id %)) active)
            "Активная сессия должна быть в списке"))
      (session-audit/log-logout! session-id))))

(deftest test-get-active-sessions-by-org
  (testing "Фильтрация активных сессий по организации"
    (let [session-id (session-audit/log-login! 1 test-username "10.0.0.10" "Active/2.0" 1 true)]
      (let [active (session-audit/get-active-sessions 1)]
        (is (sequential? active))
        (is (pos? (count active)))
        (is (every? #(= 1 (:organization_id %)) active)))
      (session-audit/log-logout! session-id))))

(deftest test-get-session-count-by-day
  (testing "Статистика сессий по дням"
    (session-audit/log-login! 1 test-username "10.0.0.11" "Stats/1.0" 1 true)
    (let [stats (session-audit/get-session-count-by-day 30)]
      (is (sequential? stats))
      (is (pos? (count stats)))
      (is (some #(:day %) stats) "Должно быть поле day")
      (is (some #(:count %) stats) "Должно быть поле count"))))

(deftest test-get-session-count-by-day-by-org
  (testing "Статистика сессий по дням с фильтром организации"
    (session-audit/log-login! 1 test-username "10.0.0.12" "Stats/2.0" 1 true)
    (let [stats (session-audit/get-session-count-by-day 30 1)]
      (is (sequential? stats))
      (is (some #(:day %) stats)))))

(deftest test-format-sessions
  (testing "Форматирование сессий в human-readable формат"
    (let [raw [{:id 1 :user_id 1 :username "user1" :login_time "2026-07-29T10:00:00"
                :logout_time "2026-07-29T12:30:00" :ip_address "127.0.0.1"
                :user_agent "Test" :success 1 :fail_reason nil :organization_id 1}]
          formatted (session-audit/format-sessions raw)]
      (is (sequential? formatted))
      (is (pos? (count formatted)))
      (let [f (first formatted)]
        (is (true? (:success f)))
        (is (= "150 мин" (:duration f)))
        (is (= "127.0.0.1" (:ip_address f)))))))

(deftest test-format-sessions-empty
  (testing "Форматирование пустого списка"
    (is (= [] (session-audit/format-sessions []))))

  (testing "Форматирование nil"
    (is (= [] (session-audit/format-sessions nil)))))

(deftest test-format-sessions-failed
  (testing "Форматирование неудачной сессии"
    (let [raw [{:id 2 :user_id 1 :username "user2" :login_time "2026-07-29T10:00:00"
                :logout_time nil :ip_address "192.168.1.1"
                :user_agent "Test" :success 0 :fail_reason "Bad password" :organization_id 1}]
          formatted (session-audit/format-sessions raw)
          f (first formatted)]
      (is (false? (:success f)))
      (is (nil? (:duration f)) "Неудачная сессия не имеет длительности")
      (is (= "Bad password" (:fail_reason f))))))

(deftest test-authenticate-creates-session
  (testing "Аутентификация создаёт запись в Сессия"
    (let [result (auth/create-user test-username test-email "test_pass_123" "viewer")
          user (auth/get-user-by-username test-username)
          auth-result (auth/authenticate test-username "test_pass_123")]
      (is (some? auth-result))
      (is (number? (:session_id auth-result)) "authenticate должен возвращать session_id в :session_id")
      (let [sessions (session-audit/get-user-sessions (:id user) 10)]
        (is (some #(and (= test-username (:username %)) (= 1 (:success %))) sessions)
            "Должна быть запись успешного входа")))))

(deftest test-authenticate-failed-creates-session
  (testing "Неудачная аутентификация создаёт запись в Сессия"
    (let [user (auth/get-user-by-username test-username)]
      (when user
        (auth/authenticate test-username "wrong_pass_456")
        (let [sessions (session-audit/get-user-sessions (:id user) 10)]
          (is (some #(and (= 0 (:success %)) (= "Неверный пароль" (:fail_reason %))) sessions)
              "Должна быть запись неудачного входа"))))))

(ns my-ring-app.api.session-audit-test
  "Тесты для REST API журнала сессий (org-scoping)"
  (:require [clojure.test :refer :all]
            [clojure.edn :as edn]
            [my-ring-app.api.session-audit :refer :all]
            [my-ring-app.session-audit :as session-audit]
            [my-ring-app.test-helper :as helper]
            [my-ring-app.config :refer [db-spec]]
            [clojure.java.jdbc :as jdbc]))

(use-fixtures :once helper/setup-db)

(def test-username (str "api_session_user_" (System/currentTimeMillis)))

(defn cleanup [f]
  (try (jdbc/execute! db-spec ["DELETE FROM Сессия WHERE username = ?" test-username]) (catch Exception _))
  (f)
  (try (jdbc/execute! db-spec ["DELETE FROM Сессия WHERE username = ?" test-username]) (catch Exception _)))

(use-fixtures :each cleanup)

(defn- body
  [response]
  (if (string? (:body response))
    (edn/read-string (:body response))
    (:body response)))

(defn- req
  ([identity]
   (req identity {}))
  ([identity extra]
   (let [ident (merge {:username "user" :id 1} identity)]
     {:request-method :get
      :uri "/api/sessions"
      :params extra
      :route-params {}
      :identity ident
      :org-id (:organization_id identity)
      :session {:identity ident}})))

(deftest test-get-sessions-manager-sees-own-only
  (testing "Менеджер видит только свои сессии"
    (session-audit/log-login! 1 test-username "10.0.0.1" "Agent/1.0" 1 true)
    (session-audit/log-login! 2 "other_user_x" "10.0.0.2" "Agent/2.0" 1 true)
    (let [response (get-sessions-api (req {:role "manager" :id 1 :organization_id 1} {:limit 100}))
          sessions (:data (body response))]
      (is (= 200 (:status response)))
      (is (every? #(= test-username (:username %)) sessions)
          "Менеджер не должен видеть сессии других пользователей")
      (is (some #(= test-username (:username %)) sessions)))))

(deftest test-get-sessions-admin-org-scoped
  (testing "Admin видит сессии в рамках своей организации"
    (session-audit/log-login! 1 test-username "10.0.0.3" "Agent/3.0" 1 true)
    (session-audit/log-login! 2 "other_org_user" "10.0.0.4" "Agent/4.0" 2 true)
    (let [response (get-sessions-api (req {:role "admin" :id 1 :organization_id 1} {:limit 100}))
          sessions (:data (body response))]
      (is (= 200 (:status response)))
      (is (every? #(= 1 (:organization_id %)) sessions)
          "Admin видит только сессии своей организации"))))

(deftest test-get-failed-logins-api
  (testing "Неудачные попытки входа org-scoped"
    (session-audit/log-login! 1 test-username "10.0.0.5" "Hacker/1.0" 1 false "Bad password")
    (let [response (get-failed-logins-api (req {:role "manager" :id 1 :organization_id 1} {:limit 100}))
          failed (:data (body response))]
      (is (= 200 (:status response)))
      (is (every? #(false? (:success %)) failed)
          "Только неудачные попытки")
      (is (every? #(= 1 (:organization_id %)) failed)
          "Только сессии своей организации"))))

(deftest test-get-active-sessions-api
  (testing "Активные сессии org-scoped"
    (let [session-id (session-audit/log-login! 1 test-username "10.0.0.6" "Active/1.0" 1 true)]
      (let [response (get-active-sessions-api (req {:role "manager" :id 1 :organization_id 1}))
            active (:data (body response))]
        (is (= 200 (:status response)))
        (is (some #(= session-id (:id %)) active)))
      (session-audit/log-logout! session-id))))

(deftest test-get-session-stats-api
  (testing "Статистика сессий по дням org-scoped"
    (session-audit/log-login! 1 test-username "10.0.0.7" "Stats/1.0" 1 true)
    (let [response (get-session-stats-api (req {:role "manager" :id 1 :organization_id 1} {:days 30}))
          stats (:data (body response))]
      (is (= 200 (:status response)))
      (is (sequential? stats))
      (is (some #(:day %) stats)))))

(ns my-ring-app.api.organizations-test
  "Тесты для REST API организаций (мульти-тенантность)"
  (:require [clojure.test :refer :all]
            [clojure.edn :as edn]
            [my-ring-app.api.organizations :refer :all]
            [my-ring-app.test-helper :as helper]))

(use-fixtures :once helper/setup-db)

(defn- get-body
  "Получение тела ответа (map или JSON)"
  [response]
  (let [body (:body response)]
    (if (string? body)
      (edn/read-string body)
      body)))

(defn- org-req
  "Запрос с идентичностью и org-id"
  [identity org-id]
  {:request-method :get
   :uri "/api/organizations"
   :params {}
   :route-params {}
   :identity identity
   :org-id org-id})

(deftest test-get-organizations-admin-sees-all
  (testing "Глобальный admin видит все организации"
    (let [response (get-organizations (org-req {:role "admin" :username "adm"} 1))
          body (get-body response)]
      (is (= 200 (:status response)))
      (is (true? (:success body)))
      (is (>= (count (:data body)) 5)))))

(deftest test-get-organizations-manager-sees-own-only
  (testing "Менеджер видит только свою организацию (защита от утечки)"
    (let [response (get-organizations (org-req {:role "manager" :username "mgr"} 2))
          body (get-body response)
          orgs (:data body)]
      (is (= 200 (:status response)))
      (is (= 1 (count orgs)))
      (is (= 2 (:id (first orgs)))))))

(deftest test-get-organization-by-id-own-org
  (testing "Получение своей организации"
    (let [request (-> (org-req {:role "manager" :username "mgr"} 2)
                      (assoc :route-params {:id "2"}))
          response (get-organization-by-id request)
          body (get-body response)]
      (is (= 200 (:status response)))
      (is (= 2 (get-in body [:data :id]))))))

(deftest test-get-organization-by-id-not-found
  (testing "Получение несуществующей организации"
    (let [request (-> (org-req {:role "admin" :username "adm"} 1)
                      (assoc :route-params {:id "99999"}))
          response (get-organization-by-id request)
          body (get-body response)]
      (is (= 404 (:status response)))
      (is (= "NOT_FOUND" (get-in body [:error :code]))))))

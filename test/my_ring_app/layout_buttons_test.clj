(ns my-ring-app.layout-buttons-test
  (:require [clojure.test :refer :all]
            [my-ring-app.views.layout :as layout]))

(deftest header-buttons-appear-with-bound-user
  (let [html (binding [layout/*user* {:username "admin" :role "admin"}]
               (layout/wrap-html "<p>test</p>" "Тест" "home"))]
    (is (clojure.string/includes? html "Профиль"))
    (is (clojure.string/includes? html "Выход"))
    (is (clojure.string/includes? html "admin"))
    (is (clojure.string/includes? html "/shops"))
    (is (clojure.string/includes? html "/tariffs"))
    (is (clojure.string/includes? html "/organizations"))))

(deftest header-buttons-absent-without-user
  (let [html (layout/wrap-html "<p>test</p>" "Тест" "home")]
    (is (not (clojure.string/includes? html "Профиль")))
    (is (not (clojure.string/includes? html "/shops")))))

(deftest org-role-navigation
  (let [html (binding [layout/*user* {:username "mgr" :role "viewer" :org_role "org_manager"}]
               (layout/wrap-html "<p>test</p>" "Тест" "home"))]
    (is (clojure.string/includes? html "Менеджер организации"))
    (is (clojure.string/includes? html "/db"))
    (is (not (clojure.string/includes? html "/organizations")))))

(deftest no-garbage-on-login-page
  (let [html (layout/wrap-html "<p>login</p>" "Вход" nil nil)]
    (is (not (clojure.string/includes? html "Профиль")))))

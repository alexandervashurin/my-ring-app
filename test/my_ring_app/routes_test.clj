(ns my-ring-app.routes-test
  "Тесты для routes"
  (:require [clojure.test :refer :all]
            [my-ring-app.routes :as routes]
            [my-ring-app.test-helper :as helper]))

(use-fixtures :once helper/setup-db)

(defn- make-req
  "Создание тестового запроса"
  ([method uri]
   (make-req method uri {}))
  ([method uri request-overrides]
   (merge {:request-method method
           :uri uri
           :params {}
           :route-params {}
           :identity nil}
          request-overrides)))

(defn- has-status? [response status]
  (= status (:status response)))

(defn- has-redirect? [response path]
  (and (has-status? response 302)
       (= path (get-in response [:headers "Location"]))))

;; ======================================================================
;; Публичные маршруты
;; ======================================================================

(deftest test-home-page-public
  (testing "Главная страница доступна без аутентификации"
    (let [response ((resolve 'my-ring-app.routes/app-routes)
                    (make-req :get "/"))]
      (is (has-status? response 200)))))

(deftest test-login-page-public
  (testing "Страница логина доступна без аутентификации"
    (let [response ((resolve 'my-ring-app.routes/app-routes)
                    (make-req :get "/login"))]
      (is (has-status? response 200)))))

(deftest test-api-health-public
  (testing "Health check доступен без аутентификации"
    (let [response ((resolve 'my-ring-app.routes/app-routes)
                    (make-req :get "/api/health"))]
      (is (has-status? response 200)))))

(deftest test-api-ready-public
  (testing "Ready check доступен без аутентификации"
    (let [response ((resolve 'my-ring-app.routes/app-routes)
                    (make-req :get "/api/ready"))]
      (is (has-status? response 200)))))

(deftest test-api-live-public
  (testing "Live check доступен без аутентификации"
    (let [response ((resolve 'my-ring-app.routes/app-routes)
                    (make-req :get "/api/live"))]
      (is (has-status? response 200)))))

(deftest test-lang-switch-public
  (testing "Переключение языка работает"
    (let [response ((resolve 'my-ring-app.routes/app-routes)
                    (make-req :get "/lang/en"))]
      (is (has-status? response 302))
      (is (= "/" (get-in response [:headers "Location"])))
      (is (map? (:session response)))
      (is (= "en" (get-in response [:session :lang]))))))

(deftest test-lang-switch-invalid
  (testing "Невалидный язык заменяется на ru"
    (let [response ((resolve 'my-ring-app.routes/app-routes)
                    (make-req :get "/lang/xx"))]
      (is (has-status? response 302))
      (is (= "ru" (get-in response [:session :lang]))))))

(deftest test-lang-switch-preserves-session
  (testing "Переключение языка сохраняет существующую сессию"
    (let [response ((resolve 'my-ring-app.routes/app-routes)
                    (make-req :get "/lang/en" {:session {:user-id 42}}))]
      (is (= 42 (get-in response [:session :user-id])))
      (is (= "en" (get-in response [:session :lang]))))))

;; ======================================================================
;; Маршруты, требующие аутентификации
;; ======================================================================

(deftest test-auth-required-redirects-when-not-authenticated
  (testing "Маршруты с auth-required перенаправляют на /login"
    (let [response ((resolve 'my-ring-app.routes/app-routes)
                    (make-req :get "/workers"))]
      (is (has-redirect? response "/login")))))

(deftest test-dashboard-requires-auth
  (testing "Дашборд требует аутентификации"
    (let [response ((resolve 'my-ring-app.routes/app-routes)
                    (make-req :get "/dashboard"))]
      (is (has-redirect? response "/login")))))

(deftest test-workers-salary-requires-auth
  (testing "Страница зарплаты требует аутентификации"
    (let [response ((resolve 'my-ring-app.routes/app-routes)
                    (make-req :get "/workers/1/salary"))]
      (is (has-redirect? response "/login")))))

(deftest test-workers-work-time-requires-auth
  (testing "Страница учёта времени требует аутентификации"
    (let [response ((resolve 'my-ring-app.routes/app-routes)
                    (make-req :get "/workers/1/work-time"))]
      (is (has-redirect? response "/login")))))

;; ======================================================================
;; Admin-only маршруты
;; ======================================================================

(deftest test-admin-only-forbids-when-not-authenticated
  (testing "Admin-only маршруты возвращают 403 при отсутствии аутентификации"
    (let [response ((resolve 'my-ring-app.routes/app-routes)
                    (make-req :get "/organizations"))]
      (is (has-status? response 403)))))

(deftest test-admin-only-forbids-non-admin
  (testing "Admin-only маршруты запрещают роль manager"
    (let [response ((resolve 'my-ring-app.routes/app-routes)
                    (make-req :get "/organizations"
                              {:identity {:username "mgr" :role "manager"}}))]
      (is (has-status? response 403)))))

(deftest test-admin-only-allows-admin
  (testing "Admin-only маршруты разрешают роль admin"
    (let [response ((resolve 'my-ring-app.routes/app-routes)
                    (make-req :get "/organizations"
                              {:identity {:username "adm" :role "admin" :organization_id 1}}))]
      (is (= 200 (:status response))))))

(deftest test-workers-db-admin-only
  (testing "Страница /db доступна только админу"
    (let [anon-resp ((resolve 'my-ring-app.routes/app-routes)
                     (make-req :get "/db"))
          manager-resp ((resolve 'my-ring-app.routes/app-routes)
                        (make-req :get "/db"
                                  {:identity {:username "mgr" :role "manager"}}))]
      (is (has-status? anon-resp 403))
      (is (has-status? manager-resp 403)))))

;; ======================================================================
;; Manager-or-admin маршруты
;; ======================================================================

(deftest test-manager-can-access-new-worker-form
  (testing "Менеджер может открывать форму создания работника"
    (let [response ((resolve 'my-ring-app.routes/app-routes)
                    (make-req :get "/workers/new"
                              {:identity {:username "mgr" :role "manager" :organization_id 1}}))]
      (is (= 200 (:status response))))))

(deftest test-worker-cannot-access-new-worker-form
  (testing "Работник не может открывать форму создания работника"
    (let [response ((resolve 'my-ring-app.routes/app-routes)
                    (make-req :get "/workers/new"
                              {:identity {:username "w1" :role "worker"}}))]
      (is (has-status? response 403)))))

;; ======================================================================
;; 404
;; ======================================================================

(deftest test-not-found-route
  (testing "Несуществующий маршрут возвращает 404"
    (let [response ((resolve 'my-ring-app.routes/app-routes)
                    (make-req :get "/nonexistent"))]
      (is (has-status? response 404)))))

;; ======================================================================
;; API Documentation redirect
;; ======================================================================

(deftest test-api-docs-redirect
  (testing "/api-docs перенаправляет на /api-docs.html"
    (let [response ((resolve 'my-ring-app.routes/app-routes)
                    (make-req :get "/api-docs"))]
      (is (has-redirect? response "/api-docs.html")))))

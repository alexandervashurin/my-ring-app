(ns my-ring-app.logger-test
  (:require [clojure.test :refer :all]
            [clojure.tools.logging.test :refer [with-log logged?]]
            [my-ring-app.logger :as logger]))

(deftest test-log-info
  (with-log
    (logger/log-info "hello world")
    (is (logged? "my-ring-app.logger" :info "hello world"))))

(deftest test-log-warn
  (with-log
    (logger/log-warn "warning text")
    (is (logged? "my-ring-app.logger" :warn "warning text"))))

(deftest test-log-request
  (with-log
    (logger/log-request {:request-method :get :uri "/api/workers" :remote-addr "127.0.0.1"})
    (is (logged? "my-ring-app.logger" :info #"REQUEST: get /api/workers \| IP: 127.0.0.1")))
  (with-log
    (logger/log-request {:request-method :post :uri "/login" :headers {"x-forwarded-for" "10.0.0.1"}})
    (is (logged? "my-ring-app.logger" :info #"REQUEST: post /login \| IP: 10.0.0.1"))))

(deftest test-log-response
  (with-log
    (logger/log-response 200 "/api/workers")
    (is (logged? "my-ring-app.logger" :info "RESPONSE: 200 /api/workers"))))

(deftest test-log-error
  (with-log
    (logger/log-error (Exception. "boom") "test message" {:key "val"})
    (is (logged? "my-ring-app.logger" :error Exception
                 #"ERROR: boom \| MESSAGE: test message \| CONTEXT:"))))

(deftest test-log-audit
  (with-log
    (logger/log-audit "CREATE" "worker" 42 "поля изменены")
    (is (logged? "my-ring-app.logger" :info
                 #"ACTION: CREATE \| ENTITY: worker \| ID: 42 \| DETAILS: поля изменены")))
  (with-log
    (logger/log-audit "DELETE" "worker" 7)
    (is (logged? "my-ring-app.logger" :info #"DETAILS: -"))))

(deftest test-log-sql
  (with-log
    (logger/log-sql "SELECT * FROM Работник WHERE id = ?" [1 2])
    (is (logged? "my-ring-app.logger" :debug
                 #"SQL: SELECT \* FROM Работник WHERE id = \? \| PARAMS: \[1 2\]"))))

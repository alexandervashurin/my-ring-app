(ns my-ring-app.test-helper
  "Общие вспомогательные функции для тестов"
  (:require [my-ring-app.auth :as auth]
            [my-ring-app.config :refer [db-spec]]
            [my-ring-app.migration :as migration]))

(defn make-request
  "Создание тестового запроса"
  ([method uri]
   {:request-method method
    :uri uri
    :params {}
    :route-params {}
    :identity nil})
  ([method uri params]
   {:request-method method
    :uri uri
    :params params
    :route-params {}
    :identity nil})
  ([method uri params route-params identity]
   {:request-method method
    :uri uri
    :params params
    :route-params route-params
    :identity identity}))

(defn- fresh-test-db-spec
  "db-spec для изолированной тестовой БД (временный файл, не igra.db)"
  []
  (let [db-file (doto (java.io.File/createTempFile "my-ring-app-test" ".db")
                  (.deleteOnExit))]
    {:classname "org.sqlite.JDBC"
     :subprotocol "sqlite"
     :subname (.getAbsolutePath db-file)}))

(defn setup-db
  "Фикстура :once. Подменяет db-spec на свежую временную БД, применяет миграции
   и создаёт admin-пользователя. Dev-база igra.db не затрагивается."
  [f]
  (with-redefs [db-spec (fresh-test-db-spec)]
    (migration/run-migrations!)
    (auth/init-db!)
    (f)))

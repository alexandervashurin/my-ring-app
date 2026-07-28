(ns my-ring-app.test-helper
  "Общие вспомогательные функции для тестов")

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

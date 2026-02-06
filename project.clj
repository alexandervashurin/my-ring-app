(defproject my-ring-app "0.1.0-SNAPSHOT"
  :description "Система управления персоналом"
  :url "http://example.com"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 ;; Ring
                 [ring/ring-core "1.9.6"]
                 [ring/ring-jetty-adapter "1.9.6"]
                 [ring/ring-defaults "0.3.4"]
                 [ring/ring-json "0.5.1"]
                 ;; Compojure
                 [compojure "1.7.0"]
                 ;; JDBC и БД
                 [org.clojure/java.jdbc "0.7.12"]
                 [org.xerial/sqlite-jdbc "3.45.1.0"]
                 ;; Логирование — КРИТИЧЕСКИ ВАЖНО: правильные версии
                 [org.slf4j/slf4j-api "2.0.9"]
                 [ch.qos.logback/logback-classic "1.4.11"]
                 [org.clojure/tools.logging "1.2.4"]]
  :main ^:skip-aot my-ring-app.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
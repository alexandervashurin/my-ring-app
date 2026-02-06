(defproject my-ring-app "0.1.0-SNAPSHOT"
  :description "CRUD приложение для управления БД работников"
  :url "http://example.com"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [ring/ring-core "1.9.6"]
                 [ring/ring-jetty-adapter "1.9.6"]
                 [compojure "1.7.0"]
                 [ring/ring-defaults "0.3.3"]
                 [ring/ring-json "0.5.1"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [org.xerial/sqlite-jdbc "3.42.0.0"]
                 [clojure-csv/clojure-csv "2.0.2"]
                 ;; Логирование
                 [org.clojure/tools.logging "1.2.4"]
                 [ch.qos.logback/logback-classic "1.4.11"]
                 [ch.qos.logback/logback-core "1.4.11"]
                 [org.slf4j/slf4j-api "2.0.9"]]
  :main ^:skip-aot my-ring-app.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
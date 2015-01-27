(defproject capstone "0.1.0-SNAPSHOT"
  :description "A data evaluation tool for Trimet data, a capstone project for Parker Harris Emerson."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"] ; Clojure
                 [org.clojure/java.jdbc "0.3.6"] ; JDBC
                 [postgresql "9.3-1102.jdbc41"] ; Postgres
                 [java-jdbc/dsl "0.1.1"]] ; JSQL DSL
  :main ^:skip-aot capstone.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})

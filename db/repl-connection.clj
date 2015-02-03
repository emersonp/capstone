(require '[clojure.java.jdbc :as jdbc])

(def pgdb
  {:subprotocol "postgresql"
   :subname "//localhost:5432/capstone_db"
   })

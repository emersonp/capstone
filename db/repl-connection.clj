(require '[clojure.java.jdbc :as jdbc])

(def pgdb
  {:subprotocol "postgresql"
   :subname "//localhost:5432/clj_test"
   })

(def create-users-table-sql
  "CREATE TABLE users (
    id bigserial primary key,
    username varchar(255),
    email varchar(255) not null
  );")

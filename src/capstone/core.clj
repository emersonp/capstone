(ns capstone.core
  (:gen-class))

(defn average
  "Prints the average of a collection."
  [coll]
  (/ (reduce + coll) (count coll)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

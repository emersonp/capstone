(ns capstone.core
  (:gen-class)
  (require [capstone.query-interface :as qi])
  (require [capstone.gui :as gui])
  (require [seesaw.core :as seesaw]))

(defn average
  "Prints the average of a collection."
  [coll]
  (/ (reduce + coll) (count coll)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (do
    (seesaw/native!)
    (-> gui/primary-window seesaw/pack! seesaw/show!)))

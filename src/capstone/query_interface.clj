(ns capstone.query-interface
  (:use java-jdbc.sql)
  (:require [clojure.java.jdbc :as jdbc])
  (:refer-clojure :exclude [find]))

(def ^:private pgdb
  { :subprotocol "postgresql"
    :subname "//localhost:5432/capstone_db" })

(def ^:private query
  (partial jdbc/query pgdb))

(defn find-sql [table {fields :select conditions :conditions :or {fields *}}]
    (select fields table
            (where conditions)))

(defn find-by-attribute-sql
  ([table attribute value] (find-by-attribute-sql table attribute value {}))
  ([table attribute value options]
   (find-sql table (merge options {:conditions {attribute value}}))))

(defn find-by-id-sql [table id & args]
  (apply find-by-attribute-sql (concat (list table :id id) args)))

(def find (comp query find-sql))

(def find-by-attribute (comp query find-by-attribute-sql))

(def find-by-id (comp first query find-by-id-sql))

;; Database-specific wrapper functions

(defn find-route-stops
  "Given a route-number, finds all stops that route stops at."
  [route-number]
  (map :location_id (set (find-by-attribute :stopdata_03122014 :ROUTE_NUMBER route-number {:select :LOCATION_ID}))))

(defn shared-stops
  "Given two routes, finds all stops those routes share, and returns them as a set."
  [route1 route2]
  (clojure.set/intersection (set (find-route-stops route1)) (set (find-route-stops route2))))

(defn find-stop-routes
  "Given a stop-number, finds all routes that stop at it and returns them as set."
  [stop-number]
  (map :route_number (set (find-by-attribute :stopdata_03122014 :LOCATION_ID stop-number {:select :route_number}))))

(defn shared-routes
  "Given two stops, finds all stops those routes share, and returns them as a set."
  [stop1 stop2]
  (clojure.set/intersection (set (find-stop-routes stop1)) (set (find-stop-routes stop2))))

(defn connecting-stops
  "Given two stops, returns all stops where they have connecting routes."
  [stop1 stop2]
  (reduce clojure.set/union
   (for
    [route1 (find-stop-routes stop1)
     route2 (find-stop-routes stop2)]
    (shared-stops route1 route2))))

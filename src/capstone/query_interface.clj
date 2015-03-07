;; Copyright (c) 2015 Parker Harris Emerson

(ns capstone.query-interface
  (:use java-jdbc.sql)
  (:require [clojure.java.jdbc :as jdbc])
  (:refer-clojure :exclude [find])
  (:require [honeysql.core :as sql]
            [honeysql.helpers :as sqlhelp]))

(def ^:private pgdb
  { :subprotocol "postgresql"
    :subname "//localhost:5432/capstone_db" })

(def ^:private query
  (partial jdbc/query pgdb))

(defn find-sql
  [table {fields :select conditions :conditions :or {fields *}}]
  (select fields table
          (where conditions)))

(defn find-by-attribute-sql
  ([table attribute value] (find-by-attribute-sql table attribute value {}))
  ([table attribute value options]
   (find-sql table (merge options {:conditions {attribute value}}))))

(defn find-by-id-sql
  [table id & args]
  (apply find-by-attribute-sql (concat (list table :id id) args)))

(defn find-all-column-sql
  [table column]
  (select column table))

;;(defn find-all-column-sql
;;  "Finds all elements of a column."
;;  [column & args]
;;  (sql/format {:select [column args]
;;               :from [:stopdata_03122014]}))
;;               :where [:= :ROUTE_NUMBER :20]}))

(def find (comp query find-sql))

(def find-by-attribute (comp query find-by-attribute-sql))

(def find-by-id (comp first query find-by-id-sql))

(def find-all-column (comp query find-all-column-sql))

;;
;; Database-specific wrapper functions
;;

(def find-all-unique-column 
  "Applies clojure set to find all distinct values within a column"
  ;; Likely inefficient, but unsure how to implement SQL DISTINCT in JSQL.
  (comp set find-all-column))

(defn find-average-passengers
  "Given a route-number, two stop-ids, a direction and a train, finds the average number of passengers on the train."
  [route-number stop1 stop2 train direction]
  (let [datadump (find :stopdata_03122014 {:select [:ESTIMATED_LOAD :LEAVE_TIME :ROUTE_NUMBER :LOCATION_ID] :conditions {:TRAIN train :ROUTE_NUMBER route-number :DIRECTION direction}})
        time1 (:leave_time (first (find :stopdata_03122014 {:select [:LEAVE_TIME] :conditions {:TRAIN train :ROUTE_NUMBER route-number :LOCATION_ID stop1 :DIRECTION direction}})))
        time2 (:leave_time (first (find :stopdata_03122014 {:select [:LEAVE_TIME] :conditions {:TRAIN train :ROUTE_NUMBER route-number :LOCATION_ID stop2 :DIRECTION direction}})))
        start-time (min time1 time2)
        end-time (max time1 time2)
        filtered-data (filter #(>= end-time (get % :leave_time)) (filter #(<= start-time (get % :leave_time)) datadump))
        ]
    (float (/ (reduce + (map :estimated_load filtered-data)) (count filtered-data)))
    ))

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
  ;; Test with stop-ids 4537 and 3538
  [stop1 stop2]
  (reduce clojure.set/union
   (for
    [route1 (find-stop-routes stop1)
     route2 (find-stop-routes stop2)]
    (shared-stops route1 route2))))

(defn bus-arrival-departure-time-sql
  "Given a route number and a stop id, returns a set of all departure times and arrival times."
  [route-number location-id]
  (find-sql :stopdata_03122014 {:select [:LEAVE_TIME :ARRIVE_TIME] :conditions {:ROUTE_NUMBER route-number, :LOCATION_ID location-id}}))

(def bus-event (comp query bus-arrival-departure-time-sql))

(defn departing-buses-window
  "Given a route number, a stop id, and a start time and end time (in int seconds after midnight), returns all departure times and arrival times where departure times are between the start and end time."
  [route-number location-id start-window end-window]
  (filter #(< % end-window)
  (filter #(> % start-window)
          (map :leave_time
               (bus-event route-number location-id)))))

(defn arriving-buses-window
  "Given a route number, a stop id, and a start time and end time (in int seconds after midnight), returns all arrival times and arrival times where departure times are between the start and end time."
  [route-number location-id start-window end-window]
  (filter #(< % end-window)
  (filter #(> % start-window)
          (map :arrive_time
               (bus-event route-number location-id)))))

;(defn connecting-routes
;  "Given a start-stop, and end-stop, and a connecting-stop, returns all routes connecting the start-stop to the end-stop."
;  [start-stop end-stop connecting-stop]
  

(defn find-route
  "Given two stops, return all possible routes. Note that this function brute-forces the pathfinding, only finding paths between stops with 0 or 1 transfers involved."
  [stop1 stop2]
  (let [routes (shared-routes stop1 stop2)]
    (if (= routes #{})
      (prn "No shared routes.\n")
      (shared-routes stop1 stop2))))


;; Copyright (c) 2015 Parker Harris Emerson

(ns capstone.query-interface
  (:use java-jdbc.sql)
  (:require [clojure.java.jdbc :as jdbc])
  (:refer-clojure :exclude [find])
  (:require [clojure.set :refer [union]])
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
  "Given a route-number, two stop-ids, and a trip-number, finds the average number of passengers on the train."
  [route-number stop1 stop2 trip-number train]
  (let [datadump (find :stopdata_03122014 {:select [:ESTIMATED_LOAD :LEAVE_TIME :ROUTE_NUMBER :LOCATION_ID :TRIP_NUMBER :DIRECTION] :conditions {:TRIP_NUMBER trip-number, :ROUTE_NUMBER route-number, :TRAIN train}})
        time1 (:leave_time (first (filter #(= stop1 (:location_id %)) datadump)))
        time2 (:leave_time (first (filter #(= stop2 (:location_id %)) datadump)))
        start-time (if (and time1 time2)
                     (min time1 time2)
                     (or time1 time2))
        end-time (if (and time1 time2)
                     (min time1 time2)
                     (or time1 time2))
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
  "Given a route number, a stop id, and a start time and end time (in int seconds after midnight), returns all departure times where departure times are between the start and end time."
  [route-number location-id start-window end-window]
  (filter #(< % end-window)
  (filter #(> % start-window)
          (map :leave_time
               (bus-event route-number location-id)))))

(defn arriving-buses-window
  "Given a route number, a stop id, and a start time and end time (in int seconds after midnight), returns all arrival times where arrival times are between the start and end time."
  [route-number location-id start-window end-window]
  (filter #(< % end-window)
  (filter #(> % start-window)
          (map :arrive_time
               (bus-event route-number location-id)))))

(defn connecting-routes
  "Given a start-stop, an end-stop, and a connecting-stop, returns all routes connecting the start-stop to the end-stop."
  ;; Test with stop-ids 4537 7476 3538
  [start-stop connecting-stop end-stop]
  (list (shared-routes start-stop connecting-stop) (shared-routes connecting-stop end-stop)))

(defn find-all-trips
  "Given a start-stop and end-stop, as well as a start-time and end-time, if there is a connecting route, will return all trips a passenger can take."
  [start-stop end-stop start-time end-time]
  (if (shared-routes start-stop end-stop)
    (for [route (shared-routes start-stop end-stop)]
      (assoc {} :route route :departure-times (departing-buses-window route start-stop start-time end-time) :then nil))))

(defn print-all-trips
  "Prints find-all-trips"
  [start-stop end-stop start-time end-time]
  (for [trip (find-all-trips start-stop end-stop start-time end-time)]
    (for [leave-time (:departure-times trip)]
      (do
        (str "Start at stop ID " start-stop ". Get on bus number " (:route trip) " at " leave-time ". Get off at stop ID " end-stop)))))

(defn get-train-number
  "Given a stop-id, leave-time, and route, return a train."
  [stop-id leave-time route]
  (:train (first (find :stopdata_03122014 {:select [:TRAIN] :conditions {:ROUTE_NUMBER route :LOCATION_ID stop-id :LEAVE_TIME leave-time}}))))

(defn get-direction
  "Given a stop-id, leave-time, and route, return a direction."
  [stop-id leave-time route]
  (:direction (first (find :stopdata_03122014 {:select [:DIRECTION] :conditions {:ROUTE_NUMBER route :LOCATION_ID stop-id :LEAVE_TIME leave-time}}))))

(defn get-trip-number
  "Given a stop-id, leave-time, and route, return a trip number."
  [stop-id leave-time route]
  (:trip_number (first (find :stopdata_03122014 {:select [:TRIP_NUMBER] :conditions {:ROUTE_NUMBER route :LOCATION_ID stop-id :LEAVE_TIME leave-time}}))))

(defn fakefunc
  [arg1 arg2 arg3 arg4 arg5]
  [arg1 arg2 arg3 arg4 arg5])

(defn plan-trip-based-passenger
  "Given a start-stop and end-stop, and a start-time and end-time, finds best route based on emptiness of bus."
  [start-stop end-stop start-time end-time]
  (let [minimum-pass (apply min (map #(apply min %)
                                     (for [route (find-all-trips start-stop end-stop start-time end-time)]
                                       (for [departure-times (:departure-times route)]
                                         (find-average-passengers (:route route)
                                                                  start-stop
                                                                  end-stop
                                                                  (get-trip-number start-stop departure-times (:route route))
                                                                  (get-train-number start-stop departure-times (:route route)) )))
    ))]
(first (filter identity (distinct (apply union (map distinct
     (for [route (find-all-trips start-stop end-stop start-time end-time)]
       (for [departure-times (:departure-times route)]
         (if (= minimum-pass (find-average-passengers (:route route)
                                  start-stop
                                  end-stop
                                  (get-trip-number start-stop departure-times (:route route))
                                  (get-train-number start-stop departure-times (:route route)) ))
           {:route-number (:route route), :start-stop start-stop, :end-stop end-stop, :leave-time departure-times}))))))))))


(defn find-trip-stop-time
  "Given a route, a start-stop, an end-stop, and a departure-time (from start-stop), returns the arrival time of the bus at the end-stop."
  [route start-stop end-stop departure-time]
  (let [trip (find :stopdata_03122014 {:select [:TRIP_NUMBER] :conditions {:ROUTE_NUMBER route, :LOCATION_ID start-stop :LEAVE_TIME departure-time}})]
    (:arrive_time (first (find :stopdata_03122014 {:select [:ARRIVE_TIME] :conditions {:ROUTE_NUMBER route, :LOCATION_ID end-stop, :TRIP_NUMBER (:trip_number (first trip))}})))))


(defn find-route
  "Given two stops, return all possible routes. Note that this function brute-forces the pathfinding, only finding paths between stops with 0 or 1 transfers involved."
  [stop1 stop2]
  (let [routes (shared-routes stop1 stop2)]
    (if (= routes #{})
      (prn "No shared routes.\n")
      (shared-routes stop1 stop2))))


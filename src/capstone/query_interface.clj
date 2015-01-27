(ns capstone.query-interface
  (:use java-jdbc.sql)
  (:require [clojure.java.jdbc :as jdbc])
  (:refer-clojure :exclude [find]))

(def ^:private pgdb
  { :subprotocol "postgresql"
    :subname "//localhost:5432/clj_test" })

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

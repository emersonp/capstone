(ns capstone.gui
  (use [seesaw.core])
  (require [capstone.query-interface :as qi]))

(def test-content (apply str (qi/find 'stop_data_2 {:select [:badge :route_number] :conditions {:stop_time 36196}})))

(def primary-window (frame :title "Parker's Trimet Wonderstravaganza"
                           :content test-content))

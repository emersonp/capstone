(ns capstone.gui
  (use [seesaw.core])
  (require [capstone.query-interface :as qi]))

(defn make-print-list
  "Takes a collection and converts it to a string, interposing a newline in between each element."
  [coll]
  (apply str (interpose \newline coll)))

(defn num-value
  "Parses an input value of a text box into an integer and returns it."
  [object]
  (Integer/parseInt (value object)))

(defn stop-number-search
  [num]
  (make-print-list (qi/find 'stopdata_03122014 {:select [:badge :route_number]
                                          :conditions {:stop_time num}})))

(def test-content (text :editable? false
                        :multi-line? true
                        :rows 5
                        :text (stop-number-search 36196)))

(def my-left-column (grid-panel :border "Trip Parameters"
                                :columns 1
                                :items ["Prioritize"
                                        (combobox :model ["Emptiest Route" "Fastest Route"])
                                        "  "
                                        "Window for..."
                                        (combobox :model ["Arrival" "Destination"])
                                        "  "
                                        "Departure Stop ID"
                                        (text "12345")
                                        "Destination Stop ID"
                                        (text "54321")
                                        "  "
                                        (button :text "Calculate")]))

(def test-layout (grid-panel :columns 2
                             :items [my-left-column
                                     "Frank!"]))

(def primary-window (frame :title "Parker's Trimet Wonderstravaganza"
                           :content test-layout
                           :on-close :exit))

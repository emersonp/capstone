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

;; (def test-content (text :editable? false
;;                         :multi-line? true
;;                         :rows 5
;;                         :text (stop-number-search 36196)))

(def text-display (text :text "Frank"
                        :editable? false
                        :multi-line? true))

(def departure-id-text (text :text "6415"))

(def destination-id-text (text :text "6411"))

(def calculate-button (button :text "Calculate"
                              :mnemonic \C
                              :listen [:action (fn [e]
                                                 (config! text-display
                                                          :text (str
                                                                  (qi/shared-routes
                                                                    (num-value departure-id-text)
                                                                    (num-value destination-id-text)))))]))

(def my-left-column (grid-panel :border "Trip Parameters"
                                :columns 1
                                :items ["Prioritize"
                                        (combobox :model ["Emptiest Route" "Fastest Route"])
                                        "  "
                                        "Window for..."
                                        (combobox :model ["Arrival" "Destination"])
                                        "  "
                                        "Start Time"
                                        (text "12:30")
                                        "End Time"
                                        (text "13:30")
                                        " "
                                        "Departure Stop ID"
                                        departure-id-text
                                        "Destination Stop ID"
                                        destination-id-text
                                        "  "
                                        calculate-button]))

(def test-layout (grid-panel :columns 2
                             :items [my-left-column
                                     text-display]))

(def primary-window (frame :title "Parker's Trimet Wonderstravaganza"
                           :content test-layout
                           :on-close :exit))

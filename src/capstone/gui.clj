;; Copyright (c) 2015 Parker Harris Emerson

(ns capstone.gui
  (use [seesaw.core])
  (require [capstone.query-interface :as qi]))

;;
;; Declarations
;;

(declare convert-to-stop_time)
(declare convert-from-seconds)

;;
;; Primary GUI Code
;;

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

(def text-display (text :text "Frank"
                        :editable? false
                        :multi-line? true))

(def departure-id-text (text :text "6411"))

(def destination-id-text (text :text "6414"))

(def start-time-text (text :text "12:30"))

(def end-time-text (text :text "14:30"))

(def time-window-dropdown (combobox :model ["Arrival" "Destination"]))

(defn arrival-text 
  "Returns a newline-separated string of all arrival times at a stop."
 []
 (clojure.string/join "\n"
                      (sort
                        (map convert-from-seconds
                             (qi/arriving-buses-window 19
                                                        (num-value destination-id-text)
                                                        (convert-to-stop_time (value start-time-text))
                                                        (convert-to-stop_time (value end-time-text)))))))

(defn departure-text
  "Returns a newline-separated string of all departure times at a stop."
 []
 (clojure.string/join "\n"
                      (sort
                        (map convert-from-seconds
                             (qi/departing-buses-window 19
                                                        (num-value departure-id-text)
                                                        (convert-to-stop_time (value start-time-text))
                                                        (convert-to-stop_time (value end-time-text)))))))

;;(defn route-plan-text
  ;;[]

(def calculate-button
  (button :text "Calculate"
          :mnemonic \C
          :listen [:action (fn [e]
                             (config! text-display :text (let [route-map (qi/plan-trip-based-passenger (num-value departure-id-text)
                                                 (num-value destination-id-text)
                                                 (convert-to-stop_time (value start-time-text))
                                                 (convert-to-stop_time (value end-time-text)))]
    (str "Go to stop ID " (:start-stop route-map) ".\nAt " (convert-from-seconds (:leave-time route-map)) ",\nget on the " (:route-number route-map) " bus.\nGet off at stop ID " (:end-stop route-map) "."))))]))
                                      
(def my-left-column (grid-panel :border "Trip Parameters"
                                :columns 1
                                :items ["Prioritize"
                                        (combobox :model ["Emptiest Route" "Fastest Route"])
                                        "  "
                                        "Window for..."
                                        time-window-dropdown
                                        "  "
                                        "Start Time"
                                        start-time-text
                                        "End Time"
                                        end-time-text
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

;;
;; TIME-BASED FUNCTIONS
;;

(defn parse-hour
  "Parses digits before :"
  [time-string]
  (Integer/parseInt (re-find #"[^:]*" time-string)))

(defn parse-minute
  "Parses digits after :"
  [time-string]
  (Integer/parseInt (re-find #"(?<=:)[0-9]*" time-string)))

(defn parse-miltime-hour
  "Turns hours greater than 12 (military) to regular hours."
  [time-string]
  (if (> (parse-hour time-string) 12)
    (- (parse-hour time-string) 12)
    (parse-hour time-string)))

(defn seconds-after-midnight
  "Turns a time string into seconds past midnight."
  [time-string]
  (+ (* (parse-hour time-string) 3600) (* (parse-minute time-string) 60)))

(defn convert-to-stop_time
  "Turns a time string into seconds after midnight, converting times before 2 AM into seconds since previous midnight, for use with STOP_TIME."
  [time-string]
  (let [seconds (seconds-after-midnight time-string)]
    (if (< seconds 7200)
      (+ seconds 86400)
      seconds)))

(defn convert-from-seconds
  "Converts an integer of seconds into a time-string."
  [seconds]
  (let [hours (quot seconds 3600)
        minutes (format "%02d" (quot (mod seconds 3600) 60))]
    (if (> seconds 86400)
      (str (- hours 24) ":" minutes)
      (str hours ":" minutes))))



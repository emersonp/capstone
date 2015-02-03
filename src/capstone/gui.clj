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

(def center-text (flow-panel :align :center :items [(label "Center")]))

(def capture-text (text :text "The biggest and baddest."))

(def east-capture-button (button :text "Stop Number Search"
                                 :mnemonic \S
                                 :listen
                                   [:action (fn [e]
                                              (config! test-content :text (stop-number-search (num-value capture-text))))]))

(def west-capture-button
  (button :text "Find Route Stops"
          :mnemonic \F
          :listen [:action (fn [e] (config! test-content :text (make-print-list (qi/find-route-stops (num-value capture-text)))))]))

(def test-layout (border-panel :north (scrollable test-content)
                               :center center-text
                               :east east-capture-button
                               :west west-capture-button
                               :south capture-text
                               :vgap 5 :hgap 5 :border 5))

(def primary-window (frame :title "Parker's Trimet Wonderstravaganza"
                           :content test-layout
                           :on-close :exit))

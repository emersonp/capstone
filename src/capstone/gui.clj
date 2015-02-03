(ns capstone.gui
  (use [seesaw.core])
  (require [capstone.query-interface :as qi]))

(defn make-print-list
  "Takes a collection and converts it to a string, interposing a newline in between each element."
  [coll]
  (apply str (interpose \newline coll)))

(defn stop-time-search
  "Given a stop_time [num], returns the route and driver badge number of all buses stopping at that minute."
  [num]
  (make-print-list (qi/find 'stopdata_03122014 {:select [:badge :route_number]
                                          :conditions {:stop_time num}})))

(def test-content (text :editable? false
                        :multi-line? true
                        :rows 5
                        :text (stop-time-search 36196)))

(def center-text (flow-panel :align :center :items [(label "Center")]))

(def capture-text (text :text "The biggest and baddest."))

(def east-capture-button (button :text "East generate"
                                 :mnemonic \E
                                 :listen
                                   [:action (fn [e]
                                              (config! test-content :text (stop-time-search (Integer/parseInt (value capture-text)))))]))

(def west-capture-button (button :text "West generate"
                                 :mnemonic \W
                                 :listen [:action (fn [e] (config! test-content :text (str (value test-content) \newline (value capture-text))))]))

(def test-layout (border-panel :north (scrollable test-content)
                               :center center-text
                               :east east-capture-button
                               :west west-capture-button
                               :south capture-text
                               :vgap 5 :hgap 5 :border 5))

(def primary-window
  "Main window for program."
  (frame :title "Parker's Trimet Wonderstravaganza"
                           :content test-layout
                           :on-close :exit))

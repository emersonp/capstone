(ns capstone.gui
  (use [seesaw.core])
  (require [capstone.query-interface :as qi]))

(defn stop-number-search
  [num]
  (apply str (interpose \newline (qi/find 'stop_data_2 {:select [:badge :route_number] :conditions {:stop_time num}}))))


(def test-content (text :editable? false
                        :multi-line? true
                        :rows 5
                        :text (stop-number-search 36196)))

(def center-text (flow-panel :align :center :items [(label "Center")]))

(def capture-text (text :text "The biggest and baddest."))

(def east-capture-button (button :text "East generate"
                                 :mnemonic \E
                                 :listen
                                   [:action (fn [e]
                                              (config! test-content :text (stop-number-search (Integer/parseInt (value capture-text)))))]))

(def west-capture-button (button :text "West generate"
                                 :mnemonic \W
                                 :listen [:action (fn [e] (config! test-content :text (str (value test-content) \newline (value capture-text))))]))

(def test-layout (border-panel :north (scrollable test-content)
                               :center center-text
                               :east east-capture-button
                               :west west-capture-button
                               :south capture-text
                               :vgap 5 :hgap 5 :border 5))

(def primary-window (frame :title "Parker's Trimet Wonderstravaganza"
                           :content test-layout
                           :on-close :exit))

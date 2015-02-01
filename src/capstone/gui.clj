(ns capstone.gui
  (use [seesaw.core])
  (require [capstone.query-interface :as qi]))

(def test-content (label (apply str (qi/find 'stop_data_2 {:select [:badge :route_number] :conditions {:stop_time 36196}}))))

(def center-text (flow-panel :align :center :items [(label "Center")]))

(def test-layout (border-panel :north test-content
                               :center center-text
                               :east "East"
                               :west "West"
                               :vgap 5 :hgap 5 :border 5))

(def primary-window (frame :title "Parker's Trimet Wonderstravaganza"
                           :content test-layout
                           :on-close :exit))

(ns pmmt.components.map
  (:require [ajax.core :as ajax]
            [reagent.core :as r :refer [atom]]
            [reagent.session :as session]))

;;; Global state

(defonce ^{:doc "global app state"}
  app-state (atom {:gmap nil
                   :markers nil}))

;;; helper fns



;;; Dev fns

(defn get-sample-data!
  "Sample ocorrencia data for testing purposes"
  []
  (ajax/GET "/sample-ocorrencias"
            {:handler #(session/put! :sample-ocorrencias %)}))

(defonce sample-marker-data
  ;; edit data on the format expected by google
  (reduce (fn [acc m]
            (conj acc
                  {:position {:lat (:latitude m), :lng (:longitude m)}
                   :title (:local m)}))
          [] (session/get :sample-ocorrencias)))

;;; markers

(defn append-marker! [marker]
  (swap! app-state update-in [:markers] conj marker))

(defn create-oms-marker
  "OMS marker"
  [{:keys [lat lng id natureza address]}]
  (let [marker (js/StyledMarker.
                (clj->js
                 {:styleIcon (js/StyledIcon. js/StyledIconTypes.MARKER
                                             {:color "ddd", :text "O"})
                  :position (clj->js {:lat lat, :lng lng})
                  :map (:gmap @app-state)}))]
    (append-marker! marker)))

(defn create-gmaps-marker
  "Create a gmap marker and append it to the gmap instance
  google.maps.Marker takes two keys:
  - position which is a map with two keys: :lat and :lng
  - title which will be rendered as the marker title"
  [{:keys [position title]}]
  (let [marker (js/google.maps.Marker.
                 (clj->js {:position position, :title title}))]
    (.setMap marker (:gmap @app-state))
    (append-marker! marker)))

(defn clear-map! []
  (doseq [m (:markers @app-state)]
    (.setMap m nil)))


;;; Misc
(defn handle-request! [fields]
  (ajax/GET "/analise_criminal/geo/dados"
            {:params @fields
             :handler #(.log js/console (str "Success: " %))
             :error-handler #(.log js/console (str "Error: " %))}))

;;; Reagent map-container callbacks

(defn map-render []
  [:div#map.center-block])

(defn create-gmap-instance
  "Create a google.maps.Map instance and store it at the gmap instance"
  [comp]
  (let [map-canvas (r/dom-node comp)
        map-options (clj->js {:center {:lat -11.855275, :lng -55.505966}
                              :zoom 14
                              :mapTypeid js/google.maps.MapTypeId.ROADMAP})
        gmap-instance (js/google.maps.Map. map-canvas map-options)]
    (swap! app-state assoc :gmap gmap-instance)
    (:gmap @app-state)))

(defn map-component []
    (r/create-class {:display-name "map-container"
                     :reagent-render map-render
                     :component-did-mount
                     (fn [comp]
                        (let [map-canvas (r/dom-node comp)
                              map-options (clj->js {:center {:lat -11.855275, :lng -55.505966}
                                                    :zoom 14
                                                    :mapTypeid js/google.maps.MapTypeId.ROADMAP})
                              gmap-instance (js/google.maps.Map. map-canvas map-options)]
                          (swap! app-state assoc :gmap gmap-instance)))}))

(defn map-container []
  (fn []
    [map-component]))

(ns pmmt.utils.gmaps)

; ------------------------------------------------------------------------------
; JavaScript dependencies:
; - js/google.maps
; ------------------------------------------------------------------------------

(defn clear-markers! [markers]
  (when-let [markers (seq markers)]
    (doseq [m markers]
      (.setMap m nil))))

(defn add-listiner! [marker event f]
  (.addListener js/google.maps.event marker event f))

(defn create-marker!
  "Create a js/google.maps.Marker; returns the marker instantiated.
  - gMap is an instance of js/google.maps.Map;
  - infoWindow is an instance of js/google.maps.InfoWindow
  - position is a map with two keys: :lat and :lng
  - label: a string or MarkerLabel obj
  - events is a vector with an event (str) and a callback (fn)"
  [{:keys [gMap drabbagle infoWindow label position title events dragge]}]
  (let [marker (js/google.maps.Marker.
                (clj->js {:position position,
                          :title title
                          :draggable draggable
                          :label label}))]
    (.setMap marker gMap)
    (add-listiner! marker "mouseover" #(.setOpacity marker 0.5))
    (add-listiner! marker "mouseout" #(.setOpacity marker 1))
    (when infoWindow
      (add-listiner! marker "click" #(do (.setContent infoWindow title)
                                         (.open infoWindow gMap marker))))
    (when events
      (doseq [[event callback] events]
        (add-listiner! marker event callback)))
    marker))

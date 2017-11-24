(ns pmmt.components.admin.sync-lab.handlers
  (:require
   [clojure.string :as string]
   [goog.labs.format.csv :as csv]
   [pmmt.gmaps :refer [clear-markers! create-marker!]]
   [pmmt.pages.components :refer [set-state-with-value]]
   [pmmt.utils :refer [csv->map query to-csv-string <sub]]
   [pmmt.utils.geocoder :as geocoder]
   [reagent.core :as r]
   [re-frame.core :as rf]))

; ------------------------------------------------------------------------------
; Utils
; ------------------------------------------------------------------------------

(defn markable-address? [addrs]
  (and (not (string/blank? (:lat addrs)))
       (not (string/blank? (:lng addrs)))))

(defn markable-addresses [addrs]
  (filter markable-address? addrs))

; ------------------------------------------------------------------------------
; Handlers
; ------------------------------------------------------------------------------

(rf/reg-event-db
 :sync-lab/clear-map
 (fn [db _]
   (clear-markers! (<sub [:query :sync-lab.markers]))
   (assoc-in db [:sync-lab :markers] [])))

(rf/reg-event-db
 :sync-lab/geocode
 (fn [db _]
   (let [addresses (<sub [:sync-lab/selected-addresses])
         formatted-addresses
         (map (fn [{:keys [id logr-tipo logr bairro]}]
                {:id id
                 :route-type logr-tipo
                 :route logr
                 :neighborhood bairro
                 :city "Sinop"
                 :state "MT"
                 :country "Brasil"})
              addresses)
         ok (fn [GeocoderResult address]
              (let [ks [:sync-lab :addresses (:id address)]]
                ((set-state-with-value (conj ks :lat) (geocoder/lat GeocoderResult)))
                ((set-state-with-value (conj ks :lng) (geocoder/lng GeocoderResult)))
                ((set-state-with-value (conj ks :found) (geocoder/formatted-address GeocoderResult)))))]
     (geocoder/geocode-addresses
      formatted-addresses {:ok ok})
     (assoc-in db [:sync-lab :geocode :ready?] true))))


; As we load new addresses we want to clear the markers from the map,
; since they represent the previously loaded data.
(rf/reg-event-db
 :sync-lab/load-addresses
 (fn [db [_ csv-]]
   (clear-markers! (<sub [:query :sync-lab.markers]))
   (-> db
       (assoc-in [:sync-lab :markers] [])
       (assoc-in [:sync-lab :addresses]
                 (vec
                  (map-indexed (fn [i row] (assoc row :id i))
                               csv-))))))

(rf/reg-event-fx
 :sync-lab/process-input-file
 (fn [db [_ file]]
   (let [reader (js/FileReader.)]
     (set! (.-onload reader)
           #(rf/dispatch [:sync-lab/load-addresses (-> % .-target .-result csv/parse js->clj csv->map)]))
     (.readAsText reader file))
   nil))

(rf/reg-event-fx
 :sync-lab/create-marker
 (fn [{:keys [db]} [_ {:keys [title position events]}]]
   (let [marker
         (create-marker! {:gMap (get-in db [:sync-lab :gmap])
                          :infoWindow (get-in db [:sync-lab :info-window])
                          :position position
                          :title title
                          :events events})]
     {:db (update-in db [:sync-lab :markers] conj marker)})))

(rf/reg-event-fx
 :sync-lab/create-markers
 (fn [{:keys [db]} [_ addresses]]
   (doseq [row addresses]
     (let [ks [:sync-lab :addresses (:id row)]]
       (rf/dispatch [:sync-lab/create-marker
                       {:title (str (:logr-tipo row) " " (:logr row) ", " (:bairro row))
                        :position (select-keys row [:lat :lng])
                        :events [["dragend" #(do (rf/dispatch [:set-state (conj ks :lat) (-> % .-latLng .lat)])
                                                 (rf/dispatch [:set-state (conj ks :lng) (-> % .-latLng .lng)]))]]}])))
   nil))

(rf/reg-event-fx
 :sync-lab/map-selected-addresses
 (fn [{:keys [db]} _]
   (rf/dispatch [:sync-lab/clear-map])
   (rf/dispatch [:sync-lab/create-markers
                 (markable-addresses
                  (<sub [:sync-lab/selected-addresses]))])
   nil))

(rf/reg-event-db
 :init-map
 (fn [db [_ ns- comp]]
   (let [canvas (r/dom-node comp)
         ;; default map values (Sinop, MT, BR)
         map-opts (clj->js {:center {:lat -11.855275, :lng -55.505966}
                            :zoom 14
                            :mapTypeid js/google.maps.MapTypeId.ROADMAP
                            :scrollwheel false})
         ;;; initialize google maps assets
         heatmap (js/google.maps.visualization.HeatmapLayer.)
         gmap (js/google.maps.Map. canvas map-opts)
         info-window (js/google.maps.InfoWindow.)]
     (-> db
         (assoc-in (conj ns- :gmap) gmap)
         (assoc-in (conj ns- :info-window) info-window)
         (assoc-in (conj ns- :heatmap) heatmap)))))

(defn set-all-selected [db bool]
  (assoc-in db [:sync-lab :all-selected?] bool))

(defn set-selected-addresses [db val]
  (update-in db [:sync-lab :addresses]
    #(mapv (fn [row] (assoc row :selected? val))
          %)))

(rf/reg-event-db
 :sync-lab/select-all-addresses
 (fn [db _]
   (if (<sub [:sync-lab/all-selected?])
     (set-selected-addresses db nil)
     (set-selected-addresses db true))))

(rf/reg-sub :sync-lab/gmap query)

(rf/reg-sub :sync-lab/info-window query)

(rf/reg-sub :sync-lab query)

(rf/reg-sub :sync-lab/addresses query)

(rf/reg-sub :sync-lab/markers query)

(rf/reg-sub
 :sync-lab/selected-addresses
 :<- [:sync-lab/addresses]
 (fn [addresses]
   (filter :selected? addresses)))

(rf/reg-sub
 :sync-lab/all-selected?
 :<- [:sync-lab]
 (fn [sync-lab]
   (every? :selected? (:addresses sync-lab))))

(rf/reg-sub
 :sync-lab/downloadable-addresses-url
 :<- [:sync-lab/addresses]
 (fn [addresses]
   (js/URL.createObjectURL
     (js/Blob.
      (clj->js
       [(to-csv-string
         (map #(select-keys % [:bairro :logr-tipo :logr :lat :lng]) addresses)
         {:with-headers true})])
      (clj->js {:type "text/csv"})))))

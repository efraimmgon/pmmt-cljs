(ns pmmt.components.admin.sync-lab.handlers
  (:require
   [clojure.string :as string]
   [goog.labs.format.csv :as csv]
   [pmmt.gmaps :refer [clear-markers! create-marker!]]
   [pmmt.utils :refer [csv->map query to-csv-string <sub]]
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


; gMaps getters ----------------------------------------------------------------

(defn formatted-address [GeocoderResult]
  (-> GeocoderResult (get 0) .-formatted_address))

(defn lat [GeocoderResult]
  (-> GeocoderResult (get 0) .-geometry .-location .lat))

(defn lng [GeocoderResult]
  (-> GeocoderResult (get 0) .-geometry .-location .lng))


(defn assoc-geocoder-request [address]
  (assoc address :address
   (str (:logr-tipo address) " "
        (:logr address) ", "
        (:bairro address) ", "
        " Sinop - MT, Brasil")))

(defn update-lat-lng [GeocoderResult address]
  (let [ks [:sync-lab :addresses (:id address)]]
    (rf/dispatch [:set-state (conj ks :lat) (lat GeocoderResult)])
    (rf/dispatch [:set-state (conj ks :lng) (lng GeocoderResult)])
    (rf/dispatch [:set-state (conj ks :found) (formatted-address GeocoderResult)])))

(defn geocode
  "Use `js/google.maps.Geocoder` to geocode an address"
  [address query-limit?]
  (let [geocoder (new js/google.maps.Geocoder)
        opts (clj->js (select-keys address [:address]))
        handler (fn [GeocoderResult status]
                  (condp = status
                         "OK" (update-lat-lng GeocoderResult address)
                         "OVER_QUERY_LIMIT" (reset! query-limit? true)
                         "ZERO_RESULTS" (println "ZERO RESULTS => " address)
                         ;; default
                         (println status)))]
    (.geocode geocoder opts handler)))

(defn run-geocode
  "Applies geocode to each address given, making sure gmap's API
   policies are followed."
  ([addresses]
   (run-geocode addresses {:queries 0, :query-limit? (atom false)}))
  ([addresses {:keys [queries, start, query-limit?] :as control}]
   (cond
     @query-limit? (js/alert "OVER_QUERY_LIMIT")

     ;; NOTE: <implementatian simplicity> each `n` queries we wait
     ;;       for 5 seconds before continuing execution so we don't
     ;;       exceed our 50 queries/s limit.
     (= queries 20)
     (js/setTimeout
      #(run-geocode addresses (assoc control :queries 0))
      10000)

     (seq addresses)
     (do (geocode (assoc-geocoder-request (first addresses)) query-limit?)
         (run-geocode (rest addresses) (assoc control :queries (inc (:queries control)))))

     (empty? addresses)
     (js/alert "Fini!"))))

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
   (->> (<sub [:sync-lab/selected-addresses])
        (filter markable-addresses)
        run-geocode)
   (assoc-in db [:sync-lab :geocode :ready?] true)))

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

(rf/reg-sub :sync-lab/addresses query)

(rf/reg-sub :sync-lab/gmap query)

(rf/reg-sub :sync-lab/info-window query)

(rf/reg-sub :sync-lab query)

(rf/reg-sub :sync-lab/addresses query)

(rf/reg-sub
 :sync-lab/selected-addresses
 :<- [:sync-lab]
 (fn [sync-lab]
   (filter :selected? (:addresses sync-lab))))

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
         (map #(select-keys % [:bairro :via :lat :lng]) addresses)
         {:with-headers true})])
      (clj->js {:type "text/csv"})))))

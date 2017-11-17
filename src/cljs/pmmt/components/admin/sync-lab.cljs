(ns pmmt.components.admin.sync-lab
  (:require
   [clojure.string :as string]
   [clojure.walk :as walk]
   [goog.labs.format.csv :as csv]
   [reagent.core :as r :refer [atom]]
   [re-frame.core :as rf]
   [pmmt.components.common :as c]
   [pmmt.pages.components :refer [input]]
   [pmmt.utils :refer [query]]))

(defn csv->map [csv-data]
  (let [[header & rows] csv-data]
    (map (fn [row]
           (-> (zipmap
                (map (comp string/lower-case #(string/replace % #" " "-")) header)
                row)
               (walk/keywordize-keys)))
         rows)))

; ------------------------------------------------------------------------------
; Gmaps Markers
; ------------------------------------------------------------------------------
(defn clear-markers! [markers]
  (doseq [m markers]
    (.setMap m nil)))

(defn add-listiner! [marker event f]
  (.addListener js/google.maps.event marker event f))

(defn create-marker!
  "Create a gmap marker and append it to the gmap instance
  google.maps.Marker takes two keys:
  - position which is a map with two keys: :lat and :lng
  - title which will be rendered as the marker title"
  [{:keys [gmap info-window position title]}]
  (let [marker (js/google.maps.Marker.
                 (clj->js {:position position, :title title}))]
    (.setMap marker gmap)
    (add-listiner! marker "click" #(do (.setContent info-window title)
                                       (.open info-window gmap marker)))
    (add-listiner! marker "mouseover" #(.setOpacity marker 0.5))
    (add-listiner! marker "mouseout" #(.setOpacity marker 1))))

; TODO:
; [input csv file with neighborhoods, routes, lats and lats ready]
;
; [load csv file] [ok]
;
; [parse the csv: {:bairro neighborhood, :logr-tipo name, :logr name, :lat nil, :lng nil}]
;
; [query the google maps API a limited number of addresses (10)]
;
; [display the addresses on the map with the returned lats and lngs]
; [display a table with the result]
;
; [the marker’s title is the index + neighborhood + street]
; [I can move the markers and the result will be updated]
;
; [button to query the google maps API for other addresses]
;
; [button to save the addresses to a csv file]

; ------------------------------------------------------------------------------
; Handlers
; ------------------------------------------------------------------------------

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
    (rf/dispatch [:update-state (conj ks :lat) (lat GeocoderResult)])
    (rf/dispatch [:update-state (conj ks :lng) (lng GeocoderResult)])
    (rf/dispatch [:update-state (conj ks :found) (formatted-address GeocoderResult)])))

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

     ;; NOTE: <implementatian simplicity> each 50 queries we wait
     ;;       for a second before continuing execution so we don't
     ;;       exceed our 50 queries/s limit.
     (= queries 10)
     (js/setTimeout
      #(run-geocode addresses (assoc control :queries 0))
      50000)

     (seq addresses)
     (do (geocode (assoc-geocoder-request (first addresses)) query-limit?)
         (run-geocode (rest addresses) (assoc control :queries (inc (:queries control)))))

     (empty? addresses)
     (js/alert "Fini!"))))

(rf/reg-event-db
 :sync-lab/geocode
 (fn [db _]
   (run-geocode (filter #(string/blank? (:lat %)) (get-in db [:sync-lab :addresses])))
   (assoc-in db [:sync-lab :geocode :ready?] true)))

(rf/reg-event-db
 :sync-lab/load-addresses
 (fn [db [_ csv-]]
   (assoc-in db [:sync-lab :addresses]
             (vec
              (map-indexed (fn [i row] (assoc row :id i))
                           csv-)))))

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
 (fn [{:keys [db]} [_ title lat lng]]
   (create-marker! {:gmap (get-in db [:sync-lab :gmap])
                    :info-window (get-in db [:sync-lab :info-window])
                    :position {:lat lat :lng lng}
                    :title title})
   nil))

(rf/reg-event-fx
 :sync-lab/create-markers
 (fn [{:keys [db]} [_ addresses]]
   (doseq [row addresses]
     (rf/dispatch [:sync-lab/create-marker
                   (str (:logr-tipo row) " " (:logr row) ", " (:bairro row)) (:lat row) (:lng row)]))
   nil))


(defn init-gmap [db [_ ns- comp]]
  (let [canvas (r/dom-node comp)
        ;; default map values (Sinop, MT, BR)
        map-opts (clj->js {:center {:lat -11.855275, :lng -55.505966}
                           :zoom 14
                           :mapTypeid js/google.maps.MapTypeId.ROADMAP})
        ;;; initialize google maps assets
        heatmap (js/google.maps.visualization.HeatmapLayer.)
        gmap (js/google.maps.Map. canvas map-opts)
        info-window (js/google.maps.InfoWindow.)]
    (-> db
        (assoc-in (conj ns- :gmap) gmap)
        (assoc-in (conj ns- :info-window) info-window)
        (assoc-in (conj ns- :heatmap) heatmap))))

(rf/reg-event-db
 :init-map
 init-gmap)

(rf/reg-sub :sync-lab/addresses query)

(rf/reg-sub :sync-lab/gmap query)

(rf/reg-sub :sync-lab/info-window query)

; ------------------------------------------------------------------------------
; Views
; ------------------------------------------------------------------------------

(defn str->keyword [& s]
  (keyword (apply str s)))

(defn display-addresses []
  (r/with-let [addresses (rf/subscribe [:sync-lab/addresses])]
    (when @addresses
      (rf/dispatch [:sync-lab/create-markers (filter #(not (string/blank? (:lat %))) @addresses)])
      [:div.card
       [:div.content
        [:button.btn.btn-primary
         {:on-click #(rf/dispatch [:sync-lab/geocode])}
         "Geocode"]
        [:table.table.table-striped
         [c/thead ["Bairro" "Tipo de logradouro" "Logradouro" "Latitude" "Longitude" "Show on map"]]
         [:tbody
          (map-indexed
           (fn [i row]
            (let [ns- (str "sync-lab.addresses." i ".")]
              ^{:key (:id row)}
              [:tr
               [:td.text-center
                [input {:type :text,
                        :name (str->keyword ns- "bairro")
                        :class "form-control"
                        :value (:bairro row)}]]
               [:td.text-center
                [input {:type :text,
                        :name (str->keyword ns- "logr-tipo")
                        :class "form-control"
                        :value (:logr-tipo row)}]]
               [:td.text-center
                [input {:type :text,
                        :name (str->keyword ns- "logr")
                        :class "form-control"
                        :value (:logr row)}]]
               [:td.text-center
                [input {:type :text,
                        :name (str->keyword ns- "lat")
                        :class "form-control"
                        :value (:lat row)}]]
               [:td.text-center
                [input {:type :text,
                        :name (str->keyword ns- "lng")
                        :class "form-control"
                        :value (:lng row)}]]
               [:td
                [:button.btn.btn-default
                 {:on-click #(rf/dispatch [:sync-lab/create-marker (str (:logr-tipo row) " " (:logr row) ", " (:bairro row)) (:lat row) (:lng row)])}
                 "Show on map"]]]))
           @addresses)]]]])))

(defn map-comp []
  (r/create-class
   {:display-name "sync-lab/map-comp"
    :reagent-render (fn []
                      [:div#map.center-block
                       {:style {:height "650px"}}])
    :component-did-mount #(rf/dispatch [:init-map [:sync-lab] %])}))

(defn upload-csv-file-form []
  [:div.card
   [:div.content
    [:div.row>div.col-md-12
     [:label "Upload a csv file"
      [:input {:type :file
               :on-change #(rf/dispatch [:sync-lab/process-input-file (-> % .-target .-files (aget 0))])}]]]]])

(defn sync-lab-panel- []
  [:div.card
   [:div.header
    [:h4.title "Sync-lab"]]
   [:div.content
    [upload-csv-file-form]
    [map-comp]
    [display-addresses]]])

(defn sync-lab-panel []
  (r/with-let [google-api-key (rf/subscribe [:settings/google-api-key])]
    [c/js-loader
     {:scripts {#(exists? js/google) (str "https://maps.googleapis.com/maps/api/js?"
                                          "key=" @google-api-key
                                          "&libraries=geometry,visualization")}
      :loading [:div.loading "Loading..."]
      :loaded [sync-lab-panel-]}]))

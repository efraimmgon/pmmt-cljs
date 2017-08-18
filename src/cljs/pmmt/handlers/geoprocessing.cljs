(ns pmmt.handlers.geoprocessing
  (:require
   [clojure.string :as string]
   [ajax.core :as ajax]
   day8.re-frame.http-fx
   [reagent.core :as r :refer [atom]]
   [re-frame.core :refer
    [reg-event-db reg-sub subscribe dispatch]]
   [pmmt.utils :as utils]
   [pmmt.validation :as v]))

; ---------------------------------------------------------------------
; Helpers
; ---------------------------------------------------------------------

(defn select-not-empty-keys [m ks]
  (let [m (select-keys m ks)]
    (apply dissoc
           m
           (for [[k v] m :when (nil? v)] k))))

(defn dispatch-n [& handlers]
  (doseq [handler handlers]
    (dispatch handler)))

; ---------------------------------------------------------------------
; Subscriptions
; ---------------------------------------------------------------------

;; For sortable table
(reg-sub
 :sorted-content
 (fn [query-v args]
   [(subscribe [:get-db :sort-key])
    (subscribe [:get-db :comp])
    (subscribe [:get-db :crime-reports])
    (subscribe [:get-db :ascending])])
 (fn [[sort-key comp crime-reports ascending] _]
   (let [sorted (sort-by sort-key comp crime-reports)]
     (if ascending
       sorted
       (rseq sorted)))))

; ---------------------------------------------------------------------
; Setup
; ---------------------------------------------------------------------

(def ^{:doc "local app state"}
  initial-state
  {:gmap nil
   :marker-type :basic-marker
   :heatmap nil
   :heatmap-data []
   ;; :basic-marker
   :markers nil
   :info-window nil
   :crime-reports-count {:total 0, :roubo 0, :furto 0, :droga 0, :homicidio 0, :outros 0}
   :crime-reports []
   ;; sortable table
   :show-table? false
   :sort-key :crime
   :comp compare
   :ascending true})

; ---------------------------------------------------------------------
; Specific Helpers
; ---------------------------------------------------------------------

(defn inc-crime-report! [n]
  (cond
    (string/includes? n "Roubo") (dispatch [:inc-count :roubo])
    (string/includes? n "Furto") (dispatch [:inc-count :furto])
    (string/includes? n "Homic") (dispatch [:inc-count :homicidio])
    (string/includes? n "Drogas") (dispatch [:inc-count :droga])
    :else (dispatch [:inc-count :outros]))
  (dispatch [:inc-count :total]))

;; Markers
; TODO OMS markers
(defn add-listiner! [marker event f]
  (.addListener js/google.maps.event marker event f))

(defn create-marker
  "Create a gmap marker and append it to the gmap instance
  google.maps.Marker takes two keys:
  - position which is a map with two keys: :lat and :lng
  - title which will be rendered as the marker title"
  [{:keys [position title]}]
  (let [marker (js/google.maps.Marker.
                 (clj->js {:position position, :title title}))
        gmap (subscribe [:get-db :gmap])
        info-window (subscribe [:get-db :info-window])]
    (.setMap marker @gmap)
    (add-listiner! marker "click" #(do (.setContent @info-window title)
                                       (.open @info-window @gmap marker)))
    (add-listiner! marker "mouseover" #(.setOpacity marker 0.5))
    (add-listiner! marker "mouseout" #(.setOpacity marker 1))
    (dispatch [:append-marker marker])))

(defn create-markers! [markers-data]
  (doseq [row markers-data]
    (create-marker
     {:position {:lat (:latitude row),
                 :lng (:longitude row)}
      ;; Title: crime, id, address
      :title (str "<b>"(:crime row) " [" (:id row) "]</b><br/>"
                  (string/join ", " (select-not-empty-keys row [:bairro :via :numero])))})))

(defn create-heatmap-layer! [crime-reports]
  (let [gmap (subscribe [:get-db :gmap])
        data (map (fn [row]
                    (js/google.maps.LatLng.
                     (clj->js (:latitude row))
                     (clj->js (:longitude row))))
                  crime-reports)
        heatmap-opts {:data data, :dissipating false, :map @gmap}
        heatmap (js/google.maps.visualization.HeatmapLayer.
                 (clj->js heatmap-opts))]
    (.set heatmap "radius" 1)
    (.set heatmap "scaleRadius" false)
    (dispatch [:assoc-db :heatmap heatmap])))

(defn clear-map!
  "Remove the markers, or heatmap layer, from the map"
  [db]
  (when-let [markers (:markers db)]
    (doseq [m markers]
      (.setMap m nil)))
  (when-let [heatmap (:heatmap db)]
    (.setMap heatmap nil)))

; ---------------------------------------------------------------------
; Handlers
; ---------------------------------------------------------------------

(defn create-marker-layer [marker-type data]
  (if (= marker-type :basic-marker)
    (create-markers! data)
    (create-heatmap-layer! data)))

(reg-event-db
 :map/initial-state
 (fn [db _]
   (merge db initial-state)))

(reg-event-db
 :append-crime-reports
 (fn [db [_ rows]]
   (let [updated-rows
         (map (fn [row]
                (inc-crime-report! (:crime row))
                ; TODO: update fields with nil when blank
                (-> row
                    (update :hora utils/long->time-str)
                    (update :data utils/long->date-str)
                    (assoc :weekday (utils/long->weekday (:data row)))))
              rows)]
     (assoc db :crime-reports updated-rows))))

;  takes an optional compare function
(reg-event-db
 :update-sort-engine
 (fn [db [_ options]]
   ; if sort-key == current :sort-key, keep order as is, otherwise switch to ascending
   (let [sort-key (:key options)
         ascending (if (= sort-key (:sort-key db))
                     (not (:ascending db))
                     true)
         comp (or (:fn options) compare)]
     (-> db
         (assoc :ascending ascending)
         (assoc :comp comp)
         (assoc :sort-key sort-key)))))

(reg-event-db
 :inc-count
 (fn [db [_ id]]
   (update-in db [:crime-reports-count id] inc)))

(reg-event-db
 :append-marker
 (fn [db [_ marker]]
   (update db :markers conj marker)))

(reg-event-db
 :clear-map
 (fn [db _]
   (clear-map! db)
   (-> db
       (assoc :markers nil))))

(reg-event-db
 :reset-map-state
 (fn [db _]
   (clear-map! db)
   (-> db
       (assoc :crime-reports [])
       (assoc :crime-reports-count nil)
       (assoc :markers nil))))

(reg-event-db
 :update-marker-type
 (fn [db [_ marker-type]]
   (let [map-data (:crime-reports db)
         current-marker (:marker-type db)]
     (dispatch [:remove-modal])
     ;; only do something if the selected marker is not the current one
     (if (not= marker-type current-marker)
       (if (not-empty map-data)
         ;; if there's some data we populate the new marker layer with it
         (do (dispatch [:clear-map])
             (create-marker-layer marker-type map-data)
             (assoc db :marker-type marker-type))
         ;; otherwise we just set the new marker type
         (assoc db marker-type marker-type))
       db))))

(reg-event-db
 :process-geo-dados
 (fn [db [_ response]]
   (dispatch [:append-crime-reports response])
   (dispatch [:remove-modal])
   (create-marker-layer (:marker-type db) response)
   (assoc db :show-table? true)))

(reg-event-db
 :query-geo-dados
 (fn [db [_ fields errors]]
   (if-let [error (v/validate-map-args @fields)]
     (do (reset! errors error)
         db)
     (do
       (ajax/GET "/analise-criminal/geo/dados"
                 {:params @fields
                  :handler #(dispatch [:process-geo-dados %])
                  :error-handler #(println (str %))})
       ;; clear map and other data
       (dispatch [:reset-map-state])
       ;; store query params in db
       (assoc db :geo-query-params @fields)))))

(defn init-gmap [db [_ comp]]
  (if (:gmap db)
    db
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
          (assoc :gmap gmap)
          (assoc :info-window info-window)
          (assoc :heatmap heatmap)))))

(reg-event-db
 :init-gmap
 init-gmap)

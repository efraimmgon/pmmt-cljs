(ns pmmt.handlers.geoprocessing
  (:require
   [clojure.string :as string]
   [ajax.core :as ajax]
   day8.re-frame.http-fx
   [reagent.core :as r :refer [atom]]
   [re-frame.core :as re-frame :refer
    [reg-event-db reg-event-fx reg-fx reg-sub subscribe dispatch dispatch-sync]]
   [pmmt.utils :as utils]
   [pmmt.validation :as v]))

; ---------------------------------------------------------------------
; Subscriptions
; ---------------------------------------------------------------------

;; For sortable table
(reg-sub
 :sorted-content
 (fn [query-v args]
   [(subscribe [:get-db :sort-key])
    (subscribe [:get-db :comp])
    (subscribe [:get-db :ocorrencias])
    (subscribe [:get-db :ascending])])
 (fn [[sort-key comp ocorrencias ascending] _]
   (let [sorted (sort-by sort-key comp ocorrencias)]
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
   :ocorrencias-count {:total 0, :roubo 0, :furto 0, :droga 0, :homicidio 0, :outros 0}
   :ocorrencias []
   ;; sortable table
   :show-table? false
   :sort-key :natureza
   :comp compare
   :ascending true})

; ---------------------------------------------------------------------
; Helpers
; ---------------------------------------------------------------------

(defn inc-ocorrencia! [n]
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
    (add-listiner! marker "click" #(do (.setContent @info-window) title
                                       (.open @info-window) @gmap marker))
    (add-listiner! marker "mouseover" #(.setOpacity marker 0.5))
    (add-listiner! marker "mouseout" #(.setOpacity marker 1))
    (dispatch [:append-marker marker])))

(defn create-markers! [markers-data]
  (doseq [row markers-data]
    (create-marker
     {:position {:lat (:latitude row),
                 :lng (:longitude row)}
      :title (str "<b>"(:natureza row) " [" (:id row) "]</b><br/>"
                  (string/join ", " [(:bairro row) (:via row) (:numero row)]))})))

(defn create-heatmap-layer! [ocorrencias]
  (let [gmap (subscribe [:get-db :gmap])
        data (map (fn [row]
                    (js/google.maps.LatLng.
                     (clj->js (:latitude row))
                     (clj->js (:longitude row))))
                  ocorrencias)
        heatmap-opts {:data data, :dissipating false, :map @gmap}
        heatmap (js/google.maps.visualization.HeatmapLayer.
                 (clj->js heatmap-opts))]
    (.set heatmap "radius" 1)
    (.set heatmap "scaleRadius" false)
    (dispatch [:assoc-db :heatmap heatmap])))

(defn clear-map! [db]
  (when-let [markers (:markers db)]
    (doseq [m markers]
      (.setMap m nil)))
  (when-let [heatmap (:heatmap db)]
    (.setMap heatmap nil)))

; ---------------------------------------------------------------------
; Handlers
; ---------------------------------------------------------------------

(reg-fx
 :create-marker-layer
 (fn [[marker-type data]]
   (if (= marker-type :basic-marker)
     (create-markers! data)
     (create-heatmap-layer! data))))

(reg-event-db
 :map/initial-state
 (fn [db _]
   (merge db initial-state)))

(reg-event-db
 :append-ocorrencias
 (fn [db [_ rows]]
   (let [updated-rows
         (map (fn [row]
                (inc-ocorrencia! (:natureza row))
                ; TODO: update fields with nil when blank
                (-> row
                    (update :hora utils/long->time-str)
                    (update :data utils/long->date-str)
                    (assoc :weekday (utils/long->weekday (:data row)))))
              rows)]
     (assoc db :ocorrencias updated-rows))))

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
   (update-in db [:ocorrencias-count id] inc)))

(reg-event-db
 :append-marker
 (fn [db [_ marker]]
   (update db :markers conj marker)))

(reg-event-fx
 :clear-map
 (fn [{:keys [db]} _]
   (clear-map! db)
   {:db (-> db
            (assoc :markers nil))}))

(reg-event-fx
 :reset-map-state
 (fn [{:keys [db]} _]
   (clear-map! db)
   {:db (-> db
            (assoc :ocorrencias [])
            (assoc :ocorrencias-count nil)
            (assoc :markers nil))}))

(reg-event-fx
 :update-marker-type
 (fn [{:keys [db]} [_ marker-type]]
   (let [ocorrencias (:ocorrencias db)
         current-marker (:marker-type db)]
     ;; only do something if the marker-types are different
     (if (not= marker-type current-marker)
       (if (not-empty ocorrencias)
         {:dispatch-n (list [:clear-map] [:remove-modal])
          :create-marker-layer [marker-type ocorrencias]
          :db (assoc db :marker-type marker-type)}
         {:dispatch [:remove-modal]
          :db (assoc db :marker-type marker-type)})
       {:dispatch [:remove-modal]
        :db db}))))

(reg-event-fx
 :process-geo-dados
 (fn [{:keys [db]} [_ response]]
   {:dispatch-n (list [:append-ocorrencias response] [:remove-modal])
    :create-marker-layer [(:marker-type db) response]
    :db (assoc db :show-table? true)}))

(reg-event-fx
 :query-geo-dados
 (fn [{:keys [db]} [_ fields errors]]
  (if-let [e (v/validate-map-args @fields)]
    {:reset [errors e]
     :db db}
    {:http-xhrio {:method :get
                  :uri "/analise-criminal/geo/dados"
                  :params @fields
                  :on-success [:process-geo-dados]
                  :response-format (ajax/json-response-format {:keywords? true})}
     :dispatch [:reset-map-state]
     ;; store query params in db
     :db (assoc db :geo-query-params @fields)})))

(defn init-gmap [{:keys [db]} [_ comp]]
  (if (:gmap db)
    {:db db}
    (let [map-canvas (r/dom-node comp)
          ;; default map values
          map-opts (clj->js {:center {:lat -11.855275, :lng -55.505966}
                             :zoom 14
                             :mapTypeid js/google.maps.MapTypeId.ROADMAP})
          ;;; initialize google maps assets
          heatmap (js/google.maps.visualization.HeatmapLayer.)
          gmap (js/google.maps.Map. map-canvas map-opts)
          info-window (js/google.maps.InfoWindow.)]
      {:db (-> db
               (assoc :gmap gmap)
               (assoc :info-window info-window)
               (assoc :heatmap heatmap))})))

(reg-event-fx
 :init-gmap
 init-gmap)

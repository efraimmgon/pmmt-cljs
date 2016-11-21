(ns pmmt.components.map
  (:require [clojure.string :as string]
            [ajax.core :as ajax]
            [reagent.core :as r :refer [atom]]
            [re-frame.core :as re-frame :refer
             [reg-event-db reg-event-fx reg-fx reg-sub subscribe dispatch dispatch-sync]]
            [day8.re-frame.http-fx]
            [pmmt.validation :as v]
            [pmmt.utils :as utils]))

(def ocorrencias-count
  {:total 0, :roubo 0, :furto 0, :droga 0, :homicidio 0, :outros 0})

(def ^{:doc "global app state"}
  initial-state
  {:gmap nil
   :marker-type :basic-marker
   :heatmap nil
   :heatmap-data []
   ;; :basic-marker
   :markers nil
   :info-window nil
   :ocorrencias-count ocorrencias-count
   :ocorrencias []
   ;; sortable table
   :show-table? false
   :sort-val :natureza
   :comp compare
   :ascending true})

; helper fns ------------------------------------------------------------

(defn inc-ocorrencia! [n]
  (cond
    (string/includes? n "Roubo") (dispatch [:inc-count :roubo])
    (string/includes? n "Furto") (dispatch [:inc-count :furto])
    (string/includes? n "Homic") (dispatch [:inc-count :homicidio])
    (string/includes? n "Drogas") (dispatch [:inc-count :droga])
    :else (dispatch [:inc-count :outros]))
  (dispatch [:inc-count :total]))

; Events ---------------------------------------------------------------

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

(reg-event-db
 :update-sort-value
 (fn [db [_ new-val & [comp]]]
   ; if new-val == current-val, keep order as is, otherwise switch to ascending
   (let [ascending (if (= new-val (:sort-val db))
                     (not (:ascending db))
                     true)
         comp (or comp (:comp db))]
     (-> db
         (assoc :ascending ascending)
         (assoc :comp comp)
         (assoc :sort-val new-val)))))

(reg-event-db
 :inc-count
 (fn [db [_ id]]
   (update-in db [:ocorrencias-count id] inc)))

(reg-event-db
 :append-marker
 (fn [db [_ marker]]
   (update db :markers conj marker)))

(defn clear-map! [db]
  (when-let [markers (:markers db)]
    (doseq [m markers]
      (.setMap m nil)))
  (when-let [heatmap (:heatmap db)]
    (.setMap heatmap nil)))

;; todo: refactor side effects
(reg-event-fx
 :clear-map
 (fn [{:keys [db]} _]
   (clear-map! db)
   {:db (-> db
            (assoc :markers nil))}))

;; todo: refactor side effects
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

(defn init-gmap [{:keys [db]} [_ this]]
  (if (:gmap db)
    {:db db}
    (let [map-canvas (r/dom-node this)
          map-opts (clj->js {:center {:lat -11.855275, :lng -55.505966}
                             :zoom 14
                             :mapTypeid js/google.maps.MapTypeId.ROADMAP})
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

; Subscriptions ---------------------------------------------------------

;; For sortable table
(reg-sub
 :sorted-content
 ; eitheir form is fine:
 ; (fn [query-v args]
 ;   [(subscribe [:get-db :sort-val])
 ;    (subscribe [:get-db :comp])
 ;    (subscribe [:get-db :ocorrencias])
 ;    (subscribe [:get-db :ascending])])
 ; or:
 :<- [:get-db :sort-val]
 :<- [:get-db :comp]
 :<- [:get-db :ocorrencias]
 :<- [:get-db :ascending]
 (fn [[sort-val comp ocorrencias ascending] _]
   (let [sorted (sort-by sort-val comp ocorrencias)]
     (if ascending
       sorted
       (rseq sorted)))))

; Sortable table ----------------------------------------------------------
; compare functions
(defn duration-comp [a b]
  (cond
    (= a "null") 1
    (= b "null") -1
    :else
    (let [tuple-a (string/split a ":")
          tuple-b (string/split b ":")]
      (- (-> (js/Number (tuple-a 0))
             (* 60)
             (+ (js/Number (tuple-a 1))))
         (-> (js/Number (tuple-b 0))
             (* 60)
             (+ (js/Number (tuple-b 1))))))))

(defn date-comp [a b]
  (compare (utils/str->date a)
           (utils/str->date b)))

(defn weekday-comp [a b]
  (let [comparable #(cond
                      (= "Segunda" %) 1
                      (= "Terça" %) 2
                      (= "Quarta" %) 3
                      (= "Quinta" %) 4
                      (= "Sexta" %) 5
                      (= "Sábado" %) 6
                      (= "Domingo" %) 7)]
    (compare (comparable a)
             (comparable b))))

; Auxiliary html ---------------------------------------------------------

(defn display-query-params []
  (let [query-params (subscribe [:get-db :geo-query-params])]
        ;; todo: user friendly cities and nats
        ; cities (subscribe [:cities])
        ; naturezas (subscribe [:naturezas])]
    (fn []
      [:div
       [:h3 "Parâmetros"]
       (into
        [:div]
        (map (fn [[k v]]
               [:span {:style {:padding-right "30px"}}
                (str (name k) ": " v)])
             (seq @query-params)))])))

(defn display-ocorrencias-count []
  (let [ocorrencias-count (subscribe [:get-db :ocorrencias-count])
        total (:total @ocorrencias-count)
        roubo (:roubo @ocorrencias-count)
        furto (:furto @ocorrencias-count)
        droga (:droga @ocorrencias-count)
        homicidio (:homicidio @ocorrencias-count)
        outros (:outros @ocorrencias-count)]
    (into [:div]
          (map (fn [len text]
                 (when (pos? len)
                   ^{:key (gensym)}
                   [:span {:style {:padding-right "30px"}}
                    text]))
               [total roubo furto droga homicidio outros]
               [(str "Total: " total)
                (str "Roubos: " roubo)
                (str "Furtos: " furto)
                (str "Entorpecentes: " droga)
                (str "Homicídios: " homicidio)
                (str "Outros: " outros)]))))

(defn table-body []
  (let [sorted-content (subscribe [:sorted-content])]
    (fn []
      (into [:tbody]
        (map (fn [ocorrencia class]
               ^{:key ocorrencia}
               [:tr {:class class}
                [:td (:natureza ocorrencia)]
                [:td (:bairro ocorrencia)]
                [:td (:via ocorrencia)]
                [:td (:data ocorrencia)]
                [:td (:weekday ocorrencia)]
                [:td (:hora ocorrencia)]])
             @sorted-content
             (cycle ["odd" "even"]))))))

(defn table []
  [:div
    [display-query-params]
    [:h3 "Ocorrências registradas"]
    [display-ocorrencias-count]
    [:table.sortable
     [:thead>tr
      [:th {:on-click #(dispatch [:update-sort-value :natureza])}
       "Natureza"]
      [:th {:on-click #(dispatch [:update-sort-value :bairro])}
       "Bairro"]
      [:th {:on-click #(dispatch [:update-sort-value :via])}
       "Via"]
      [:th {:on-click #(dispatch [:update-sort-value :data date-comp])}
       "Data"]
      [:th {:on-click #(dispatch [:update-sort-value :weekday weekday-comp])}
       "Dia da semana"]
      [:th {:on-click #(dispatch [:update-sort-value :hora duration-comp])}
       "Hora"]]
     [table-body]]])

; main component

(defn map-inner []
  (r/create-class {:display-name "map-container"
                   :reagent-render (fn []
                                     [:div#map.center-block
                                      {:style {:height "650px"}}])
                   :component-did-mount #(dispatch [:init-gmap %])}))

(defn map-outer []
  (dispatch-sync [:map/initial-state])
  #(do
    [map-inner]))

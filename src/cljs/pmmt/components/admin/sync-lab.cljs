(ns pmmt.components.admin.sync-lab
  (:require
   [clojure.string :as string]
   [clojure.walk :as walk]
   [goog.labs.format.csv :as csv]
   [reagent.core :as r :refer [atom]]
   [re-frame.core :as rf]
   [pmmt.pages.components :refer [input]]
   [pmmt.components.common :as c]))

(defn csv->map [csv-data]
  (let [[header & rows] csv-data]
    (map (fn [row]
           (-> (zipmap
                (map (comp string/lower-case #(string/replace % #" " "-")) header)
                row)
               (walk/keywordize-keys)))
         rows)))

; TODO:
; [input csv file with neighborhoods, routes, lats and lats ready]
;
; [load csv file] [ok]
;
; [parse the csv: {:bairro neighborhood, :via street, :lat nil, :lng nil}]
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

(rf/reg-event-db
 :sync-lab/load-addresses
 (fn [db [_ csv-]]
   (assoc-in db [:sync-lab :addresses]
             (map-indexed (fn [i row] (assoc row :id i))
                          csv-))))

(rf/reg-event-fx
 :sync-lab/process-input-file
 (fn [db [_ file]]
   (let [reader (js/FileReader.)]
     (set! (.-onload reader)
           #(rf/dispatch [:sync-lab/load-addresses (-> % .-target .-result csv/parse js->clj csv->map)]))
     (.readAsText reader file))
   nil))

(rf/reg-sub
 :sync-lab/addresses
 (fn [db]
   (get-in db [:sync-lab :addresses])))

(defn display-addresses []
  (r/with-let [addresses (rf/subscribe [:sync-lab/addresses])]
    (when @addresses
      [:div.card
       [:div.content
        [:table.table.table-striped
         [c/thead ["Bairro" "Tipo de logradouro" "Logradouro" "Latitude" "Longitude"]]
         [:tbody
          (for [row @addresses]
            ^{:key (:id row)}
            [:tr
             [:td.text-center
              [input {:type :text, 
                      :class "form-control"
                      :value (:bairro row)}]]
             [:td.text-center
              [input {:type :text, 
                      :class "form-control"
                      :value (:logr-tipo row)}]]
             [:td.text-center
              [input {:type :text, 
                      :class "form-control"
                      :value (:logr row)}]]
             [:td.text-center
              [input {:type :text, 
                      :class "form-control"
                      :value (:lat row)}]]
             [:td.text-center
              [input {:type :text, 
                      :class "form-control"
                      :value (:lng row)}]]])]]]])))

(defn map-comp []
  (r/create-class
   {:display-name "sync-lab/map-comp"
    :reagent-render (fn []
                      [:div#map.center-block
                       {:style {:height "650px"}}])
    :component-did-mount #(rf/dispatch [:init-gmap %])}))

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
    ;[map-comp]
    [display-addresses]]])



(defn sync-lab-panel []
  (r/with-let [google-api-key (rf/subscribe [:settings/google-api-key])]
    [c/js-loader
     {:scripts {#(exists? js/google) (str "https://maps.googleapis.com/maps/api/js?"
                                          "key=" @google-api-key
                                          "&libraries=geometry,visualization")}
      :loading [:div.loading "Loading..."]
      :loaded [sync-lab-panel-]}]))

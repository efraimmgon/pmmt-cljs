(ns pmmt.components.admin.sync-lab
  (:require
   [clojure.string :as string]
   [reagent.core :as r :refer [atom]]
   [re-frame.core :as rf]
   [pmmt.components.admin.sync-lab.handlers :refer [markable-address?]]
   [pmmt.components.common :as c]
   [pmmt.pages.components :refer [input form-group pretty-display]]
   [pmmt.utils :refer [str->keyword <sub]]))

; ------------------------------------------------------------------------------
; Utils
; ------------------------------------------------------------------------------

; TODO fetch :lat and :lng from db
(defn add-default-position [row]
  (assoc row :lat -11.855275
             :lng -55.505966))

; ------------------------------------------------------------------------------
; Views
; ------------------------------------------------------------------------------

(defn addresses-actions [addresses]
  (r/with-let [selected (rf/subscribe [:sync-lab/selected-addresses])
               markers (rf/subscribe [:sync-lab/markers])]
    [:div
     [:button.btn.btn-info
      {:on-click #(rf/dispatch [:sync-lab/select-all-addresses])}
      (if (<sub [:sync-lab/all-selected?])
        "Deselect all"
        "Select all")] " "
     [:button.btn.btn-primary
      {:on-click #(rf/dispatch [:sync-lab/geocode])
       :class (when (empty? @selected) "disabled")}
      "Geocode selected"] " "
     [:button.btn.btn-default
      {:on-click #(rf/dispatch [:sync-lab/map-selected-addresses])
       :class (when (empty? @selected) "disabled")}
      "Show selected on map"] " "
     [:button.btn.btn-danger
      {:on-click #(rf/dispatch [:sync-lab/clear-map])
       :class (when (empty? @markers) "disabled")}
      "Clear map"] " "
     [:a.btn.btn-success
      {:href (<sub [:sync-lab/downloadable-addresses-url])
       :download "bairros+ruas+lat+lng.csv"}
      "Download table data"]]))

(defn addresses-table [addresses]
  [:table.table.table-striped
   [c/thead ["Select" "Bairro" "Tipo de logradouro" "Logradouro" "Latitude" "Longitude" "Show on map"]]
   [:tbody
    (doall
      (map-indexed
       (fn [i row]
        (let [ns- (str "sync-lab.addresses." i ".")]
          ^{:key (:id row)}
          [:tr
           [:td
            [input {:type :checkbox
                    :name (str->keyword ns- "selected?")
                    :class "form-control"
                    :on-change #(rf/dispatch [:update-state [:sync-lab :addresses i :selected?] not])
                    :checked (:selected? row)
                    :value (:id row)}]]
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
            [input {:type :number,
                    :name (str->keyword ns- "lat")
                    :class "form-control"
                    :value (:lat row)}]]
           [:td.text-center
            [input {:type :number,
                    :name (str->keyword ns- "lng")
                    :class "form-control"
                    :value (:lng row)}]]
           [:td
            [:button.btn.btn-default
             {:on-click #(rf/dispatch
                          [:sync-lab/create-markers
                           [(if (markable-address? row)
                              row
                              (add-default-position row))]])}
             "Show on map"]]]))
       @addresses))]])

(defn addresses-component []
  (r/with-let [addresses (rf/subscribe [:sync-lab/addresses])
               markers (rf/subscribe [:query :sync-lab.markers])
               gMap (rf/subscribe [:query :sync-lab.gmap])]
    ; The google maps canvas is initialized every time the component is
    ; reloaded, losing all data. We set the saved marker instances to the
    ; saved gmap instance to display them.
    (when (seq @markers)
      (doseq [marker @markers]
        (.setMap marker @gMap)))
    (when @addresses
      [:div.card
       [:div.content
        [addresses-actions addresses]
        [addresses-table addresses]]])))

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
    [addresses-component]]])

(defn sync-lab-panel []
  (r/with-let [google-api-key (rf/subscribe [:settings/google-api-key])]
    [c/js-loader
     {:scripts {#(exists? js/google) (str "https://maps.googleapis.com/maps/api/js?"
                                          "key=" @google-api-key
                                          "&libraries=geometry,visualization")}
      :loading [:div.loading "Loading..."]
      :loaded [sync-lab-panel-]}]))

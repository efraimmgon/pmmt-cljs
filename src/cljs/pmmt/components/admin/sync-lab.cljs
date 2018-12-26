(ns pmmt.components.admin.sync-lab
  (:require
   [clojure.string :as string]
   [laconic.utils.core :refer [with-deps]]
   [reagent.core :as r :refer [atom]]
   [re-frame.core :as rf]
   [reframe-forms.core :refer [input]]
   [pmmt.components.admin.sync-lab.handlers :refer [markable-address?]]
   [pmmt.components.common :as c]
   [pmmt.pages.components :refer [form-group pretty-display]]
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

; ------------------------------------------------------------------------------
; Pager

(defn forward
  "Tick current page forward."
  [i pages]
  (if (< i (dec pages))
    (inc i)
    i))

(defn back
  "Tick current page backward-"
  [i]
  (if (pos? i)
    (dec i)
    i))

(defn nav-link
  "Renders the pager link to a page."
  [page i]
  [:li.page-item {:class (when (= i @page) "active")}
   [:a.page-link
    {:on-click #(rf/dispatch [:set :sync-lab/current-page i])
     :href "#"}
    [:span i]]])

(defn pager
  "pager (pagination) component.
  Takes the page count and current page."
  [page-count page]
  (when (> page-count 1)
    (into
      [:div.text-xs-center>ul.pagination]
      (concat
        ;; back button
        [[:li.page-item>a.page-link
          {:on-click #(swap! page back page-count)
           :class (when (= @page 0) "disabled")}
          [:span "<<"]]]
        ;; navigation links for the range of page count
        (map (partial nav-link page) (range page-count))
        ;; forward button
        [[:li.page-item>a.page-link
          {:on-click #(swap! page forward page-count)
           :class (when (= @page (dec page-count)) "disabled")}
          [:span ">>"]]]))))

; ------------------------------------------------------------------------------
; Addresses table

(defn addresses-actions []
  (r/with-let [selected (rf/subscribe [:sync-lab/selected-addresses])
               markers (rf/subscribe [:sync-lab/markers])]
    [:div
     [:button.btn.btn-info
      {:on-click #(rf/dispatch [:sync-lab/toggle-selected-addresses])}
      (if (<sub [:sync-lab/all-selected?])
        "Deselect all"
        "Select all")] " "
     [:button.btn.btn-info
      {:on-click #(rf/dispatch [:sync-lab/toggle-current-page])}
      "Toggle current page"] " "
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
    (for [[i row] (map vector (range) addresses)
          :let [path [:sync-lab :addresses (:id row)]]]
      ^{:key (:id row)}
      [:tr
       [:td
        [input {:type :checkbox
                :name (conj path :selected?)
                :class "form-control"}]]
       [:td.text-center
        [input {:type :text
                :name (conj path :bairro)
                :class "form-control"}]]
       [:td.text-center
        [input {:type :text
                :name (conj path :logr-tipo)
                :class "form-control"}]]
       [:td.text-center
        [input {:type :text
                :name (conj path :logr)
                :class "form-control"}]]
       [:td.text-center
        [input {:type :number
                :name (conj path :lat)
                :class "form-control"}]]
       [:td.text-center
        [input {:type :number
                :name (conj path :lng)
                :class "form-control"}]]
       [:td
        [:button.btn.btn-default
         {:on-click #(rf/dispatch
                      [:sync-lab/create-markers
                       [(if (markable-address? row)
                          row
                          (add-default-position row))]])}
         "Show on map"]]])]])

(defn addresses-component []
  (r/with-let [;addresses (rf/subscribe [:sync-lab/addresses])
               partitioned-addresses (rf/subscribe [:sync-lab/partitioned-addresses])
               markers (rf/subscribe [:query :sync-lab.markers])
               gMap (rf/subscribe [:query :sync-lab.gmap])
               current-page (rf/subscribe [:sync-lab/current-page])]
    ; The google maps canvas is initialized every time the component is
    ; reloaded, losing all data. We set the saved marker instances to the
    ; saved gmap instance to display them.
    (when (seq @markers)
      (doseq [marker @markers]
        (.setMap marker @gMap)))
    ;; Need to call `vec`, since we'll be calling `nth` on it
    (when (seq @partitioned-addresses)
      [:div.card
       [:div.content
        ;[c/pretty-display @(rf/subscribe [:query [:page/loaded-deps]])]
        ;[c/pretty-display @(rf/subscribe [:query [:sync-lab :addresses 0]])]
        [addresses-actions]
        [pager (count @partitioned-addresses) current-page]
        [addresses-table (get @partitioned-addresses @current-page)]
        [pager (count @partitioned-addresses) current-page]]])))

; ------------------------------------------------------------------------------
; Comps

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
     [:label "Upload a csv file (headers: logr-tipo logr bairro)"
      [:input {:type :file
               :on-change #(rf/dispatch [:sync-lab/process-input-file
                                         (-> % .-target .-files (aget 0))])}]]]]])

(defn sync-lab-panel-
  "Main component's content."
  []
  [:div.card
   [:div.header
    [:h4.title "Sync-lab"]]
   [:div.content
    [upload-csv-file-form]
    [map-comp]
    [addresses-component]]])
        
(defn sync-lab-panel
  "Main component."
  []
  (r/with-let [google-api-key (rf/subscribe [:settings/google-api-key])]
    [with-deps
     {:deps [{:id "google-maps-js"
              :type "text/javascript"
              :src (str "https://maps.googleapis.com/maps/api/js?"
                        "key=" @google-api-key
                        "&libraries=geometry,visualization")}]
      :loading [:div.loading "Loading..."]
      :loaded [sync-lab-panel-]}]))
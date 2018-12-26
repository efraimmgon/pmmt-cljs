(ns pmmt.components.common
  (:require
   [dommy.core :as dommy :refer-macros [sel sel1]]
   [reagent.core :as r :refer [atom]]
   [re-frame.core :as rf :refer [dispatch]]))

; --------------------------------------------------------------------
; Debugging
; --------------------------------------------------------------------

(defn pretty-display [data]
  [:div
   [:pre
    (with-out-str
     (cljs.pprint/pprint data))]])

; --------------------------------------------------------------------
; MISC
; --------------------------------------------------------------------

(defn modal [header body footer]
  [:div
   [:div.modal-dialog
    [:div.modal-content
     [:div.modal-header [:h3 header]]
     [:div.modal-body body]
     [:div.modal-footer
      [:div.bootstrap-dialog-footer
       footer]]]]
   [:div.modal-backdrop.fade.in]])

(defn display-error [errors id]
  (when-let [error (id @errors)]
    [:div.alert.alert-danger (clojure.string/join error)]))

(defn card [{:keys [title subtitle content footer]}]
  [:div.card
   [:div.header
    [:h4.title title]
    (when subtitle
      [:p.category subtitle])]
   [:div.content
    content]
   (when footer
     [:div.footer footer])])

(defn tabs
  "Plain text tabs.
   `items` is a coll of maps with keys :title and :body"
  [items]
  (r/with-let [ids (map #(gensym) (range (count items)))]
    [:div
     (into
       [:ul.nav.nav-tabs
        {:role "tablist"}]
       ;;; tab title
       (map
        (fn [i item id]
          [:li
           (when (zero? i)
             {:class "active"
              :role "presentation"})
           [:a {:data-toggle "tab", :href (str "#" id)}
            (:title item)]])
        (range) items ids))
     ;;; tab body
     (into
       [:div.tab-content]
       (map
        (fn [i {:keys [body]} id]
          [:div.tab-pane
           {:id id
            :class (when (zero? i) "active")}
           body])
        (range) items ids))]))

; --------------------------------------------------------------------
; Charts
; --------------------------------------------------------------------

(defn chart [opts]
  (r/create-class
   {:display-name (:id opts)
    :reagent-render
    (fn [] [:canvas.ct-chart {:id (:id opts)}])
    :component-did-mount #(dispatch [:charts/plot-chart opts])
    :component-did-update #(dispatch [:charts/plot-chart opts])}))


; --------------------------------------------------------------------
; BUTTON
; --------------------------------------------------------------------

(defn nav-button [{:keys [handler title]}]
  [:span
   [:button.btn.btn-default
    {:on-click handler}
    title]
   " "])

; --------------------------------------------------------------------
; FORMS
; --------------------------------------------------------------------

; Component parts

(defn select-input [id options fields]
  ;; set default value
  (swap! fields assoc id (-> options first :value))
  [:select.form-control
   {:id id
    :style {:height "43px"}
    :name id
    :on-change #(swap! fields assoc id (-> % .-target .-value))}
   (for [{:keys [value display]} options]
     ^{:key value}
     [:option
      {:value (str value)}
      display])])

(defn input [type id placeholder fields]
  [:input.form-control
   {:type type
    :placeholder placeholder
    :value (id @fields)
    :on-change #(swap! fields assoc id (-> % .-target .-value))}])

(defn form-input [type label id placeholder fields optional?]
  [:div.form-group
   [:label label]
   (if optional?
     [input type id placeholder fields]
     [:div.input-group
      [input type id placeholder fields]
      [:span.input-group-addon
       "*"]])])

(defn checkbox-input-group [label id value fields]
  (let [checked? (atom false)]
    [:label
      [:input
       {:type :checkbox,
        :value value,
        :on-change #(do (swap! checked? not)
                        (if @checked?
                          (swap! fields assoc id (-> % .-target .-value))
                          (swap! fields dissoc id)))}]
     label]))

; Components

;; Text

(defn text-input [label id placeholder fields & [optional?]]
  (form-input :text label id placeholder fields optional?))

;; Number

(defn number-input [label id placeholder fields & [optional?]]
  (form-input :number label id placeholder fields optional?))

;; Password

(defn password-input [label id placeholder fields & [optional?]]
  (form-input :password label id placeholder fields optional?))

;; Checkbox

(defn checkbox-input [label id value fields & [optional?]]
  [:div.checkbox
   (if optional?
     [checkbox-input-group label id value fields]
     [:div.input-group
      checkbox-input-group label id value fields
      [:span.input-group-addon
       "*"]])])

;; Select

(defn select-form [label id options fields & [optional?]]
  [:div.form-group
   [:label {:for id} label]
   (if optional?
     [select-input id options fields]
     [:div.input-group
      [select-input id options fields]
      [:span.input-group-addon
       "*"]])])

; --------------------------------------------------------------------
; PAGER
; --------------------------------------------------------------------

(defn forward [i pages]
  (if (< i (dec pages)) (inc i) i))

(defn back [i]
  (if (pos? i) (dec i) i))

(defn nav-link [page i]
  [:li.page-item>a.page-link.btn.btn-primary
   {:on-click #(reset! page i)
    :class (when (= i @page) "active")}
   [:span i]])

(defn pager [pages page]
  (when (> pages 1)
    (into
     [:div.text-xs-center>ul.pagination.pagination-lg]
     (concat
      [[:li.page-item>a.page-link.btn
        {:on-click #(swap! page back pages)
         :class (when (= @page 0) "disabled")}
        [:span "<<"]]]
      (map (partial nav-link page) (range pages))
      [[:li.page-item>a.page-link.btn
        {:on-click #(swap! page forward pages)
         :class (when (= @page (dec pages)) "disabled")}
        [:span ">>"]]]))))

; table ------------------------------------------------------------

(defn thead [headers]
  [:thead
   [:tr
    (for [th headers]
      ^{:key th}
      [:th.text-center th])]])

(defn tbody [rows]
  (into
   [:tbody]
   (for [row rows]
     (into
      [:tr]
      (for [td row]
        [:td.text-center
         td])))))
(defn thead-indexed
  "Coupled with `tbody-indexed`, allocates a col for the row's index."
  [headers]
  [:thead
   (into
     [:tr
      [:th.text-center "Ord."]]
     (for [th headers]
       [:th.text-center th]))])

(defn tbody-indexed
  "Coupled with `thead-indexed`, allocates a col for the row's index."
  [rows]
  (into
   [:tbody]
   (map-indexed
    (fn [i row]
      (into
       [:tr [:td.text-center (inc i)]]
       (for [td row]
         [:td.text-center
          td])))
    rows)))


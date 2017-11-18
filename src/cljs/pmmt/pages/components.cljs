(ns pmmt.pages.components
  (:require
   [cljs.reader :as reader]
   [pmmt.utils :refer [extract-ns-and-name <sub]]
   [reagent.core :as r :refer [atom]]
   [re-frame.core :as rf]))

; ------------------------------------------------------------------------------
; Debugging
; ------------------------------------------------------------------------------

(defn pretty-display [data]
  [:div
   [:pre
    (with-out-str
     (cljs.pprint/pprint @data))]])

; ------------------------------------------------------------------------------
; Forms
; ------------------------------------------------------------------------------

; Helpers ----------------------------------------------------------------------
(defn- keyword-or-int [x]
  (if (int? (js/parseInt x))
    (js/parseInt x)
    (keyword x)))

(defn- checkbox-input [attrs]
  (let [name (:name attrs)
        acc (rf/subscribe [:query name])
        ks (extract-ns-and-name name)
        f (fn [acc]
            (let [val (:value attrs)]
              (cond
                (nil? acc) #{val}
                (contains? acc val) (disj acc val)
                :default (conj acc val))))
        attrs+defaults
        (-> attrs
            (assoc :on-change
                   (fn [comp]
                     (rf/dispatch [:update-state ks f])))
            (update :default-checked
                    #(or %
                         (when (contains? @acc (:value attrs))
                           true))))]
    [:input attrs+defaults]))

(defn- radio-input [attrs]
  (let [name (:name attrs)
        ks (extract-ns-and-name name)
        attrs+defaults
        (-> attrs
            (assoc :on-change
                   (fn [comp]
                     (rf/dispatch [:set-state ks (:value attrs)]))))]
    [:input attrs+defaults]))

(defn- number-input [attrs]
  (let [ks (extract-ns-and-name (:name attrs))
        attrs+defaults
        (-> attrs
            (assoc :on-change
                   (fn [comp]
                     (rf/dispatch [:set-state ks (-> comp .-target .-value reader/read-string)]))))]
    [:input attrs+defaults]))

; Core -------------------------------------------------------------------------

(defn form [& body]
  (into
   [:div]
   body))

(defn form-group [label & input]
  [:div.form-group
   [:label label]
   (into
    [:div]
    input)])

(defn input [attrs]
  (let [ks (extract-ns-and-name (:name attrs))
        attrs-defaults
        (-> attrs
            (update :on-change
                    #(or % (fn [comp] (rf/dispatch [:set-state ks (-> comp .-target .-value)])))))]
    (condp = (:type attrs)
           :checkbox [checkbox-input attrs]
           :radio [radio-input attrs-defaults]
           :number [number-input attrs-defaults]

           ;; default
           (do (println "I'm default")
               [:input attrs-defaults]))))

(defn textarea [attrs]
  (let [ks (extract-ns-and-name (:name attrs))
        attrs-defaults
        (-> attrs
            (update :on-change
                    #(or % (fn [comp] (rf/dispatch [:set-state ks (-> comp .-target .-value)])))))]
    [:textarea attrs-defaults]))

;;; with the current implementation it does not support options with
;;; strings as values.
(defn select [attrs & options]
  (let [ks (extract-ns-and-name (:name attrs))
        default-val (-> options ffirst second :value)
        attrs-defaults
        (-> attrs
            ;; `(-> % .-target .-value)` returns strings no matter what
            ;; we're trying to do go around that
            (update :on-change
                    #(or %
                         (fn [comp]
                           (rf/dispatch [:set-state ks (-> comp .-target .-value reader/read-string)]))))
            (update :value
                    #(or %
                         (do (rf/dispatch [:set-state ks default-val])
                             default-val))))]
    (into
     [:select attrs-defaults]
     options)))

; ------------------------------------------------------------------------------
; MISC
; ------------------------------------------------------------------------------

(defn breadcrumbs [& items]
  (into
   [:ol.breadcrumb
    [:li [:a {:href "/"} "Home"]]]
   (for [{:keys [href title active?] :as item} items]
     (if active?
       [:li.active title]
       [:li [:a {:href href} title]]))))

(defn base [& body]
  (into
   [:div.container]
   body))

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
        [:td.text-center td])))))

(defn thead-indexed
  "Coupled with `tbody-indexed`, allocates a col for the row's index."
  [headers]
  [:thead
   (into
     [:tr
      [:th.text-center "#"]]
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

(defn thead-editable [headers]
  [thead
   (conj headers "Edit" "Delete")])

(ns pmmt.pages.admin.dashboard
  (:require
   [reagent.core :as r]
   [re-frame.core :refer [subscribe dispatch]]))

; -----------------------------------------------------------------------
; Charts
; -----------------------------------------------------------------------

(defn crime-reports-by-crime-type-chart [crime-reports]
  (r/create-class
   {:display-name "crime-reports-by-crime-type-template"
    :reagent-render
    (fn []
      [:div#crime-reports-by-crime-type.ct-chart.ct-perfect-fourth])
    :component-did-mount
    #(dispatch [:charts/plot-pie-chart %])
    :component-did-update
    #(dispatch [:charts/plot-pie-chart %])}))

(defn crime-reports-by-crime-type-template []
  (r/with-let [crime-reports-by-crime-type (subscribe [:admin.crime-reports/by-crime-type])]
    [:div.card
     [:div.header
      [:h4.title "Crime Reports"]
      [:p.category "by Crime Type"]]
     [:div.content
      [crime-reports-by-crime-type-chart
       [(range 1 (count @crime-reports-by-crime-type))
        (map :count @crime-reports-by-crime-type)]]]
     [:div.footer
      [:div.legend
       [:h6 "Legend"]
       (into [:ol]
         (map
          (fn [row]
            [:li (:crime_type row)])
          @crime-reports-by-crime-type))]]]))

(defn crime-reports-by-month-chart [crime-reports]
  (r/create-class
   {:display-name "crime-reports-by-crime-type-template"
    :reagent-render
    (fn []
      [:div#crime-reports-by-month.ct-chart])
    :component-did-mount
    #(dispatch [:charts/plot-line-chart %])
    :component-did-update
    #(dispatch [:charts/plot-line-chart %])}))

(defn crime-reports-by-month-template []
  (r/with-let [crime-reports-by-month (subscribe [:admin.crime-reports/by-month])]
    [:div.card
     [:div.header
      [:h4.title "Crime Reports"]
      [:p.category "by Month"]]
     [:div.content
      [crime-reports-by-month-chart
       [(map #(inc (.getMonth (:month %))) @crime-reports-by-month)
        [(map :count @crime-reports-by-month)]]]]]))

(defn crime-reports-by-hour-chart [crime-reports]
  (r/create-class
   {:display-name "crime-reports-by-crime-type-template"
    :reagent-render
    (fn []
      [:div#crime-reports-by-hour.ct-chart])
    :component-did-mount
    #(dispatch [:charts/plot-bar-chart %])
    :component-did-update
    #(dispatch [:charts/plot-bar-chart %])}))

(defn crime-reports-by-hour-template []
  (r/with-let [crime-reports-by-hour (subscribe [:admin.crime-reports/by-hour])]
    [:div.card
     [:div.header
      [:h4.title "Crime Reports"]
      [:p.category "by Hour"]]
     [:div.content
      [crime-reports-by-hour-chart
       [(map :hour @crime-reports-by-hour)
        [(map :count @crime-reports-by-hour)]]]]]))

; -----------------------------------------------------------------------
; Content
; -----------------------------------------------------------------------

(defn content []
  (dispatch [:api/get-crime-reports-by-crime-type 2017])
  (dispatch [:api/get-crime-reports-by-month 2017])
  (dispatch [:api/get-crime-reports-by-hour 2017])
  (fn []
    [:div.content
     [:div.container-fluid
      [:div.row
       [:div.col-md-6
        [crime-reports-by-crime-type-template]]
       [:div.col-md-6
        [crime-reports-by-month-template]]]
      [:div.row
       [:div.col-md-12
        [crime-reports-by-hour-template]]]]]))

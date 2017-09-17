(ns pmmt.pages.admin.dashboard
  (:require
   [reagent.core :as r]
   [re-frame.core :refer [subscribe dispatch]]
   [pmmt.components.common :refer [card chart]]))

; -----------------------------------------------------------------------
; Charts
; -----------------------------------------------------------------------

(defn crime-reports-by-crime-type-template []
  (r/with-let [crime-reports-by-crime-type (subscribe [:admin.crime-reports/by-crime-type])]
    [card
     {:title "Crime Reports"
      :subtitle "by Crime Type"
      :content
      [chart
       {:display-name "chart-reports-by-crime-type"
        :chart-type "pie"
        :data [(range 1 (count @crime-reports-by-crime-type))
               (map :count @crime-reports-by-crime-type)]}]
      :footer
      [:div.legend
       [:h6 "Legend"]
       (into [:ol]
         (map
          (fn [row]
            [:li (:crime-type row)])
          @crime-reports-by-crime-type))]}]))

(defn crime-reports-by-month-template []
  (r/with-let [crime-reports-by-month (subscribe [:admin.crime-reports/by-month])]
    [card
     {:title "Crime Reports"
      :subtitle "by Month"
      :content [chart
                {:display-name "chart-reports-by-month"
                 :chart-type "line"
                 :data [(map #(inc (.getMonth (:month %))) @crime-reports-by-month)
                        [(map :count @crime-reports-by-month)]]}]}]))

(defn crime-reports-by-hour-template []
  (r/with-let [crime-reports-by-hour (subscribe [:admin.crime-reports/by-hour])]
    [card
     {:title "Crime Reports"
      :subtitle "by Hour"
      :content [chart
                {:display-name "chart-reports-by-hour"
                 :chart-type "bar"
                 :data [(map :hour @crime-reports-by-hour)
                        [(map :count @crime-reports-by-hour)]]}]}]))

; -----------------------------------------------------------------------
; Content
; -----------------------------------------------------------------------

(defn content []
  (let [from "01/01/2017"
        to "31/12/2017"]
    (dispatch [:api/get-crime-reports-by-crime-type from to])
    (dispatch [:api/get-crime-reports-by-month from to])
    (dispatch [:api/get-crime-reports-by-hour from to]))
  (fn []
    [:div.content
     [:div.container-fluid
      [:div.row
       [:div.col-md-6 [crime-reports-by-crime-type-template]]
       [:div.col-md-6 [crime-reports-by-month-template]]]
      [:div.row
       [:div.col-md-12
        [crime-reports-by-hour-template]]]]]))

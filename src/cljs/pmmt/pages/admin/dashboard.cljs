(ns pmmt.pages.admin.dashboard
  (:require
   [reagent.core :as r]
   [re-frame.core :refer [subscribe dispatch]]
   [pmmt.components.common :as c :refer [card chart]]))

; -----------------------------------------------------------------------
; Charts
; -----------------------------------------------------------------------

(defn crime-reports-by-crime-type-template []
  (r/with-let [crime-reports-by-crime-type (subscribe [:admin.crime-reports/by-crime-type])]
    (when @crime-reports-by-crime-type
      [card
       {:title "Crime Reports"
        :subtitle "by Crime Type"
        :content
        [chart
         {:id "chart-reports-by-crime-type"
          :type :horizontal-bar
          :labels (map :crime-type @crime-reports-by-crime-type)
          :datasets (map :count @crime-reports-by-crime-type)}]}])))

(defn crime-reports-by-month-template []
  (r/with-let [crime-reports-by-month (subscribe [:admin.crime-reports/by-month])]
    (when @crime-reports-by-month
      [card
       {:title "Crime Reports"
        :subtitle "by Month"
        :content [chart
                  {:id "chart-reports-by-month"
                   :type :bar
                   :labels (map #(inc (.getMonth (:month %))) @crime-reports-by-month)
                   :datasets (map :count @crime-reports-by-month)}]}])))
                   ; :type :line
                   ; :labels (map #(inc (.getMonth (:month %))) @crime-reports-by-month)
                   ; :datasets [{:label "Reports by month"
                   ;             :data (map :count @crime-reports-by-month)}]}]}])))

(defn crime-reports-by-hour-template []
  (r/with-let [crime-reports-by-hour (subscribe [:admin.crime-reports/by-hour])]
    (when @crime-reports-by-hour
      [card
       {:title "Crime Reports"
        :subtitle "by Hour"
        :content [chart
                  {:id "chart-reports-by-hour"
                   :type :line
                   :labels (map :hour @crime-reports-by-hour)
                   :datasets [{:label "Reports by hour"
                               :data (map :count @crime-reports-by-hour)}]}]}])))


; -----------------------------------------------------------------------
; Content
; -----------------------------------------------------------------------

(defn content []
  (let [from "02/01/2017"
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

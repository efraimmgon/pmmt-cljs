(ns pmmt.handlers.report
  (:require-macros
   [pmmt.macros :refer [log]])
  (:require
   [clojure.string :refer [capitalize]]
   [ajax.core :as ajax]
   [reagent.core :as r :refer [atom]]
   [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-sub subscribe]]
   [pmmt.validation :as v]
   [pmmt.utils :as utils]))

; ------------------------------------------------------------------------
; Subscriptions
; ------------------------------------------------------------------------

(reg-sub
 :report/params
 (fn [db _]
   (get-in db [:report :params])))

; Statistics -------------------------------------------------------------

(reg-sub
 :report/statistics
 (fn [db _]
   (get-in db [:report :statistics])))

(reg-sub
 :report.single/statistics
 (fn [query-v _]
   (subscribe [:report/statistics]))
 (fn [statistics]
   (-> statistics :ranges first :crime-reports)))

(reg-sub
 :report.single/by-crime-type
 (fn [query-v _]
   (subscribe [:report.single/statistics]))
 (fn [statistics]
   (map (juxt :type :count) (:by-crime-type statistics))))

(reg-sub
 :report.single/by-neighborhood
 (fn [query-v _]
   (subscribe [:report.single/statistics]))
 (fn [statistics]
   (map (juxt :neighborhood :count) (:by-neighborhood statistics))))

(reg-sub
 :report.single/by-weekday
 (fn [query-v _]
   (subscribe [:report.single/statistics]))
 (fn [statistics]
   (map (juxt (comp utils/psql-weekday->readable :weekday)
              :count)
        (:by-weekday statistics))))

(reg-sub
 :report.single/by-route
 (fn [query-v _]
   (subscribe [:report.single/statistics]))
 (fn [statistics]
   (map (juxt :route :count) (:by-route statistics))))

(reg-sub
 :report.single/by-period
 (fn [query-v _]
   (subscribe [:report.single/statistics]))
 (fn [statistics]
   (map (juxt (comp utils/period->readable :period)
              :count)
        (:by-period statistics))))

(reg-sub
 :report.single/by-hour
 (fn [query-v _]
   (subscribe [:report.single/statistics]))
 (fn [statistics]
   (map (juxt :hour :count)
        (:by-hour statistics))))

(reg-sub
 :report.single/by-date
 (fn [query-v _]
   (subscribe [:report.single/statistics]))
 (fn [statistics]
   (map (juxt (comp utils/date->readable :created-at) :count)
        (:by-date statistics))))


; Charts ---------------------------------------------------------------

(reg-sub
 :plot-data
 (fn [db _]
   (get-in db [:report :plots])))

(reg-sub
 :optionals-result
 (fn [db _]
   (get-in db [:report :optionals])))

; ------------------------------------------------------------------------
; Handlers
; ------------------------------------------------------------------------

(reg-event-fx
 :process-report-data
 (fn [{:keys [db]} [_ result]]
   {:dispatch [:remove-modal]
    :db (assoc-in db [:report :statistics] result)}))

(defn hm-date->str [m]
  (str (:day m) "/" (:month m) "/" (:year m)))

(reg-event-db
 :query-report
 (fn [db [_ params errors]]
   (ajax/GET "/api/crime-reports/statistics"
             {:handler #(dispatch [:process-report-data %])
              :error-handler #(log %)
              :params @params})
   (assoc-in db [:report :params] @params)))

(defn plotly-did-update-pie [comp]
  (let [;; get new data
        [_ id values] (r/argv comp)
        container (.getElementById js/document (str "id_" (:name values) "_graph"))
        data [{:type "pie"
               :labels (:labels values)
               :values (:vals values)}]]
    ;; clear previous plot from the div
    (.purge js/Plotly container)
    (.plot js/Plotly container (clj->js data))))

(reg-event-fx
 :update-pie-plot
 (fn [{:keys [db]} [_ comp]]
   (plotly-did-update-pie comp)
   {:db db}))

(defn plotly-did-update-bar [comp]
  (let [;; get new data
        [_ id values] (r/argv comp)
        container (.getElementById js/document (str "id_" id "_graph"))
        data (for [row values]
               {:type "bar"
                :x (:x row)
                :y (:y row)
                :name (:name row)})
        layout {:xaxis {:title (capitalize id)}
                :yaxis {:title "Registros"}}]
    ;; clear previous plot from the div
    (.purge js/Plotly container)
    (.newPlot js/Plotly container (clj->js data) (clj->js layout))))

(reg-event-fx
 :update-bar-plot
 (fn [{:keys [db]} [_ comp]]
   (plotly-did-update-bar comp)
   {:db db}))

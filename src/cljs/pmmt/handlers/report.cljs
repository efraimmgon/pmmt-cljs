(ns pmmt.handlers.report
  (:require-macros
   [pmmt.macros :refer [log]])
  (:require
   [clojure.string :as string :refer [capitalize]]
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

; Single Statistics ------------------------------------------------------

(reg-sub
 :report.single/statistics
 (fn [query-v _]
   (subscribe [:report/statistics]))
 (fn [statistics]
   (-> statistics :ranges first :crime-reports)))

(reg-sub
 :report.single/by
 (fn [query-v _]
   (subscribe [:report.single/statistics]))
 (fn [statistics [_ field]]
   (map (juxt field :count) ((keyword (str "by-" field)) statistics))))

(reg-sub
 :report.single/by-weekday
 (fn [query-v _]
   (subscribe [:report.single/statistics]))
 (fn [statistics]
   (map (juxt (comp utils/psql-weekday->readable :weekday)
              :count)
        (:by-weekday statistics))))

(reg-sub
 :report.single/by-period
 (fn [query-v _]
   (subscribe [:report.single/statistics]))
 (fn [statistics]
   (map (juxt (comp utils/period->readable :period)
              :count)
        (:by-period statistics))))

(reg-sub
 :report.single/by-date
 (fn [query-v _]
   (subscribe [:report.single/statistics]))
 (fn [statistics]
   (map (juxt (comp utils/date->readable :created-at) :count)
        (:by-date statistics))))

; Composite Statistics -------------------------------------------------

(reg-sub
 :report.compare/count
 (fn [query-v _]
   (subscribe [:report/statistics]))
 (fn [statistics]
   (vector
     ((juxt :old :new :increase)
      (get-in statistics [:compare :count])))))

(reg-sub
 :report.compare/by
 (fn [query-v _]
   (subscribe [:report/statistics]))
 (fn [statistics [_ field]]
   (map (juxt field :old :new :increase)

        (get-in statistics [:compare (keyword (str "by-" (name field)))]))))

(reg-sub
 :report.composite/by
 (fn [query-v _]
   (subscribe [:report/statistics]))
 (fn [statistics [_ field]]
   (->> (:ranges statistics)
        (map #(get-in % [:crime-reports (keyword (str "by-" (name field)))]))
        (map #(map (juxt field :count) %)))))

(reg-sub
 :report.compare/by-weekday
 (fn [query-v _]
   (subscribe [:report/statistics]))
 (fn [statistics]
   (map (juxt (comp utils/psql-weekday->readable :weekday)
              :old :new :increase)
        (get-in statistics [:compare :by-weekday]))))

(reg-sub
 :report.compare/by-period
 (fn [query-v _]
   (subscribe [:report/statistics]))
 (fn [statistics]
   (map (juxt (comp utils/period->readable :period)
              :old :new :increase)
        (get-in statistics [:compare :by-period]))))

(reg-sub
 :report.composite/by-date
 (fn [query-v _]
   (subscribe [:report/statistics]))
 (fn [statistics]
   (->> (:ranges statistics)
        (map #(get-in % [:crime-reports :by-date]))
        (map #(map (juxt (comp utils/date->readable :created-at)
                         :count) %)))))

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

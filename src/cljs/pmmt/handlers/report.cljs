(ns pmmt.handlers.report
  (:require-macros
   [pmmt.macros :refer [log]])
  (:require
   [clojure.string :refer [capitalize]]
   [ajax.core :as ajax]
   [reagent.core :as r :refer [atom]]
   [re-frame.core :refer [reg-event-db reg-event-fx reg-sub]]
   [pmmt.validation :as v]))

; ------------------------------------------------------------------------
; Subscriptions
; ------------------------------------------------------------------------

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
    :db (assoc db :report result)}))

(reg-event-fx
 :query-report
 (fn [{:keys [db]} [_ fields errors]]
   (if-let [err (v/validate-report-args @fields)]
     {:reset [errors err]
      :db db}
     {:http-xhrio {:method :get
                   :params @fields
                   :uri "analise-criminal/relatorio/dados"
                   :on-success [:process-report-data]
                   :response-format (ajax/json-response-format {:keywords? true})}
      :db (assoc db :report-params @fields)})))

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

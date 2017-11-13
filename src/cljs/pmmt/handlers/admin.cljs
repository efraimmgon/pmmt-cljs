(ns pmmt.handlers.admin
  (:require-macros
   [pmmt.macros :refer [log]])
  (:require
   [ajax.core :as ajax]
   [clojure.string :as string]
   [reagent.core :as r]
   [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-sub]]
   [pmmt.subs :refer [query]]))

; -------------------------------------------------------------------------
; Subscriptions
; -------------------------------------------------------------------------

(reg-sub
 :admin/active-panel
 (fn [db _]
   (get-in db [:admin :active-panel])))

(reg-sub
 :admin/active-panel-title
 (fn [db _]
   (get-in db [:admin :active-panel-title])))

(reg-sub
 :admin/active-sidebar
 (fn [db _]
   (get-in db [:admin :active-sidebar])))

(reg-sub
 :admin/table-rows
 (fn [db [_ table]]
   (get-in db [:admin table :rows])))

(reg-sub
 :admin/users
 (fn [db _]
   (get-in db [:users])))

(reg-sub
 :admin.database/setup-ready?
 (fn [db _]
   (get-in db [:admin :database :setup-ready?])))

(reg-sub
 :admin.database/active-panel
 (fn [db _]
   (get-in db [:admin :database :active-panel])))

;; returns tables that will visible at the admin interface
(reg-sub
 :admin.database/tables
 (fn [db _]
   (get-in db [:admin :database :tables])))

(reg-sub
 :admin.users/setup-ready?
 (fn [db _]
   (get-in db [:admin :users :setup-ready?])))

; Crime reports -----------------------------------------------------------

(reg-sub
 :admin.crime-reports/by-crime-type
 (fn [db _]
   (get-in db [:admin :crime-reports :by-crime-type])))

(reg-sub
 :admin.crime-reports/by-month
 (fn [db _]
   (get-in db [:admin :crime-reports :by-month])))

(reg-sub
 :admin.crime-reports/by-period
 (fn [db _]
   (get-in db [:admin :crime-reports :by-period])))

(reg-sub
 :admin.crime-reports/by-hour
 (fn [db _]
   (get-in db [:admin :crime-reports :by-hour])))

; -------------------------------------------------------------------------
; Handlers
; -------------------------------------------------------------------------

; Helpers -----------------------------------------------------------------

(reg-event-db
 :admin/set-crime-reports-by-crime-type
 (fn [db [_ rows]]
   (assoc-in db [:admin :crime-reports :by-crime-type] rows)))

(reg-event-db
 :admin/set-crime-reports-by-month
 (fn [db [_ rows]]
   (assoc-in db [:admin :crime-reports :by-month] rows)))

(reg-event-db
 :admin/set-crime-reports-by-period
 (fn [db [_ rows]]
   (assoc-in db [:admin :crime-reports :by-period] rows)))

(reg-event-db
 :admin/set-crime-reports-by-hour
 (fn [db [_ rows]]
   (assoc-in db [:admin :crime-reports :by-hour] rows)))

; DB Query ----------------------------------------------------------------

(reg-event-db
 :api/get-crime-reports-by-crime-type
 (fn [db [_ from to]]
   (ajax/GET "/api/crime-reports/by-crime-type"
             {:handler #(dispatch [:admin/set-crime-reports-by-crime-type %])
              :params {:from from, :to to}
              :error-handler #(log %)})
   db))

(reg-event-db
 :api/get-crime-reports-by-month
 (fn [db [_ from to]]
   (ajax/GET "/api/crime-reports/by-month"
             {:handler #(dispatch [:admin/set-crime-reports-by-month %])
              :params {:from from :to to}
              :error-handler #(log %)})
   db))

(reg-event-db
 :api/get-crime-reports-by-period
 (fn [db [_ from to]]
   (ajax/GET "/api/crime-reports/by-period"
             {:handler #(dispatch [:admin/set-crime-reports-by-period %])
              :params {:from from :to to}
              :error-handler #(log %)})
   db))

(reg-event-db
 :api/get-crime-reports-by-hour
 (fn [db [_ from to]]
   (ajax/GET "/api/crime-reports/by-hour"
             {:handler #(dispatch [:admin/set-crime-reports-by-hour %])
              :params {:from from :to to}
              :error-handler #(log %)})
   db))

;; get db rows from `table`
(reg-event-db
 :admin/fetch-table-rows
 (fn [db [_ table]]
   (ajax/GET (str "/db/" table)
             {:handler #(dispatch [:admin/set-table-data table %])
              :error-handler #(log %)})
   db))

(reg-event-db
 :admin/search-table
 (fn [db [_ table fields errors]]
   ;; NOTE: uri is in format "/:table/:field/:id"
   (ajax/GET (str "/db/" table "/" (string/lower-case (:field @fields)) "/" (:value @fields))
             {:handler #(do (dispatch [:admin/set-table-data table %])
                            (dispatch [:remove-modal]))
              :error-handler #(reset! errors %)})
   db))

(reg-event-db
 :admin/set-active-panel
 (fn [db [_ title panel-id]]
   (-> db
      (assoc-in [:admin :active-panel-title] title)
      (assoc-in [:admin :active-panel] panel-id))))

(reg-event-db
 :admin/set-active-sidebar
 (fn [db [_ title sidebar-id]]
   (-> db
       (assoc-in [:admin :sidebar-title] title)
       (assoc-in [:admin :active-sidebar] sidebar-id))))

(reg-event-db
 :admin/set-table-data
 (fn [db [_ table rows]]
   (if-not (empty? rows)
     (assoc-in db [:admin table :rows]
               (vec (partition-all 15 rows)))
     (do (js/alert "Nada encontrado")
         db))))

(reg-event-db
 :admin.database/setup-ready
 (fn [db _]
   (assoc-in db [:admin :database :setup-ready?] true)))

(reg-event-db
 :admin.database/set-active-panel
 (fn [db [_ panel-id]]
   (assoc-in db [:admin :database :active-panel] panel-id)))

(reg-event-db
 :get-users
 (fn [db _]
   (ajax/GET "/db/users"
             {:handler #(dispatch [:set-users %])
              :error-handler #(log %)})
   db))

(reg-event-db
 :set-users
 (fn [db [_ users]]
   (assoc db :users users)))


; -------------------------------------------------------------------------
; Charts
; -------------------------------------------------------------------------

(def colors
  ["#001f3f" ; navy
   "#0074D9" ; blue
   "#7FDBFF" ; aqua
   "#39CCCC" ; teal
   "#3D9970" ; olive
   "#2ECC40" ; green
   "#01FF70" ; lime
   "#FFDC00" ; yellow
   "#FF851B" ; orange
   "#FF4136" ; red
   "#85144b" ; maroon
   "#F012BE" ; fuchsia
   "#B10DC9" ; purple
   "#111111" ; black
   "#AAAAAA" ; gray
   "#DDDDDD"]) ;silver

(defn pick-background-colors [n]
  (if (= 1 n)
    (first colors)
    (take n colors)))

(defmulti plot-chart :type)

; dataset is a vector of ints
(defmethod plot-chart :pie
  [{:keys [id labels datasets]}]
  (let [ctx (.getContext (.getElementById js/document id) "2d")
        opts {:type :pie
              :data {:labels labels
                     :datasets [{:data datasets
                                 :backgroundColor (take (count labels) colors)}]}}]
    (js/Chart. ctx (clj->js opts))))

; dataset is a vector maps with the keys: `data` and `label`
(defmethod plot-chart :line
  [{:keys [id labels datasets]}]
  (let [ctx (.getContext (.getElementById js/document id) "2d")
        opts {:type :line
              :data {:labels labels
                     :datasets (map-indexed
                                (fn [i row]
                                  {:data (:data row)
                                   :label (:label row)
                                   :borderColor (colors i)
                                   :fill false})
                                datasets)}}]
    (js/Chart. ctx (clj->js opts))))

(defmethod plot-chart :bar
  [{:keys [id labels datasets]}]
  (let [ctx (.getContext (.getElementById js/document id) "2d")
        opts {:type :bar
              :data {:labels labels
                     :datasets [{:data datasets
                                 :backgroundColor (take (count labels) colors)}]}
              :options {:legend {:display false}}}]
    (js/Chart. ctx (clj->js opts))))

(defmethod plot-chart :horizontal-bar
  [{:keys [id labels datasets]}]
  (let [ctx (.getContext (.getElementById js/document id) "2d")
        opts {:type :horizontalBar
              :data {:labels labels
                     :datasets [{:data datasets
                                 :backgroundColor (take (count labels) colors)}]}
              :options {:legend {:display false}}}]
    (js/Chart. ctx (clj->js opts))))

(defmethod plot-chart :radar
  [{:keys [id labels datasets]}]
  (let [ctx (.getContext (.getElementById js/document id) "2d")
        opts {:type :radar
              :data {:labels labels
                     :datasets (map-indexed
                                (fn [i row]
                                  {:data (:data row)
                                   :label (:label row)
                                   :backgroundColor (colors i)
                                   :borderColor (colors i)
                                   :pointBorderColor "#fff"
                                   :pointBackgroundColor (colors i)
                                   :fill true})
                                datasets)}}]
    (js/Chart. ctx (clj->js opts))))

(reg-event-fx
 :charts/plot-chart
 (fn [db [_ opts]]
   (plot-chart opts)
   nil))

(ns pmmt.handlers
  (:require [re-frame.core :refer
             [reg-event-db reg-event-fx reg-fx]]
            [ajax.core :as ajax]
            [jayq.core :as jq]
            day8.re-frame.http-fx
            pmmt.handlers.admin
            pmmt.handlers.geoprocessing
            pmmt.handlers.report
            pmmt.handlers.utils))


(defn deep-merge-with [f & maps]
  (apply
    (fn m [& maps]
      (if (every? map? maps)
        (apply merge-with m maps)
        (apply f maps)))
    maps))

; Custom Coeffects ----------------------------------------------------

(reg-fx
 :reset
 (fn [[ratom val]]
   (reset! ratom val)))

; Events ---------------------------------------------------------------

(defn create [db [event value]]
  (assoc db event value))

(reg-event-db
 :set-initial-state
 (fn [db _]
   (deep-merge-with merge
    {
     :admin {:active-page "Painel"
             :active-panel :dashboard
             :database {:setup-ready? false
                        :active-panel :database
                        ;; TODO: move to config file
                        :tables ["cities", "crimes", "crime_reports", "modes_desc"]}}}
    db)))

(reg-event-fx
 :query-crimes
 (fn [{:keys [db]} _]
   {:http-xhrio {:method :get
                 :uri "/db/crimes"
                 :on-success [:assoc-db :crimes]
                 :response-format (ajax/json-response-format {:keywords? true})}
    :db db}))

(reg-event-db
 :modal
 (fn [db [_ modal]]
   (assoc db :modal modal)))

(reg-event-db
 :remove-modal
 (fn [db _]
   (assoc db :modal nil)))

; if (= page :admin) then hide #nav
; else if sb-admin style on head, remove it

(defn find-link [href]
  (jq/$ (str "link[rel=stylesheet][href~='" href "']")))

(defn remove-style! [page]
  (when-not (= page :admin)
    (if-let [$elt (seq (js->clj (find-link "/css/sb-admin.css")))]
      (print "Found $elt! ->" $elt))))
      ;(jq/remove (clj->js $elt)))))

; we need those hacks because the css from the admin breaks the other pages
; and the navbar from the other pages break admin.
(reg-event-fx
 :page
 (fn [{:keys [db]} [_ page]]
   (when (= page :admin)
     (jq/hide (jq/$ :#navbar)))
   {:db (assoc db :page page)}))

(reg-event-db
 :assoc-db
 (fn [db [_ key val]]
   (assoc db key val)))

(reg-event-db
 :update-db
 (fn [db [_ key f]]
   (update db key f)))

; Auth ------------------------------------------------------------------

(reg-event-db
 :remove-identity
 (fn [db _]
   (dissoc db :identity)))

(reg-event-db
 :set-identity
 (fn [db [_ id]]
   (assoc db :identity id)))

(reg-event-fx
 :logout
 (fn [{:keys [db]} _]
   {:http-xhrio {:method :post
                 :uri "/logout"
                 :format (ajax/json-request-format)
                 :on-success [:remove-identity]
                 :response-format (ajax/json-response-format {:keywords? true})}
    :db db}))

(ns pmmt.handlers
  (:require [re-frame.core :refer
             [reg-event-db reg-event-fx reg-fx]]
            [ajax.core :as ajax]
            day8.re-frame.http-fx
            [pmmt.db :as db]
            pmmt.handlers.admin
            pmmt.handlers.geocode
            pmmt.handlers.geoprocessing
            pmmt.handlers.login
            pmmt.handlers.report
            pmmt.handlers.utils
            [pmmt.utils :refer [deep-merge-with]]))

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
 (fn [_ _]
   db/default-db))

(reg-event-fx
 :query-crimes
 (fn [{:keys [db]} _]
   {:http-xhrio {:method :get
                 :uri "/db/crimes"
                 :on-success [:assoc-db :crimes]
                 :response-format (ajax/json-response-format {:keywords? true})}}))

(reg-event-db
 :add-login-event
 (fn [db [_ event]]
   (js/console.log (str "Not logged in: " event))
   db))

(reg-event-db
 :modal
 (fn [db [_ modal]]
   (assoc db :modal modal)))

(reg-event-db
 :remove-modal
 (fn [db _]
   (assoc db :modal nil)))

; we need those hacks because the css from the admin breaks the other pages
; and the navbar from the other pages break admin.
(reg-event-fx
 :page
 (fn [{:keys [db]} [_ page]]
   (when (= page :admin)
     (-> (js/document.getElementById "navbar") .-style .-display (set! "none")))
   {:db (assoc db :page page)}))

(reg-event-db
 :assoc-db
 (fn [db [_ key val]]
   (assoc db key val)))

(reg-event-db
 :update-db
 (fn [db [_ key f]]
   (update db key f)))

(reg-event-db
 :set-state
 (fn [db [_ ks val]]
   (assoc-in db ks val)))

(reg-event-db
 :update-state
 (fn [db [_ ks f]]
   (update-in db ks f)))

; Auth ------------------------------------------------------------------

(reg-event-db
 :remove-identity
 (fn [db _]
   (dissoc db :identity)))

(reg-event-fx
 :logout
 (fn [{:keys [db]} _]
   {:http-xhrio {:method :post
                 :uri "/logout"
                 :format (ajax/json-request-format)
                 :on-success [:remove-identity]
                 :response-format (ajax/json-response-format {:keywords? true})}}))

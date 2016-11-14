(ns pmmt.handlers
  (:require [re-frame.core :refer
             [reg-event-db reg-event-fx reg-sub reg-fx]]
            [ajax.core :as ajax]
            [day8.re-frame.http-fx]))

;;;; Global Events and Subscriptions

; Custom Coeffects ----------------------------------------------------

(reg-fx
 :reset
 (fn [[ratom val]]
   (reset! ratom val)))

; Events ---------------------------------------------------------------

(defn create [db [event value]]
  (assoc db event value))

; Auth

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

; Server DB access

(reg-event-fx
 :query-cities
 (fn [{:keys [db]} _]
   {:http-xhrio {:method :get
                 :uri "/db/cidade"
                 :on-success [:assoc-db :cities]
                 :response-format (ajax/json-response-format {:keywords? true})}
    :db db}))

(reg-event-fx
 :query-naturezas
 (fn [{:keys [db]} _]
   {:http-xhrio {:method :get
                 :uri "/db/natureza"
                 :on-success [:assoc-db :naturezas]
                 :response-format (ajax/json-response-format {:keywords? true})}
    :db db}))

; General

(reg-event-db
 :modal
 (fn [db [_ modal]]
   (assoc db :modal modal)))

(reg-event-db
 :remove-modal
 (fn [db _]
   (assoc db :modal nil)))

(reg-event-db
 :page
 (fn [db [_ page]]
   (assoc db :page page)))

(reg-event-db
 :assoc-db
 (fn [db [_ key val]]
   (assoc db key val)))

(reg-event-db
 :update-db
 (fn [db [_ key f]]
   (update db key f)))

; Admin

(reg-event-db
 :admin/create-rows
 (fn [db [_ table rows]]
   (assoc-in db [table :rows]
             (vec (partition-all 15 rows)))))

(reg-event-fx
 :admin/fetch-table-rows
 (fn [{:keys [db]} [_ table]]
   {:http-xhrio {:method :get
                 :uri (str "/db/" table)
                 :on-success [:admin/create-rows table]
                 :response-format (ajax/json-response-format {:keywords? true})}
    :db db}))

(reg-event-fx
 :admin/search-fail
 (fn [{:keys [db]} [_ errors response]]
   {:reset [errors response]
    :db db}))

(reg-event-fx
 :admin/search-success
 (fn [{:keys [db]} [_ table response]]
   {:dispatch-n (list [:admin/create-rows table response] [:remove-modal])
    :db db}))

(reg-event-fx
 :admin/search-table
 (fn [{:keys [db]} [_ table fields errors]]
   {:http-xhrio {:method :get
                 ;; uri is in format "/:table/:field/:id"
                 :uri (str "/db/" table "/" (:field @fields) "/" (:value @fields))
                 :on-success [:admin/search-success table]
                 :on-fail [:admin/search-fail errors]
                 :response-format (ajax/json-response-format {:keywords? true})}
    :db db}))

(reg-event-db
 :admin/set-active-panel
 (fn [db [_ value]]
   (assoc db :admin/active-panel value)))

; Subscriptions ---------------------------------------------------------

(defn query [db [event]]
  (get db event))

(defn format-opts [options value-key display-key]
  (into []
    (map (fn [m]
           {:value (get m value-key)
            :display (get m display-key)})
         options)))

; General

(reg-sub
 :get-db
 (fn [db [_ key]]
   (get db key)))

(reg-sub :modal query)

(reg-sub :page query)

(reg-sub :cities query)

(reg-sub :naturezas query)

(reg-sub
 :city-form-opts
 (fn [db _]
   (format-opts (:cities db) :id :nome)))

; Admin

(reg-sub
 :admin/read-rows
 (fn [db [_ table]]
   (get-in db [table :rows])))

(reg-sub :admin/active-panel query)

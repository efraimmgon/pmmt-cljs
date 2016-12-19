(ns pmmt.components.admin.handlers
  (:require [ajax.core :as ajax]
            [re-frame.core :refer
             [reg-event-db reg-event-fx reg-sub]]
            [day8.re-frame.http-fx]
            [pmmt.subs :refer [query]]))

; Subscriptions -----------------------------------------------------------

(reg-sub :admin/active-panel query)

(reg-sub :admin/active-page query)

(reg-sub
 :admin/read-rows
 (fn [db [_ table]]
   (get-in db [table :rows])))

(reg-sub :users query)

; Events ------------------------------------------------------------------

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
 :get-users
 (fn [cofx _]
   {:http-xhrio {:method :get
                 :uri "/db/users"
                 :on-success [:get-users-success]
                 :response-format (ajax/json-response-format {:keywords? true})}}))

(reg-event-db
 :get-users-success
 (fn [db [_ response]]
   (assoc db :users response)))

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
 (fn [db [_ title panel]]
   (assoc db
          :admin/active-page title
          :admin/active-panel panel)))

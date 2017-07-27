(ns pmmt.handlers.admin
  (:require-macros
   [pmmt.macros :refer [log]])
  (:require
   [ajax.core :as ajax]
   [re-frame.core :refer [reg-event-db reg-sub]]
   [pmmt.subs :refer [query]]))


; -------------------------------------------------------------------------
; Subscriptions
; -------------------------------------------------------------------------

(reg-sub :admin/active-panel query)

(reg-sub :admin/active-page query)

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
 :admin/table-rows
 (fn [db [_ table]]
   (get-in db [:admin table :rows])))

(reg-sub :users query)

; -------------------------------------------------------------------------
; Handlers
; -------------------------------------------------------------------------

(reg-event-db
 :admin.database/setup-ready
 (fn [db _]
   (assoc-in db [:admin :database :setup-ready?] true)))

(reg-event-db
 :admin/set-table-data
 (fn [db [_ table rows]]
   (assoc-in db [:admin table :rows]
             (vec (partition-all 15 rows)))))

;; get db rows from `table`
(reg-event-db
 :admin/fetch-table-rows
 (fn [db [_ table]]
   (ajax/GET (str "/db/" table)
             {:handler #(dispatch [:admin/set-table-data table %])
              :error-handler #(log %)})
   db))

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

(reg-event-db
 :admin/search-table
 (fn [db [_ table fields errors]]
   ;; uri is in format "/:table/:field/:id"
   (ajax/GET (str "/db/" table "/" (:field @fields) "/" (:value @fields))
             {:handler #(do (dispatch [:admin/create-rows table %])
                            (dispatch [:admin/remove-modal]))
              :error-handler #(reset! errors %)})
   db))

(reg-event-db
 :admin/set-active-panel
 (fn [db [_ title panel]]
   (assoc db
          :admin/active-page title
          :admin/active-panel panel)))

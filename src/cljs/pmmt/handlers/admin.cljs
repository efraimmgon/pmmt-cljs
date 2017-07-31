(ns pmmt.handlers.admin
  (:require-macros
   [pmmt.macros :refer [log]])
  (:require
   [ajax.core :as ajax]
   [clojure.string :as string]
   [re-frame.core :refer [dispatch reg-event-db reg-sub]]
   [pmmt.subs :refer [query]]))

; -------------------------------------------------------------------------
; Subscriptions
; -------------------------------------------------------------------------

(reg-sub
 :admin/active-panel
 (fn [db _]
   (get-in db [:admin :active-panel])))

(reg-sub
 :admin/active-page
 (fn [db _]
   (get-in db [:admin :active-page])))

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
 :admin.navbar/active-menu
 (fn [db _]
   (get-in db [:admin :navbar :active-menu])))

(reg-sub
 :admin.users/setup-ready?
 (fn [db _]
   (get-in db [:admin :users :setup-ready?])))

; -------------------------------------------------------------------------
; Handlers
; -------------------------------------------------------------------------

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
      (assoc-in [:admin :active-page] title)
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
 :admin.navbar/toggle-active-menu
 (fn [db [_ id]]
   (if (= id (get-in db [:admin :navbar :active-menu]))
     (assoc-in db [:admin :navbar :active-menu] nil)
     (assoc-in db [:admin :navbar :active-menu] id))))

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

(ns pmmt.subs
  (:require
   [pmmt.utils :refer [extract-ns+name]]
   [re-frame.core :refer [reg-sub]]))

; helpers ---------------------------------------------------------

(defn query [db [event]]
  (get db event))

(defn query-sub [db [event ns+name]]
  (get-in db (extract-ns+name ns+name)))

(defn format-opts [options value-key display-key]
  (into []
    (map (fn [m]
           {:value (get m value-key)
            :display (get m display-key)})
         options)))

; General ---------------------------------------------------------

(reg-sub
 :get-db
 (fn [db [_ key]]
   (get db key)))

(reg-sub :modal query)

(reg-sub :page query)

(reg-sub :cities query)

(reg-sub :crimes query)

; Geo --------------------------------------------------------------

(reg-sub :show-table? query)

(reg-sub :sinop query)

; -----------------------------------------------------------------------
; Settings
; -----------------------------------------------------------------------

(reg-sub
 :settings/page-color-palette
 (fn [db _]
   (get-in db [:settings :page-color-palette])))

(reg-sub
 :settings/page-background-image
 (fn [db _]
   (get-in db [:settings :page-background-image])))

(reg-sub
 :settings/sidebar-color-palette
 (fn [db _]
   (get-in db [:settings :sidebar-color-palette])))

(reg-sub
 :settings/sidebar-background-image
 (fn [db _]
   (get-in db [:settings :sidebar-background-image])))

(reg-sub
 :settings/google-api-key
 (fn [db _]
   (get-in db [:settings :google-api-key])))

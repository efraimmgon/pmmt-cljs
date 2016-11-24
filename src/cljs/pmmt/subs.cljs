(ns pmmt.subs
  (:require [re-frame.core :refer [reg-sub]]))

; helpers ---------------------------------------------------------

(defn query [db [event]]
  (get db event))

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

(reg-sub :naturezas query)

(reg-sub
 :city-form-opts
 (fn [db _]
   (format-opts (:cities db) :id :nome)))

; Geo --------------------------------------------------------------

(reg-sub :geo/scripts-loaded? query)

(reg-sub :show-table? query)
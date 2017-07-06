(ns pmmt.handlers.utils
  (:require-macros [pmmt.macros :refer [log]])
  (:require
   [ajax.core :as ajax]
   [re-frame.core :as re-frame  :refer [dispatch reg-event-db]]
   [pmmt.pages.utils :refer [date-delta-result-modal]]
   [pmmt.validation :as v]))

(reg-event-db
 :time-delta-response
 (fn [db [_ fields response]]
   (dispatch [:modal date-delta-result-modal])
   (-> db
       (assoc-in [:time-delta :date] (:date @fields))
       (assoc-in [:time-delta :days] (:days @fields))
       (assoc-in [:time-delta :end] response))))

(defn calculate-delta [db [_ fields errors]]
  (if-let [err (v/validate-util-date-calc @fields)]
    (reset! errors err)
    (ajax/GET "/calculate-delta"
              {:params @fields
               :handler #(dispatch [:time-delta-response fields %])
               :error-handler #(log %)}))
  db)
(reg-event-db
 :calculate-delta
 calculate-delta)

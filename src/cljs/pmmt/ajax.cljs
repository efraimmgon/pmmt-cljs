(ns pmmt.ajax
  (:require [ajax.core :as ajax]
            [re-frame.core :refer [dispatch]]))

(defn local-uri? [{:keys [uri]}]
  (not (re-find #"^\w+?://" uri)))

(defn default-headers [request]
  (-> request
      (-> request
        (update :uri #(str js/context %))
        (update
          :headers
          #(merge
            %
            {"Accept" "application/transit+json"
             "x-csrf-token" js/csrfToken})))
      (update :uri #(str js/context %))))


(defn user-action [request]
  (dispatch [:assoc-db :user-event true])
  request)

(defn load-interceptors! []
  (swap! ajax/default-interceptors
         conj
         (ajax/to-interceptor {:name "default headers"
                               :request default-headers})))

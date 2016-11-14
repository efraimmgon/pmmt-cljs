(ns pmmt.ajax
  (:require [ajax.core :as ajax]
            [re-frame.core :refer [dispatch]]))

(defn local-uri? [{:keys [uri]}]
  (not (re-find #"^\w+?://" uri)))

(defn default-headers [request]
  (if (local-uri? request)
    (-> request
        (update :uri #(str js/context %))
        (update :headers #(merge {"x-csrf-token" js/csrfToken} %)))
    request))

; (defn load-interceptors! []
;   (swap! ajax/default-interceptors
;          conj
;          (ajax/to-interceptor {:name "default headers"
;                                :request default-headers})))

(defn user-action [request]
  (dispatch [:assoc-db :user-event true])
  request)

(defn load-interceptors! []
  (swap! ajax/default-interceptors
         into
         [(ajax/to-interceptor {:name "default headers"
                                :request default-headers})
          (ajax/to-interceptor {:name "user action"
                                :request user-action})]))

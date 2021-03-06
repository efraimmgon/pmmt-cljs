(ns pmmt.handler
  (:require [compojure.core :refer [routes wrap-routes]]
            [pmmt.layout :refer [error-page]]
            [pmmt.routes.home :refer [home-routes]]
            [pmmt.routes.services :refer
             [service-routes restricted-service-routes]]
            [compojure.route :as route]
            [pmmt.env :refer [defaults]]
            [mount.core :as mount]
            [pmmt.middleware :as middleware]))

(mount/defstate init-app
                :start ((or (:init defaults) identity))
                :stop  ((or (:stop defaults) identity)))

(def app-routes
  (routes
    (wrap-routes #'restricted-service-routes middleware/wrap-auth)
    (-> #'home-routes
        (wrap-routes middleware/wrap-csrf)
        (wrap-routes middleware/wrap-formats))
    #'service-routes
    (route/not-found
      (:body
        (error-page {:status 404
                     :title "page not found"})))))


(defn app [] (middleware/wrap-base #'app-routes))

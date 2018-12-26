(ns pmmt.routes.home
  (:require [pmmt.layout :as layout]
            [pmmt.db.core :as db]
            [compojure.core :refer [context defroutes GET POST ANY]]
            [ring.util.http-response :as response]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [clojure.java.io :as io]))


;;; Utilitários

(def date-format (tf/formatter "dd/MM/yyyy"))

(defn find-date [date days]
  (let [date (tf/parse date-format date)]
    (tf/unparse date-format
      (-> (t/plus date
                  (t/days days))
          ;; to return the final day
          (t/minus (t/days 1))))))

(defn time-delta [date days]
  (->> (find-date date days)
       (response/ok)))

;;; Views and routes

(defn home-page []
  (layout/render "home.html"))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/utils" [] (home-page))
  (context
   "/admin" []
   (GET "/" [] (home-page))
   (GET "/dashboard" [] (home-page))
   (GET "/database" [] (home-page))
   (GET "/users" [] (home-page))
   (GET "/geo" [] (home-page))
   (GET "/report" [] (home-page))
   (ANY "/*" [] (home-page))))

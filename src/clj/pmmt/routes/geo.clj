(ns pmmt.routes.geo
  (:require [ring.util.http-response :as response]
            [clj-time.coerce :as tc]
            [pmmt.routes.common :as c]
            [pmmt.db.core :as db :refer [*db*]]))


;;; helpers

(defn geo-db-coercer
  [params]
  (let [like-fn (fn [s] (and s (str "%" (c/NFKD s) "%")))
        str-to-date (fn [s] (and s (c/str->java-date s)))
        str-to-time (fn [s] (and s (c/str->java-time s)))
        db-params (-> params
                      (update :crime_id clojure.edn/read-string)
                      (update :data_inicial str-to-date)
                      (update :data_final str-to-date)
                      (update :neighborhood like-fn)
                      (update :route like-fn)
                      (update :hora_inicial str-to-time)
                      (update :hora_final str-to-time))]
    db-params))

;;; core

(defn get-crimes []
  (response/ok
   (db/get-crimes)))

(defn adjust-to-geo [row]
  (-> row
      ;; TODO: join the nat name in the db
      (assoc :crime (get (c/CRIMES-MAP-ALL) (:crime_id row)))))

(defn get-geo-data [params]
  (let [rows (db/get-crime-reports-with-geo
              (geo-db-coercer params))]
    (map adjust-to-geo rows)))

(defn geo-dados [params]
  (response/ok
   (get-geo-data params)))

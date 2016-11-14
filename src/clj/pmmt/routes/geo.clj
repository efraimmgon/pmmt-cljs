(ns pmmt.routes.geo
  (:require [ring.util.http-response :as response]
            [clj-time.coerce :as tc]
            [pmmt.routes.common :as c]
            [pmmt.db.core :as db :refer [*db*]]))


;;; helpers

(defn geo-db-coercer
  [params]
  (let [like-fn (fn [s] (and s (str "%" (c/NFKD s) "%")))
        str->long-date (fn [s] (and s (c/str->long-date s)))
        str->long-time (fn [s] (and s (c/str->long-time s)))
        db-params (-> params
                      (update :natureza_id clojure.edn/read-string)
                      (update :data_inicial str->long-date)
                      (update :data_final str->long-date)
                      (update :bairro like-fn)
                      (update :via like-fn)
                      (update :hora_inicial str->long-time)
                      (update :hora_final str->long-time))]
    db-params))

;;; core

(defn get-cities []
  (response/ok
   (db/get-cities)))

(defn get-naturezas []
  (response/ok
   (db/get-naturezas)))

(defn adjust-to-geo [row]
  (-> row
      ;; TODO: join the nat name in the db
      (assoc :natureza (get (c/NATUREZAS-ID-ALL) (:natureza_id row)))))

(defn get-geo-data [params]
  (let [rows (db/get-ocorrencias-with-geo
              (geo-db-coercer params))]
    (map adjust-to-geo rows)))

(defn geo-dados [params]
  (response/ok
   (get-geo-data params)))

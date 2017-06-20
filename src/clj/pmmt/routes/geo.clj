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
                      (update :natureza_id clojure.edn/read-string)
                      (update :data_inicial str-to-date)
                      (update :data_final str-to-date)
                      (update :bairro like-fn)
                      (update :via like-fn)
                      (update :hora_inicial str-to-time)
                      (update :hora_final str-to-time))]
    db-params))

;;; core

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

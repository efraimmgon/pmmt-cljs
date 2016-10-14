(ns pmmt.routes.geo
  (:require [ring.util.http-response :as response]
            [clojure.java.jdbc :as sql]
            [pmmt.routes.common :as c]
            [pmmt.db.core :as db :refer [*db*]]))

(defn get-cities []
  (-> (db/get-cities)
      (response/ok)))

(defn get-naturezas []
  (-> (db/get-naturezas)
      (response/ok)))

(def NATUREZAS-ID-ALL (reduce (fn [acc n]
                                (assoc acc (:id n) (:nome n)))
                              ;;test
                              {} (db/get-naturezas)))

(defn adjust-to-geo [row]
  (-> row
      ;; TODO: join the nat name in the db
      ;; TODO: create a timestamp field in the db for the date and time
      (assoc :natureza (get NATUREZAS-ID-ALL (:natureza_id row)))
      (update :hora c/java->str-time)))

(defn get-geo-data [{:keys [cidade natureza data_inicial data_final]}]
  (let [rows (db/get-geo-data
              {:cidade (Integer. cidade)
               :natureza (Integer. natureza)
               :data_inicial (c/str->java-date data_inicial)
               :data_final (c/str->java-date data_final)})]
    (map adjust-to-geo rows)))

(defn geo-dados [params]
  (-> (get-geo-data params)
      (response/ok)))

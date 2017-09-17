(ns pmmt.routes.admin
  (:require [clojure.java.jdbc :as jdbc]
            [ring.util.http-response :as response]
            [pmmt.db.core :as db]
            [pmmt.utils :refer [domap]]
            [pmmt.routes.common :as c]))

(defn str->int [s]
  (try (Integer. s)
    (catch NumberFormatException e
      s)))

(defn fetch-rows [table]
  (response/ok
   (db/select-by-table {:table table})))

(defn fetch-rows-by-value [table field value]
  ; if value is a number we have to coerce it
  (let [query-params {:table table :field field :value (str->int value)}]
    (response/ok
     (db/select-by-field query-params))))

(defn get-ungeocoded-reports []
  (response/ok
   (db/crime-reports-with-null-coordinates)))

(defn get-users []
  (response/ok
   (db/get-users)))

(defn update-crime-reports! [crime-reports]
  (domap db/update-crime-report!
         crime-reports)
  (response/ok
   {:result :ok}))

(defn get-crime-reports-by-crime-type [params]
  (-> params
      (update :from c/str->java-date)
      (update :to c/str->java-date)
      db/get-crime-reports-by-crime-type
      response/ok))

(defn get-crime-reports-by-month [params]
  (-> params
      (update :from c/str->java-date)
      (update :to c/str->java-date)
      db/get-crime-reports-by-month
      response/ok))

(defn get-crime-reports-by-period [params]
  (-> params
      (update :from c/str->java-date)
      (update :to c/str->java-date)
      db/get-crime-reports-by-period
      response/ok))

(defn get-crime-reports-by-hour [params]
  (-> params
      (update :from c/str->java-date)
      (update :to c/str->java-date)
      db/get-crime-reports-by-hour
      response/ok))

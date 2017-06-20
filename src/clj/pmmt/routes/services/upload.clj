(ns pmmt.routes.services.upload
  (:require [pmmt.db.core :as db]
            [ring.util.http-response :refer :all]
            [clojure.tools.logging :as log]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.walk :as walk]
            [clj-time.coerce :as tc]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [pmmt.routes.common :as c])
  (:import [java.io ByteArrayOutputStream FileInputStream]))

; utility -----------------------------------------------------------

(defn read-csv-file [file]
  (-> file
      (io/reader)
      (csv/read-csv)))

(defn csv->map [csv-data]
  (let [[header & rows] csv-data]
    (map (fn [row]
           (-> (zipmap header row)
               (walk/keywordize-keys)))
         rows)))

; coercion functions ------------------------------------------------

(defn str-to-int [s]
  (if-not (clojure.string/blank? s)
     (Integer. s)))

(defn str-to-double [s]
  (if-not (clojure.string/blank? s)
     (Double. s)))

(defn str-to-long [s]
  ; tc/to-long returns 0 if its param is nil
  (if-not (clojure.string/blank? s)
    (tc/to-long s)))

(defn str-to-date [s]
  (if-not (clojure.string/blank? s)
    (let [date-format (tf/formatters :date)]
      (-> (tf/parse date-format s)
          (c/joda->java-date)))))

(defn str-to-time [s]
  (when-not (clojure.string/blank? s)
    (let [time-format (tf/formatters :hour-minute-second)]
      (-> (tf/parse time-format s)
          (c/joda->java-time)))))

(defn reports-coercer [m]
  (-> m
      (update :data str-to-date)
      (update :latitude str-to-double)
      (update :longitude str-to-double)
      (update :natureza_id str-to-int)
      (update :hora str-to-time)))

(defn natureza-coercer [m]
  (-> m
      (update :id str-to-int)))

; Core ------------------------------------------------------------

(defn file-processing [f]
  (-> f
      (io/reader)
      (csv/read-csv)
      (csv->map)))

(defn save-data! [{:keys [tempfile filename content-type] :as file}]
  (try
    ; insert rows to db
    (doseq [row (csv->map (read-csv-file tempfile))]
      (db/create-ocorrencia! (reports-coercer row)))
    (ok {:result :ok})
    (catch Exception e
      (log/error e)
      (internal-server-error "error"))))

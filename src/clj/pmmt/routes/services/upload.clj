(ns pmmt.routes.services.upload
  (:require [pmmt.db.core :as db]
            [ring.util.http-response :refer :all]
            [clojure.tools.logging :as log]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.walk :as walk]
            [clj-time.coerce :as tc])
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
  ; tc/to-long returns 0 if it's param is nil
  (if-not (clojure.string/blank? s)
    (tc/to-long s)))

(defn reports-coercer [m]
  (-> m
      (dissoc :id)
      (update :data str-to-long)
      (update :cidade_id str-to-int)
      (update :latitude str-to-double)
      (update :longitude str-to-double)
      (update :natureza_id #(-> % str-to-int dec))
      (update :hora str-to-long)))

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

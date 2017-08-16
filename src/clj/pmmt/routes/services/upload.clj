(ns pmmt.routes.services.upload
  (:require
   [clj-time.coerce :as time.coerce]
   [clj-time.core :as time]
   [clj-time.format :as time.format]
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [clojure.set :refer [rename-keys]]
   [clojure.string :as string]
   [clojure.tools.logging :as log]
   [clojure.walk :as walk]
   [ring.util.http-response :refer :all]
   [pmmt.db.core :as db]
   [pmmt.routes.common :as c])
  (:import [java.io ByteArrayOutputStream FileInputStream]))

; ----------------------------------------------------------------------
; Utilities
; ----------------------------------------------------------------------

(defn csv->map [csv-data]
  (let [[header & rows] csv-data]
    (map (fn [row]
           (-> (zipmap
                (map (comp string/lower-case #(string/replace % #" " "-")) header)
                row)
               (walk/keywordize-keys)))
         rows)))

; ----------------------------------------------------------------------
; Coercion Helpers
; ----------------------------------------------------------------------

(defn str->int [s]
  (when-not (clojure.string/blank? s)
     (Integer. s)))

(defn str->double [s]
  (when-not (clojure.string/blank? s)
    (Double. s)))

(defn str->long [s]
  ; time.coerce/to-long returns 0 if its param is nil
  (when-not (clojure.string/blank? s)
    (time.coerce/to-long s)))

(defn str->date [s]
  (when-not (clojure.string/blank? s)
    (let [date-format (time.format/formatters :date)]
      (-> (time.format/parse date-format s)
          (c/joda->java-date)))))

(defn str->time [s]
  (when-not (clojure.string/blank? s)
    (let [time-format (time.format/formatters :hour-minute-second)]
      (-> (time.format/parse time-format s)
          (c/joda->java-time)))))

(defn crime->id [s]
  (get (c/CRIMES-REVERSE-MAP) s))

(defn mode-desc->id [s]
  (get (c/MODES-DESC-REVERSE-MAP) s))

(defn city->id [s]
  (get (c/CITIES-REVERSE-MAP) s))

; ----------------------------------------------------------------------
; Core
; ----------------------------------------------------------------------

(defn crime-reports-coercer [m]
  (-> m
      ;; NOTE: Since I'm brazilian the headers of my incoming csv data will be
      ;; in portuguse. I need to map them to the db's field names.
      ;; For now, at least, I'm doing it the easy way:
      (rename-keys {:numero :report_number
                    :narrativa :report
                    :natureza :crime_id
                    :desc-forma :mode_desc_id
                    :municipio :city_id
                    :bairro :neighborhood
                    :tipo-logradouro :route_type
                    :logradouro :route
                    :logr-numero :route_number
                    :logr-complemento :route_complement
                    :data-fato :created_at
                    :hora-minuto-fato :created_on})
      ;;; Coerce foreign keys: map names to ids
      (update :crime_id crime->id)
      (update :mode_desc_id mode-desc->id)
      (update :city_id city->id)
      ;;; Coerce created_at and created_on to date and time, respectively
      (update :created_at str->date)
      (update :created_on str->time)
      ;;; NOTE: latitude and longitude are generated later, based on the
      ;;; address of the crime report. For now it will be null.
      (update :latitude str->double)
      (update :longitude str->double)))

(defn save-data! [{:keys [tempfile filename content-type] :as file}]
  (try
    ;; insert rows into the db
    (doseq [row (csv->map (csv/read-csv (io/reader tempfile)))]
      (db/create-crime-report! (crime-reports-coercer row)))
    (ok {:result :ok})
    (catch Exception e
      (log/error e)
      (internal-server-error "error"))))

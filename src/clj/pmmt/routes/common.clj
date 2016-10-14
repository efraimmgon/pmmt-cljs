(ns pmmt.routes.common
  (:require [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clj-time.format :as tf]
            [clojure.string :refer [lower-case]]))

;;; Date

(def date-format (tf/formatter "dd/MM/yyyy"))

(defn joda->java-date [date]
  (tc/to-date date))

(defn str->joda-date [string]
  (tf/parse date-format string))

(defn str->java-date [string]
  (-> string
      (str->joda-date)
      (joda->java-date)))

(defn java->str-date [date]
  (->> date
       (tc/from-date)
       (tf/unparse date-format)))

(defn format-joda-date [date]
  (tf/unparse date-format date))

(defn find-date [date days]
  (let [date (tf/parse date-format date)]
    (format-joda-date
      (-> (t/plus date
                  (t/days (Integer. days)))
          ;; to return the final day
          (t/minus (t/days 1))))))

(def WEEKDAYS {"Mon" "Segunda"
               "Tue" "Terça"
               "Wed" "Quarta"
               "Thu" "Quinta"
               "Fri" "Sexta"
               "Sat" "Sábado"
               "Sun" "Domingo"})


(defn java->weekday [date]
  (-> (tf/unparse (tf/formatters :rfc822) (tc/from-date date))
      (clojure.string/split #",")
      first
      WEEKDAYS))

;;; Time

(def time-format (tf/formatter "hh:mm"))

(defn joda->java-time [time]
  (tc/to-date time))

(defn str->joda-time [string]
  (tf/parse time-format string))

(defn str->java-time [string]
  (-> string
      (str->joda-time)
      (joda->java-time)))

(defn java->str-time [time]
  (->> time
       (tc/from-date)
       (tf/unparse time-format)))


;;; Utils

(defn NFKD [string]
  (java.text.Normalizer/normalize
   string
   java.text.Normalizer$Form/NFKD))

;;; Global vars

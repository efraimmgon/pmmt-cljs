(ns pmmt.routes.common
  (:require [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clj-time.format :as tf]
            [clojure.string :refer [lower-case includes?]]
            [pmmt.db.core :as db]))

;;; Date

(def date-format (tf/formatter "dd/MM/yyyy"))

;;; Date conversions
; from string
(defn joda->java-date [date]
  (tc/to-date date))

(defn str->joda-date [string]
  (tf/parse date-format string))

(defn str->java-date [string]
  (-> string
      (str->joda-date)
      (joda->java-date)))

(defn str->long-date [s]
  (tc/to-long (str->joda-date s)))

; from long
(defn long->java-date [n]
  (-> (tc/from-long n)
      (joda->java-date)))

; to string
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

(defn long->weekday-str [n]
  (-> (tf/unparse (tf/formatters :rfc822) (tc/from-long n))
      (clojure.string/split #",")
      first
      WEEKDAYS))

;;; Time

(def time-format (tf/formatter "HH:mm"))

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

(defn str->long-time [s]
  (tc/to-long (str->joda-time s)))

;;; Utils

(defn NFKD [string]
  (java.text.Normalizer/normalize
   string
   java.text.Normalizer$Form/NFKD))

(defn fluctuation
  "Calculate variation relative from b to a; if a < b, fluctuation means
  increase, else if a > b, fluctuation means decrease."
  [a b]
  (letfn [(calculate [a b]
                     (if (= b 0)
                       0
                       (-> (- a b) (-) (/ b) (* 100) (double) (Math/round))))]
    (if (< a b)
      (calculate a b)
      (calculate b a))))


;;; Global vars
;;; Defined as functions, since we can't access the db at compile time.

(def NATUREZAS-ALL
  (memoize #(db/get-naturezas)))

(def NATUREZAS-ID-ALL
  (memoize
   #(reduce (fn [acc n]
              (assoc acc (:id n) (:nome n)))
            {} (NATUREZAS-ALL))))

(def CRIMES-AND-IDS
  (memoize
   (fn []
     (into {}
           (map (juxt :nome :id) (db/get-naturezas))))))


(def ROUBO
  (memoize
   (fn []
     (filter #(includes? (:nome %) "Roubo")
              (NATUREZAS-ALL)))))

(def FURTO
  (memoize
   (fn []
     (filter #(includes? (:nome %) "Furto")
              (NATUREZAS-ALL)))))

(def HOMICIDIO
  (memoize
   (fn []
     (filter #(includes? (:nome %) (NFKD "Homicídio"))
              (NATUREZAS-ALL)))))

(def TRAFICO
  (memoize
   (fn []
     (filter #(includes? (:nome %) (NFKD "Tráfico"))
              (NATUREZAS-ALL)))))

(def DROGAS
  (memoize
   (fn []
     (filter #(includes? (:nome %) "Drogas")
              (NATUREZAS-ALL)))))

(defn NATUREZAS-SELECTED []
  (concat (ROUBO) (FURTO) (HOMICIDIO) (TRAFICO)))


(defn NATUREZAS-ID-SELECTED []
  (reduce (fn [acc n]
             (assoc acc (:id n) (:nome n)))
          {} (NATUREZAS-SELECTED)))

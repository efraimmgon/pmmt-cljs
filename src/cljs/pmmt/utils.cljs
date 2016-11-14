(ns pmmt.utils
  (:require [clojure.string :as string]))

(defn long->date-str [n]
  (if n
    (let [tuple (-> (js/Date. n)
                    (.toISOString)
                    (string/split "T")
                    (first)
                    (string/split "-"))]
      (str (tuple 2) "/" (tuple 1) "/" (tuple 0)))
    "null"))

(defn str->date [s]
  (let [tuple (string/split s "/")]
    (js/Date. (tuple 2) (dec (tuple 1)) (dec (tuple 0)))))

(defn long->time-str [n]
  (if n
    (let [tuple (-> (js/Date. n)
                    (.toISOString)
                    (string/split "T")
                    (second)
                    (string/split ":"))]
      (str (tuple 0) ":" (tuple 1)))
    "null"))

(defn long->weekday [d]
  (let [d (js/Date. d)
        WEEKDAY {
                 1 "Segunda"
                 2 "Terça"
                 3 "Quarta"
                 4 "Quinta"
                 5 "Sexta"
                 6 "Sábado"
                 7 "Domingo"}]
    (get WEEKDAY (inc (.getDay d)))))

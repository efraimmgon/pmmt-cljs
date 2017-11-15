(ns pmmt.utils
  (:require [clojure.string :as string]))

; ------------------------------------------------------------------------------
; re-frame helpers
; ------------------------------------------------------------------------------
(defn extract-ns-and-name [k]
  (mapv keyword
       (-> k
           name
           (.split "."))))

(defn query
  "Meant to be used with a rf/reg-sub. Takes a `db` map and an event-id from
   a rf/dispatch and gets the resource based on the namespaced id.
   Ids are namespaced by `.`, eg: `admin.background-image`"
  [db [event-id]]
  (let [event-ks (extract-ns-and-name event-id)]
    (get-in db event-ks)))

(defn <sub [query-v]
  (deref (rf/subscribe query-v)))

; ------------------------------------------------------------------------------
; misc
; ------------------------------------------------------------------------------

(defn domap
  "Implementation of Common Lisp `mapc`. It is like `map` except that the
   results of applying function are not accumulated. The `colls` argument
   is returned."
  [f & colls]
  (reduce (fn [_ args]
            (apply f args))
          nil (apply map list colls))
  colls)

(defn deep-merge-with [f & maps]
  (apply
    (fn m [& maps]
      (if (every? map? maps)
        (apply merge-with m maps)
        (apply f maps)))
    maps))

(defn date->readable [d]
  (if d
    (let [tuple (-> d
                    (.toISOString)
                    (string/split "T")
                    (first)
                    (string/split "-"))]
      (str (tuple 2) "/" (tuple 1) "/" (tuple 0)))
    "null"))

(defn long->date-str [n]
  (if n
    (let [tuple (date->readable (js/Date. n))]
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

(defn psql-weekday->readable [n]
  (get ["Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"]
       n))

(defn period->readable [n]
  (get ["Madrugada", "Matutino", "Vespertino", "Noturno"]
       n))

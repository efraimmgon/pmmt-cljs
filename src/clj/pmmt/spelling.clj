(ns pmmt.spelling
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [pmmt.routes.services.upload :as upload]))

(def project-root (System/getProperty "user.dir"))
(def db-resources-path
  (io/file project-root "resources" "db" "sanitized"))

; ------------------------------------------------------------------------

(defn words [text]
  (re-seq #"[A-ZÇÂÃÁÉÊÍÓÔÕÚ]+" (.toUpperCase text)))

(defn train
  "Count "
  [features]
  (reduce (fn [model f]
            (assoc model f (inc (get model f 0))))
          {} features))

(defn nwords [text]
  (train (words text)))

(defn edits1
  "All edits that are one edit away from `word`."
  [word]
  (let [letters (.toUpperCase "aáâãbcçdeéêfghiíjklmnoóôpqrstuúvwxyz")
        n (count word)]
    (distinct
     (concat
      ;; deletes
      (for [i (range n)]
        (str (subs word 0 i) (subs word (inc i))))
      ;; transposes
      (for [i (range (dec n))]
        (str (subs word 0 i)
             (nth word (inc i))
             (nth word i)
             (subs word (+ 2 i))))
      ;; replaces
      (for [i (range n) c letters]
        (str (subs word 0 i) c (subs word (inc i))))
      ;; inserts
      (for [i (range (inc n)) c letters]
        (str (subs word 0 i) c (subs word i)))))))

(defn known
  "The subset of `words` that appear in the dictionary of `nwords`"
  [words nwords]
  (not-empty (set (for [w words :when (nwords w)]  w))))

(defn known-edits2
  "All known words with edits that are two edits away from `word`."
  [word nwords]
  (-> (for [e1 (edits1 word)
            e2 (edits1 e1)
            :when (nwords e2)]
        e2)
      set
      not-empty))

(defn correct
  "Most probable spelling correction for `word`."
  [word nwords]
        ;; Generate possible spelling corrections for word.
  (let [candidates (or (known [word] nwords)
                       (known (edits1 word) nwords)
                       (known-edits2 word nwords) [word])]
    (apply max-key #(get nwords % 1) candidates)))

; ------------------------------------------------------------------------

(def neighborhoods-file
  (-> project-root
      (io/file  "resources" "db" "sanitized" "bairros-sinop.csv")
      .getPath))

(defn to-text [coll-of-colls]
  (reduce (fn [acc coll]
            (str acc " " (clojure.string/join " " coll)))
          "" coll-of-colls))

(defn load-neighborhoods []
  (with-open [reader (io/reader neighborhoods-file)]
    (-> (csv/read-csv reader)
        to-text
        words
        train)))

(defn load-csv [file]
  (with-open [reader (io/reader file)]
    (doall
     (csv/read-csv reader))))

(defn correct-string
  "Get each word from the string, correct it, and join them again"
  [s nwords]
  (clojure.string/join " "
    (map (fn [word]
           (if (> (count word) 2)
             (correct word nwords)
             word))
         (words s))))

(defn run-correct
  "Apply the respective correcting model to each case"
  [rows nwords]
  (map (fn [[crime neighborhood venue-type venue-name number date hour]]
         [crime,
          (correct-string neighborhood (:neighborhoods nwords))
          venue-type
          venue-name
          number
          date
          hour])

       rows))

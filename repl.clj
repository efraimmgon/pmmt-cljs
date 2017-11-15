


;; rebind conman connection
(in-ns 'pmmt.db.core)
(conman/bind-connection *db* "sql/queries.sql" "sql/analise.sql")

;; commonly used modules
(require '[pmmt.db.core :as db])
(require '[clojure.java.jdbc :as jdbc])

;; common commands
(clojure.tools.namespace.repl/refresh)

; ------------------------------------------------------------------------
; CSV data crunching
; ------------------------------------------------------------------------

(in-ns 'pmmt.spelling)

(defn diff-csv-col
  "diff the edited neighborhoods with the neighborhoods file"
  [s1 s2 out]
  (with-open [writer (io/writer out)]
    (dorun
      (csv/write-csv
       writer
       (map vector
            (sort (clojure.set/difference s1 s2)))))))

(defn remove-diacrits
  "Deaccent cells, except for the crime narrative"
  [in out]
  (with-open [reader (io/reader in)
              writer (io/writer out)]
    (dorun
     (csv/write-csv
      writer
      (->> (csv/read-csv reader)
           (map (fn [row]
                  (map-indexed #(if (= 1 %1)
                                  %2
                                  (deaccent %2))
                                row))))))))

(defn read-csv-file [fin]
  (with-open [reader (io/reader fin)]
    (doall
     (csv/read-csv reader))))


(remove-diacrits
 (io/file "/" "Users" "efraimmgon" "analise-criminal-db" "raw" "jun-jul-ago.csv")
 (io/file "/" "Users" "efraimmgon" "analise-criminal-db" "sanitized" "jun-jul-ago.csv"))


(diff-csv-col
 ;; diffed
 (->> (io/file "/" "Users" "efraimmgon" "analise-criminal-db" "sanitized" "jun-jul-ago.csv")
      (read-csv-file)
      (map #(get % 7)) ; get neighborhood cell
      (drop 1) ; drop header
      set)
 ;; differ
 (->> (io/file "/" "Users" "efraimmgon" "analise-criminal-db" "sanitized" "bairros-sinop-no-accent.csv")
      (read-csv-file)
      ; (drop 1) ; drop header
      (flatten)
      set)
 (io/file "/" "Users" "efraimmgon" "analise-criminal-db" "diff.csv"))

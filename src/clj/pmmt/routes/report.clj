(ns pmmt.routes.report
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [ring.util.http-response :as response]
            [pmmt.routes.common :as c]
            [pmmt.db.core :as db :refer [*db*]]))

; helpers -------------------------------------------------------------

(defn in? [coll obj]
  (some #(= obj %) coll))

(defn labels-and-values [coll]
  (reduce (fn [[labels vals] [k v]]
            [(conj labels k)
             (conj vals v)])
          [[] []] coll))

(defn report-db-coercer
  [{:keys [data-inicial-a data-final-a data-inicial-b data-final-b] :as params}]
  (let [like-fn (fn [s] (and s (str "%" (c/NFKD s) "%")))
        str->coll (fn [s] (and s (clojure.edn/read-string s)))]
    (-> params
        (update :data-inicial-a c/str->java-date)
        (update :data-final-a c/str->java-date)
        (update :data-inicial-b c/str->java-date)
        (update :data-final-b c/str->java-date)
        (update :roubo #(when % (map :id (c/ROUBO))))
        (update :furto #(when % (map :id (c/FURTO))))
        (update :trafico #(when % (map :id (c/TRAFICO))))
        (update :homicidio #(when % (map :id (c/HOMICIDIO))))
        (update :neighborhood like-fn))))

; core ----------------------------------------------------------------

(defn compare-incidents [params-a params-b]
  (let [get-crime-reports-count-by-crime
        (fn [params]
          ; return a coll with the :type and :count
          (db/get-crime-reports-count-by-crime
           ; we care only for a select set of incidents, not all
           (assoc params :crimes-id (c/CRIMES-SELECTED-IDS))))]

    (map (fn [a b]
           {:crime (:crime_type a)
            :a (:count a)
            :b (:count b)
            :fluctuation (c/fluctuation (:count a) (:count b))})
         (get-crime-reports-count-by-crime params-a)
         (get-crime-reports-count-by-crime params-b))))

(defn select-distinct
  "Takes a collection of maps, a function to retrieve a value from the map
  and an optional limit to the result, which defaults to 5.
  Returns => [[key s/Any val s/Any]]"
  ([coll field] (select-distinct coll field 5))
  ([coll field limit]
   (let [result (reduce (fn [acc row]
                           (let [val (field row)]
                             (if (get acc val)
                               (update acc val inc)
                               (assoc acc val 1))))
                        {} coll)]
     (if limit
       (try (subvec (vec (sort-by val > result)) 0 limit)
         ;; if result is less than limit we can't subvec it
         (catch java.lang.IndexOutOfBoundsException e
           (sort-by val > result)))
       (sort-by val > result)))))

(defn select-distinct-weekdays [coll]
  (reduce (fn [acc [long-date count]]
            (let [weekday (c/long->weekday-str long-date)]
              (update acc weekday + count)))
          ; we need to provide an array-map, as we want the weekdays
          ; to stick to the following order
          (array-map "Segunda" 0, "Terça" 0, "Quarta" 0, "Quinta" 0,
                     "Sexta" 0, "Sábado" 0, "Domingo" 0)
          ; there's no limit on the selection, as we want to get all the
          ; distinct dates, which will likely be many
          (select-distinct coll :created_at nil)))

(defn select-distinct-fields
  [coll]
  ;; we need an orderd map, since we'll call `keys` and `vals` later on
  (array-map
    :crimes (select-distinct coll :type)
    :bairros (select-distinct (remove #(empty? (:neighborhood %)) coll) :neighborhood)
    :vias (select-distinct (remove #(empty? (:route %)) coll) :route)
    :locais (select-distinct (remove #(or (empty? (:route %)) (empty? (:neighborhood %))) coll)
                             #(str (:neighborhood %) ", " (:route %)))
    :dias-da-semana  (select-distinct-weekdays coll)))
    ;; TODO: there is no longer a column named `periodo` in `crime-reports`
    ;:periodos (select-distinct (remove #(empty? (:periodo %)) coll) :periodo)))

(defn format-plot-bar-data [name title-x coll]
  (let [[labels vals] (labels-and-values coll)]
    {:name name, :title-x title-x, :x labels, :y vals}))

(defn format-plot-pie-data [id coll]
  (let [[labels vals] (labels-and-values coll)]
    {:name id :labels labels :vals vals}))

(defn process-incidents [queryset-a queryset-b incidents-ids]
  (when (not-empty incidents-ids)
    (for [[incident-key ids] incidents-ids]
      (let [filter-fn (fn [qs] (filter (fn [r] (in? ids (:crime_id r))) qs))
            [qs-a qs-b] (map filter-fn [queryset-a queryset-b])
            [distinct-fields-a distinct-fields-b] (map select-distinct-fields [qs-a qs-b])]
        ; not interested in this value
        {incident-key {:a (dissoc distinct-fields-a :crimes)
                       :b (dissoc distinct-fields-b :crimes)}}))))

(defn process-time-periods [queryset-a queryset-b time-periods?]
  (when time-periods?
    (for [time-period ["06:00 - 11:59", "12:00 - 17:59", "18:00 - 23:59", "00:00 - 05:59"]]
      (let [[distinct-fields-a distinct-fields-b]
            (map select-distinct-fields [queryset-a queryset-b])]
        ; not interested in this value
        {time-period {:a (dissoc distinct-fields-a :periodos)
                      :b (dissoc distinct-fields-b :periodos)}}))))

(defn process-neighborhood [queryset-a queryset-b bairro]
  (when (not-empty bairro)
    ; there's only one `bairro`, but we loop over it anyway, both to
    ; see if we can derive a generic function for all optionals, and because
    ; the return value is concated with the other optionals
    (for [[neighborhood-key like-name] bairro]
      (let [filter-fn (fn [qs] (filter (fn [r] (string/includes? (string/lower-case (:neighborhood r))
                                                                 ; since we're filtering the coll and not doing a new query
                                                                 ; we're not interested in the wildcard chars
                                                                 (string/lower-case (string/replace like-name #"%" ""))))
                                       qs))
            [qs-a qs-b] (map filter-fn [queryset-a queryset-b])
            [distinct-fields-a distinct-fields-b] (map select-distinct-fields [qs-a qs-b])]
        {(string/replace like-name #"%" "")
         ; not interested in these values
         {:a (dissoc distinct-fields-a :bairros :locais)
          :b (dissoc distinct-fields-b :bairros :locais)}}))))

(defn map-reports-to-weekdays
  "Loop over the queryset mapping the report to it's respective weekday.
  Return an array-map with the weekdays as keys and a vector of reports as vals."
  [queryset]
  (reduce (fn [acc row]
            (let [weekday (c/long->weekday-str (:created_at row))]
              (update acc weekday conj row)))
          ; we need to provide an array-map, as we want the weekdays
          ; to stick to the following order
          (array-map "Segunda" [], "Terça" [], "Quarta" [], "Quinta" [],
                     "Sexta" [], "Sábado" [], "Domingo" [])
          queryset))

(defn process-weekdays
  "Calls `select-distinct-fields` for each day of the week, for their
  respective reports, returning a vector of such data"
  [queryset-a queryset-b weekdays?]
  (when weekdays?
    ; begin by associating the reports to their respective weekdays
    (let [[weekdays-reports-a weekdays-reports-b]
          (map map-reports-to-weekdays [queryset-a queryset-b])]
      ; this field is process
      (map (fn [[weekday-a rows-a] [weekday-b rows-b]]
             (let [[distinct-fields-a distinct-fields-b]
                   (map select-distinct-fields [rows-a rows-b])]
               ; return value
               {weekday-a {:a (dissoc distinct-fields-a :dias-da-semana)
                           :b (dissoc distinct-fields-b :dias-da-semana)}}))
           ; since we compare the distinc-fields of each queryset, we have to
           ; return the same weekday-reports of each queryset together
           (vec weekdays-reports-a) (vec weekdays-reports-b)))))

(defn process-report-data
  [{:keys [data-inicial-a data-final-a data-inicial-b data-final-b] :as params}]
  (let [params-a {:data-inicial data-inicial-a
                  :data-final data-final-a}
        params-b {:data-inicial data-inicial-b
                  :data-final data-final-b}
        [count-a count-b] (map #(first (db/get-crime-reports-count %)) [params-a params-b])
        [period-a period-b] (map db/get-crime-reports [params-a params-b])
        [distinct-fields-a distinct-fields-b]
        (map select-distinct-fields [period-a period-b])
        ; plots
        [pie-a pie-b]
        (map format-plot-pie-data
             ["naturezas a" "naturezas b"]
             (map :crimes [distinct-fields-a distinct-fields-b]))
        ; if those keys are not chosen their value is nil
        optionals-incidents
        (process-incidents period-a period-b
                           (remove #(nil? (second %))
                                   (select-keys params
                                                [:roubo :furto :trafico :homicidio])))
        optionals-neighborhood
        (process-neighborhood period-a period-b
                              (remove #(nil? (second %))
                                      (select-keys params [:bairro])))
        optionals-weekdays
        (process-weekdays period-a period-b (:weekday params))
        optionals-horarios
        (process-time-periods period-a period-b (:times params))]

    ;; result map
    {:total {:a (:count count-a)
             :b (:count count-b)
             :fluctuation (c/fluctuation (:count count-a) (:count count-b))}
     :crime-comparison (compare-incidents params-a params-b)
     :plots {:bar {:ids (keys distinct-fields-a)
                   :vals {:a (map format-plot-bar-data
                               (repeat "Período A")
                               (keys distinct-fields-a)
                               (vals distinct-fields-a))
                          :b (map format-plot-bar-data
                               (repeat "Período B")
                               (keys distinct-fields-b)
                               (vals distinct-fields-b))}}
             :pie {:ids ["naturezas a" "naturezas b"]
                   :vals {:a pie-a
                          :b pie-b}}}
     ; since these fields are optional they'll return nil if not selected
     :optionals (apply concat (remove nil? [optionals-incidents optionals-neighborhood
                                            optionals-weekdays optionals-horarios]))}))

; response handler ----------------------------------------------------------

(defn report-data [params]
  (response/ok
   (-> params
       (report-db-coercer)
       (process-report-data))))

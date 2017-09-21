(ns pmmt.routes.report
  (:require
    [clojure.string :as string]
    [ring.util.http-response :as response]
    [pmmt.routes.common :as c]
    [pmmt.db.core :as db]
    [pmmt.utils :refer [deep-merge-with]]))

; helpers -------------------------------------------------------------

(defn format-date [m]
  (-> (str (:day m) "/" (:month m) "/" (:year m))
      (c/str->java-date)))

(defn report-db-coercer
  [{:keys [data-inicial-a data-final-a data-inicial-b data-final-b] :as params}]
  (let [like-fn (fn [s] (and s (str "%" (c/NFKD s) "%")))
        str->coll (fn [s] (and s (clojure.edn/read-string s)))]
    (-> params
        (update-in [:range1 :from] format-date)
        (update-in [:range1 :to] format-date)
        (update-in [:range2 :from] format-date)
        (update-in [:range2 :to] format-date)
        (update :roubo #(when % (map :id (c/ROUBO))))
        (update :furto #(when % (map :id (c/FURTO))))
        (update :trafico #(when % (map :id (c/TRAFICO))))
        (update :homicidio #(when % (map :id (c/HOMICIDIO))))
        (update :neighborhood like-fn))))

; core ----------------------------------------------------------------

(defn detailed-statistics [date-range params]
  {:crime-reports
   {:detailed nil}})
; For now this will be nil.
; I still have to think of a better way of generating this data.
; {:detailed
;  {:by-crime-type,
;   :by-weekday,
;   :by-period,
;   :by-neighborhood}}

(defn compare-fields [field key & ranges]
  (apply
    map
    (fn [m1 m2]
      {key (key m1)
        :old (:count m1)
        :new (:count m2)
        :increase (c/percentage-increase
                    (get m1 :count)
                    (get m2 :count))})
    (map #(get-in % [:crime-reports field]) ranges)))

(defn compare-data [key & data]
  (apply
    map
    (fn [m1 m2]
      {key (key m1)
       :old (:count m1)
       :new (:count m2)
       :increase (c/percentage-increase
                   (get m1 :count)
                   (get m2 :count))})
    data))

(defn crime-groups [m]
  (cond
    (string/includes? (:type m) "ROUBO") "ROUBO"
    (string/includes? (:type m) "FURTO") "FURTO"
    (string/includes? (:type m) "HOMICIDIO") "HOMICIDIO"
    (string/includes? (:type m) "TRAFICO") "TRAFICO"
    (string/includes? (:type m) "DROGAS") "DROGAS"))

(defn group-crimes [& rows]
  (for [row rows]
    (map second
         (reduce (fn [acc m]
                   (if-let [key (crime-groups m)]
                     (if (get m key)
                       (update-in acc [key :count] + (:count m))
                       (assoc acc key {:type key :count (:count m)}))
                     acc))
                 {} row))))

(defn compare [range1 range2]
  (let [ranges (map #(get-in % [:crime-reports :by-crime-type]) [range1 range2])]
    {:count
     {:old (get-in range1 [:crime-reports :count])
      :new (get-in range2 [:crime-reports :count])
      :increase (c/percentage-increase
                  (get-in range1 [:crime-reports :count])
                  (get-in range2 [:crime-reports :count]))},
     :by-crime-type
     (apply compare-data :type
            (apply group-crimes
                   (map #(get-in % [:crime-reports :by-crime-type]) [range1 range2])))

     :by-weekday
     (compare-fields :by-weekday :weekday range1 range2),
     :by-period
     (compare-fields :by-period :period range1 range2)}))

(defn process-range [date-range params]
  (when date-range
    (deep-merge-with
      merge
      date-range
      ;; Regular statistics
      {:crime-reports
       {:count
        (-> (db/get-crime-reports-count date-range) first :count),
        :by-crime-type
        (db/get-crime-reports-by
         (assoc date-range :field "c.type" :limit 10)),
        :by-neighborhood
        (db/get-crime-reports-by
         (assoc date-range :field "cr.neighborhood" :limit 10)),
        :by-weekday
        (db/get-crime-reports-by-weekday date-range),
        :by-route
        (db/get-crime-reports-by-route
         (assoc date-range :limit 10)),
        :by-period
        (db/get-crime-reports-by-period date-range),
        :by-date
        (db/get-crime-reports-by-date date-range)
        :by-hour
        (db/get-crime-reports-by-hour date-range)}}
      ;; Optional statistics
      (detailed-statistics date-range params))))

(defn process-report-data [params]
  (let [range1 (process-range (:range1 params) params)
        range2 (when-let [range2 (:range2 params)]
                 (process-range range2 params))]
    {:ranges [range1 range2]
     :compare (compare range1 range2)}))

; response handler ----------------------------------------------------------

(defn report-data [params]

  (response/ok
   (-> params
       (report-db-coercer)
       (process-report-data))))

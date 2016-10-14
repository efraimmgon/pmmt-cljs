;; db connection
(ns user
  (:require [mount.core :as mount]
            pmmt.core))

(defn start []
  (mount/start-without #'pmmt.core/repl-server))

(defn stop []
  (mount/stop-except #'pmmt.core/repl-server))

(defn restart []
  (stop)
  (start))

(start)

;; rebind conman connection
(in-ns 'pmmt.db.core)
(conman/bind-connection *db* "sql/queries.sql" "sql/analise_criminal.sql")

;; commonly used modules
(require '[pmmt.db.core :as db :refer [*db*]])
(require '[clojure.java.jdbc :as sql])
(require '[schema.core :as s])

(sql/query *db* ["SELECT * FROM ocorrencia LIMIT 5"])

(s/validate
 [{:id s/Int, :nome s/Str}]
 (db/get-cities))

(let [result
      (reduce (fn [acc m]
                (conj acc
                  {:position {:lat (:latitude m)
                              :lng (:longitude m)}
                   :title (:local m)}))
              [] (sql/query *db* ["SELECT * FROM ocorrencia WHERE latitude IS NOT NULL LIMIT 5"]))]
  (println result))

;;
(require '[pmmt.components.geo :as g] :reload)
(doseq [marker g/sample-marker-data]
  (-> marker (g/create-gmaps-marker) (.setMap @g/gmap)))

(defonce sample-marker-data
  (reduce (fn [acc m]
            (conj acc
                  {:position {:lat (:latitude m), :lng (:longitude m)}
                   :title (:local m)}))
          [] (g/get-sample-data)))

(start)
(require '[myproject.db.core :as db])
(db/some-function)

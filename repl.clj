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
(conman/bind-connection *db* "sql/queries.sql" "sql/analise.sql")


;; commonly used modules
(require '[pmmt.db.core :as db :refer [*db*]])
(require '[clojure.java.jdbc :as sql])
(require '[schema.core :as s])
(require '[schema.coerce :as sc])
(require '[clj-time.core :as t])
(require '[clj-time.coerce :as tc])
(require '[pmmt.routes.common :as c])

(sql/query *db* ["SELECT count(*) FROM ocorrencia"])
(sql/query *db* ["select * from natureza"])
(println (sql/query *db* ["SELECT * FROM ocorrencia order by id desc LIMIT 1"]))
(def rows (sql/query *db* ["select * from ocorrencia where hora is null limit 10"]))
(sql/query *db* ["select * from ocorrencia where via='Rua dos Esportes'"])


;;; ClojureScript

(require '[pmmt.components.map :as m])
(require '[pmmt.components.admin :as a])
(require '[pmmt.components.geo :as g])
(in-ns 'pmmt.components.geo)

;;; playground

(println
 (str "Basic "
      (.encodeToString
       (java.util.Base64/getEncoder)
       (.getBytes "user:pass"))))


(str "Basic "
     (.encodeToString
      (java.util.Base64/getEncoder)
      (.getBytes "efraimmgon:chefedoano")))

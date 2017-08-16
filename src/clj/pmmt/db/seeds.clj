(ns pmmt.db.seeds
  (:require
   [buddy.hashers :as hashers]
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [pmmt.db.core :as db]
   [pmmt.routes.common :as c]
   [pmmt.routes.services.upload :as upload]
   [pmmt.utils :refer [domap]]))

; -------------------------------------------------------------------
; Utils
; -------------------------------------------------------------------

(defn read-csv [f]
  (upload/csv->map (csv/read-csv f)))

(defn samples-pathname [project-root]
  (io/file project-root "resources" "db" "samples"))

; -------------------------------------------------------------------
; Sample Data
; -------------------------------------------------------------------

(defn users []
  [{:id "admin", :pass (hashers/encrypt "admin")}])

(defn crimes []
  (read-csv (-> (c/project-root) (samples-pathname) (io/file "crimes.csv") io/reader)))

(defn cities []
  (read-csv (-> (c/project-root) (samples-pathname) (io/file "cities.csv") io/reader)))

(defn modes-desc []
  (read-csv (-> (c/project-root) (samples-pathname) (io/file "modes-desc.csv") io/reader)))

; -------------------------------------------------------------------
; Core
; -------------------------------------------------------------------

(defn reset-db! []
  (db/delete-all-crime-reports!)
  (db/delete-all-crimes!)
  (db/delete-all-modes-desc!)
  (db/delete-all-cities!)
  (db/delete-all-users!))

(defn insert-data! []
  (domap db/create-user! (users))
  (domap db/create-crime! (crimes))
  (domap db/create-city! (cities))
  (domap db/create-mode-desc! (modes-desc)))

(defn run-seeds! []
  (reset-db!)
  (insert-data!))

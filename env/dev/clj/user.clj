(ns user
  (:require [mount.core :as mount]
            [pmmt.figwheel :refer [start-fw stop-fw cljs]]
            pmmt.core))

(defn start []
  (mount/start-without #'pmmt.core/repl-server))

(defn stop []
  (mount/stop-except #'pmmt.core/repl-server))

(defn restart []
  (stop)
  (start))



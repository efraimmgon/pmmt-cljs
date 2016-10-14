(ns pmmt.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [pmmt.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[pmmt started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[pmmt has shut down successfully]=-"))
   :middleware wrap-dev})

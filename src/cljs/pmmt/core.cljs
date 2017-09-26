(ns pmmt.core
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame :refer [dispatch-sync]]
            [pmmt.routes :refer [hook-browser-navigation!]]
            [pmmt.views :refer [page]]
            ;; register handlers and subscriptions
            pmmt.handlers
            pmmt.subs))

;; Initialize app
(defn mount-components []
  (r/render [#'page] (.getElementById js/document "app")))


(defn init! []
  (dispatch-sync [:set-initial-state])
  (hook-browser-navigation!)
  (mount-components))

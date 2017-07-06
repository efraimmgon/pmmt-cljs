(ns pmmt.routes
  (:require
   [goog.events :as events]
   [goog.history.EventType :as HistoryEventType]
   [re-frame.core :refer [dispatch]]
   [secretary.core :as secretary :include-macros true])
  (:import goog.History))

;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (dispatch [:page :home]))

(secretary/defroute "/admin" []
  (dispatch [:page :admin]))

(secretary/defroute "/utilitarios/" []
  (dispatch [:page :utilitarios]))

(secretary/defroute "/biblioteca/" []
  (dispatch [:page :biblioteca]))

(secretary/defroute "/analise-criminal/relatorio/" []
  (dispatch [:page :relatorio]))

(secretary/defroute "/analise-criminal/geo/" []
  (dispatch [:page :geo]))

;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
        (events/listen
          HistoryEventType/NAVIGATE
          (fn [event]
              (secretary/dispatch! (.-token event))))
        (.setEnabled true)))

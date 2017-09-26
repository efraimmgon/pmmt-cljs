(ns pmmt.routes
  (:require
   [accountant.core :as accountant]
   [goog.events :as events]
   [goog.history.EventType :as HistoryEventType]
   [re-frame.core :refer [dispatch subscribe]]
   [secretary.core :as secretary :include-macros true])
  (:import goog.History))

(defn logged-in? []
  @(subscribe [:identity]))

(defn run-events [events]
  (doseq [event events]
    (if (logged-in?)
      (dispatch event)
      (dispatch [:add-login-event event]))))

(defn context-url [url]
  (str js/context url))

(defn navigate! [url]
  (accountant/navigate! (context-url url)))

(defn admin-page-events [& events]
  (.scrollTo js/window 0 0)
  (run-events (into
                [[:page :admin]]
                events)))

; ------------------------------------------------------------------------------
; Routes
; ------------------------------------------------------------------------------

(secretary/defroute "/" []
  (dispatch [:page :home]))

(secretary/defroute "/admin" []
  (admin-page-events [:admin/set-active-panel "Dashboard" :dashboard]))

(secretary/defroute "/admin/dashboard" []
  (admin-page-events [:admin/set-active-panel "Dashboard" :dashboard]))

(secretary/defroute "/admin/database" []
  (admin-page-events [:admin/set-active-panel "Database" :database]))

(secretary/defroute "/admin/users" []
  (admin-page-events [:admin/set-active-panel "Users" :users]))

(secretary/defroute "/admin/geo" []
  (admin-page-events [:admin/set-active-panel "Georeferencing" :geo]))

(secretary/defroute "/admin/report" []
  (admin-page-events [:admin/set-active-panel "Criminal report" :report]))


(secretary/defroute "/utils" []
  (dispatch [:page :utilitarios]))

; ------------------------------------------------------------------------------
; History
; must be called after routes have been defined
; ------------------------------------------------------------------------------

(defn hook-browser-navigation! []
  (doto (History.)
        (events/listen
          HistoryEventType/NAVIGATE
          (fn [event]
              (secretary/dispatch! (.-token event))))
        (.setEnabled true))
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!))

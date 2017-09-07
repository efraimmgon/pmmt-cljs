(ns pmmt.pages.admin.core
  (:require
   [reagent.core :as r :refer [atom]]
   [re-frame.core :as re-frame :refer [subscribe dispatch]]
   [pmmt.components.common :as c]
   [pmmt.pages.admin.dashboard :refer [dashboard-template]]
   [pmmt.pages.admin.database :as database]
   [pmmt.pages.admin.navbar :as navbar]
   [pmmt.pages.admin.users :refer [users-template]]))

; -----------------------------------------------------------------
; Local State
; -----------------------------------------------------------------

(defonce local-state
  (atom {:setup-ready? false}))

; -----------------------------------------------------------------
; Setup
; -----------------------------------------------------------------

(defn load-styles! []
  ;; Animation library for notifications
  (c/add-style! {:href "/css/animate.min.css"})
  ;; Light Bootstrap Table core CSS
  (c/add-style! {:href "/css/light-bootstrap-dashboard.css"})
  ;; Fonts and icons
  (c/add-style! {:href "http://maxcdn.bootstrapcdn.com/font-awesome/4.2.0/css/font-awesome.min.css"})
  (c/add-style! {:href "http://fonts.googleapis.com/css?family=Roboto:400,700,300"})
  (c/add-style! {:href "/css/pe-icon-7-stroke.css"}))

(defn setup! []
  (let [setup-ready? (r/cursor local-state [:setup-ready?])]
    (when-not @setup-ready?
      (reset! setup-ready? true))))

; -----------------------------------------------------------------
; Aux. Components
; -----------------------------------------------------------------

(defn page-heading []
  (r/with-let [active-page (subscribe [:admin/active-page])]
    [:div.row
     [:div.col-lg-12
      [:h1.page-header
       @active-page]
      [:ol.breadcrumb
       [:li.active
        [:i.fa.fa-dashboard] " "
        @active-page]]]]))

; -----------------------------------------------------------------
; Navigation
; -----------------------------------------------------------------

(def panels
  {;:dashboard dashboard
   :database database/main
   :users users-template})

(defn main-content []
  (let [active-panel (subscribe [:admin/active-panel])]
    (fn []
      [:div#page-wrapper
       [:div.container-fluid
        [page-heading]
        [(panels @active-panel)]]])))

; -----------------------------------------------------------------
; Main
; -----------------------------------------------------------------

;; TODO: change name
(defn main-template []
  (setup!)
  (fn []
    [dashboard-template]))

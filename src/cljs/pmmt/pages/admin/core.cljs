(ns pmmt.pages.admin.core
  (:require
   [reagent.core :as r :refer [atom]]
   [re-frame.core :as re-frame :refer [subscribe dispatch]]
   [pmmt.components.common :as c]
   [pmmt.pages.admin.dashboard :refer [dashboard]]
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

(defn load-scripts! []
  (c/add-style! {:href "/css/sb-admin.css"})
  (c/add-style! {:href "/css/plugins/morris.css"}))

(defn setup! []
  (let [setup-ready? (r/cursor local-state [:setup-ready?])]
    (when-not @setup-ready?
      (load-scripts!)
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
  {:dashboard dashboard
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

(def scripts
  {#(exists? js/Raphael) "/js/plugins/morris/raphael.min.js"
   #(exists? js/Morris) "/js/plugins/morris/morris.min.js"})

;; TODO: change name
(defn main-template []
  (setup!)
  (fn []
    [c/js-loader
     {:scripts scripts
      :loading [:div.loading "Loading..."]
      :loaded [:div#wrapper
               [navbar/main]
               [main-content]]}]))

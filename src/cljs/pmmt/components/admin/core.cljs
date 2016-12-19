(ns pmmt.components.admin.core
  (:require [clojure.string :as string]
            [reagent.core :as r :refer [atom]]
            [re-frame.core :as re-frame :refer
             [subscribe dispatch]]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [cljs-dynamic-resources.core :as cdr]
            [pmmt.components.common :as c]
            [pmmt.components.admin.handlers]
            [pmmt.components.admin.dashboard :refer [dashboard]]
            [pmmt.components.admin.database :refer
             [database-panel-interface]]
            [pmmt.components.admin.navbar :refer [navbar]]
            [pmmt.components.admin.users :refer [users-interface]]))

; local state ------------------------------------------------------------

(defonce local-state
  (atom {:setup-ready? false}))

; misc -------------------------------------------------------------------

(defn load-scripts! []
  (cdr/add-scripts!
   [{:src "/js/plugins/morris/raphael.min.js" :opts {:id "raphael-js"}}
    {:src "/js/plugins/morris/morris.min.js" :opts {:id "morris-js"}}
    {:src "/js/plugins/morris/morris-data.js" :opts {:id "morris-data-js"}}])
  (cdr/add-style! "/css/sb-admin.css" nil {:id "sb_admin"})
  (cdr/add-style! "/css/plugins/morris.css" nil {:id "morris_css"}))

(defn setup! []
  (let [setup-ready? (r/cursor local-state [:setup-ready?])]
    (when-not @setup-ready?
      (load-scripts!)
      (reset! setup-ready? true))))

; aux. components ---------------------------------------------------------

(defn page-heading []
  (let [active-page (subscribe [:admin/active-page])]
    (fn []
      [:div.row
       [:div.col-lg-12
        [:h1.page-header
         @active-page]
        [:ol.breadcrumb
         [:li.active
          [:i.fa.fa-dashboard] " "
          @active-page]]]])))

; main components ---------------------------------------------------------

(def panels
  {:dashboard dashboard
   :database database-panel-interface
   :users users-interface})

(defn main-content []
  (let [active-panel (subscribe [:admin/active-panel])]
    (fn []
      [:div#page-wrapper
       [:div.container-fluid
        [page-heading]
        [(panels @active-panel)]]])))

(defn admin-page []
  (setup!)
  (fn []
    [:div#wrapper
     ; "<!-- Navigation -->"
     [navbar]
     ; "<!-- main content -->"
     [main-content]]))

(ns pmmt.pages.admin.core
  (:require
   [reagent.core :as r :refer [atom]]
   [re-frame.core :as re-frame :refer [subscribe dispatch]]
   [pmmt.components.common :as c]
   [pmmt.components.admin.dashboard :refer [dashboard]]
   [pmmt.components.admin.database :refer
     [database-panel-interface]]
   [pmmt.components.admin.navbar :refer [navbar]]
   [pmmt.components.admin.users :refer [users-interface]]))

; local state ------------------------------------------------------------

(defonce local-state
  (atom {:setup-ready? false}))

; Setup ------------------------------------------------------------------

(defn load-scripts! []
  (c/add-script! {:src "/js/plugins/morris/morris-data.js"})
  (c/add-style! {:href "/css/sb-admin.css"})
  (c/add-style! {:href "/css/plugins/morris.css"}))

(defn setup! []
  (let [setup-ready? (r/cursor local-state [:setup-ready?])]
    (when-not @setup-ready?
      (load-scripts!)
      (reset! setup-ready? true))))

; aux. components --------------------------------------------------------

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

(defn main-page []
  (setup!)
  (fn []
    [:div#wrapper
     [navbar]
     [main-content]]))

(def scripts
  {#(exists? js/Raphael) "/js/plugins/morris/raphael.min.js"
   #(exists? js/Morris) "/js/plugins/morris/morris.min.js"})

(defn main-page []
  (fn []
    (setup!)
    [c/js-loader
     {:scripts {#(exists? js/Stripe) "https://js.stripe.com/v2/"}
      :loading [:img {:src "https://media.giphy.com/media/3oEjI6SIIHBdRxXI40/giphy.gif"}]
      :loaded [:div#wrapper
               [navbar]
               [main-content]]}]))

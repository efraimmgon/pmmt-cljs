(ns pmmt.pages.admin.core
  (:require
   [reagent.core :as r :refer [atom]]
   [re-frame.core :as re-frame :refer [subscribe dispatch]]
   [pmmt.components.common :as c]
   [pmmt.pages.admin.dashboard :as dashboard]
   [pmmt.pages.admin.database :as database]
   [pmmt.pages.admin.geoprocessing :as geo]
   [pmmt.pages.admin.report :as report]
   [pmmt.pages.admin.users :as users]))

; -----------------------------------------------------------------------
; NAVBAR
; -----------------------------------------------------------------------

(defn navbar []
  (r/with-let [active-panel (subscribe [:admin/active-panel])
               active-panel-title (subscribe [:admin/active-panel-title])]
    [:nav.navbar.navbar-default
     [:div.container-fluid
      [:div.navbar-header
       [:button.navbar-toggle
        {:data-target "#navigation-example-2",
         :data-toggle "collapse",
         :type "button"}
        [:span.sr-only "Toggle navigation"]
        [:span.icon-bar]
        [:span.icon-bar]
        [:span.icon-bar]]
       [:a.navbar-brand {:href (str "/admin/" (name @active-panel))}
        @active-panel-title]]
      [:div.collapse.navbar-collapse
       [:ul.nav.navbar-nav.navbar-left
        [:li
         [:a.dropdown-toggle
          {:data-toggle "dropdown", :href "#"}
          [:i.fa.fa-dashboard]]
         [:p.hidden-lg.hidden-md "Dashboard"]]
        [:li.dropdown
         [:a.dropdown-toggle
          {:data-toggle "dropdown", :href "#"}
          [:i.fa.fa-globe]
          [:b.caret.hidden-sm.hidden-xs]
          [:span.notification.hidden-sm.hidden-xs "5"]]
         [:p.hidden-lg.hidden-md
          "5 Notifications"
          [:b.caret]]
         [:ul.dropdown-menu
          [:li [:a {:href "#"} "Notification 1"]]
          [:li [:a {:href "#"} "Notification 2"]]
          [:li [:a {:href "#"} "Notification 3"]]
          [:li [:a {:href "#"} "Notification 4"]]
          [:li [:a {:href "#"} "Another notification"]]]]
        [:li
         [:a {:href ""} [:i.fa.fa-search]]
         [:p.hidden-lg.hidden-md "Search"]]]
       [:ul.nav.navbar-nav.navbar-right
        [:li [:a {:href ""} [:p "Account"]]]
        [:li.dropdown
         [:a.dropdown-toggle {:data-toggle "dropdown", :href "#"}
          [:p
           "Dropdown"
           [:b.caret]]]
         [:ul.dropdown-menu
          [:li [:a {:href "#"} "Action"]]
          [:li [:a {:href "#"} "Another action"]]
          [:li [:a {:href "#"} "Something"]]
          [:li [:a {:href "#"} "Another action"]]
          [:li [:a {:href "#"} "Something"]]
          [:li.divider]
          [:li [:a {:href "#"} "Separated link"]]]]
        [:li [:a {:href "#"} [:p "Log out"]]]
        [:li.separator.hidden-lg.hidden-md]]]]]))

; -----------------------------------------------------------------------
; SIDEBAR
; -----------------------------------------------------------------------

(defn sidebar-item [id title fav-icon active-panel]
  [:li {:class (when (= @active-panel id) "active")}
   [:a {:href (str "/admin/" (name id))}
    fav-icon
    [:p title]]])

(defn sidebar []
  (r/with-let [active-panel (subscribe [:admin/active-panel])
               color-palette (subscribe [:settings/sidebar-color-palette])
               background-image (subscribe [:settings/sidebar-background-image])]
    [:div.sidebar
     ; Tip 1: you can change the color of the sidebar using:
     ; data-color=\"blue | azure | green | orange | red | purple\"
     ; Tip 2: you can also add an image using data-image tag
     {:data-image @background-image :data-color @color-palette}
     [:div.sidebar-wrapper
      [:div.logo
       [:a.logo-text
        {:href "http://www.pm.mt.gov.br"}
        "PMMT"]]
      [:ul.nav
       [sidebar-item :dashboard "Dashboard" [:i.pe-7s-graph] active-panel]
       [sidebar-item :database "Database" [:i.pe-7s-note2] active-panel]
       [sidebar-item :users "Users" [:i.pe-7s-user] active-panel]
       [sidebar-item :geo "Georeferencing" [:i.pe-7s-map-marker] active-panel]
       [sidebar-item :report "Criminal report" [:i.pe-7s-note2] active-panel]]]

     [:div.sidebar-background {:style {:background-image (str "url(" @background-image ")")}}]]))

; -----------------------------------------------------------------------
; FOOTER
; -----------------------------------------------------------------------

(defn footer []
  [:footer.footer
   [:div.container-fluid
    [:nav.pull-left
     [:ul
      [:li
       [:a {:href "/"} "Home"]]]]
    [:p.copyright.pull-right
     "© "
     (.getFullYear (js/Date.))
     " "
     [:a {:href "http://www.google.com"} "Laconic Projects"]]]])

; -----------------------------------------------------------------
; Content navigation
; -----------------------------------------------------------------

(def panels
  {:dashboard dashboard/content
   :database database/content
   :users users/content
   :geo geo/content
   :report report/content})

(defn content [active-panel]
  [(get panels @active-panel)])

; -----------------------------------------------------------------
; Main
; -----------------------------------------------------------------

(defn main-template []
  (r/with-let [active-panel (subscribe [:admin/active-panel])]
    [:div.wrapper
     [sidebar]
     [:div.main-panel
      [navbar]
      [content active-panel]
      [footer]]]))

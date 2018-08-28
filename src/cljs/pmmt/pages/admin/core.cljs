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
      [:div.navbar-minimize
       [:button#minimizeSidebar.btn.btn-warning.btn-fill.btn-round.btn-icon
        [:i.fa.fa-ellipsis-v.visible-on-sidebar-regular]
        [:i.fa.fa-navicon.visible-on-sidebar-mini]]]
      [:div.navbar-header
       [:button.navbar-toggle
        {:data-toggle "collapse", :type "button"}
        [:span.sr-only "Toggle navigation"]
        [:span.icon-bar]
        [:span.icon-bar]
        [:span.icon-bar]]
       [:a.navbar-brand {:href (str "/admin/" (name @active-panel))}
        @active-panel-title]]
      [:div.collapse.navbar-collapse
       [:form.navbar-form.navbar-left.navbar-search-form
        {:role "search"}
        [:div.input-group
         [:span.input-group-addon [:i.fa.fa-search]]
         [:input.form-control
          {:placeholder "Search...", :value "", :type "text"}]]]
       [:ul.nav.navbar-nav.navbar-right
        [:li.dropdown.dropdown-with-icons
         [:a.dropdown-toggle
          {:data-toggle "dropdown", :href "#"}
          [:i.fa.fa-list]]
         [:p.hidden-md.hidden-lg
          "\n\t\t\t\t\t\t\t\t\tMore\n\t\t\t\t\t\t\t\t\t"
          [:b.caret]]
         [:ul.dropdown-menu.dropdown-with-icons
          [:li
           [:a
            {:href "#"}
            [:i.pe-7s-tools]
            " Settings"]]
          [:li.divider]
          [:li
           [:a
            {:href "#"}
            [:i.pe-7s-lock]
            " Lock Screen"]]
          [:li
           [:a.text-danger
            {:href "#"}
            [:i.pe-7s-close-circle]
            "Log out"]]]]]]]]))

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
        {:href "http://localhost:3000"}
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

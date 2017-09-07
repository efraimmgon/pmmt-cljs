(ns pmmt.pages.admin.dashboard
  (:require
   [reagent.core :as r]))

; -----------------------------------------------------------------------
; SIDEBAR
; -----------------------------------------------------------------------

(defn sidebar []
  [:div.sidebar
   ; Tip 1: you can change the color of the sidebar using:
   ; data-color=\"blue | azure | green | orange | red | purple\"
   ; Tip 2: you can also add an image using data-image tag
   {:data-image "/img/sidebar-5.jpg" :data-color "purple"}
   [:div.sidebar-wrapper
    [:div.logo
     [:a.simple-text
      {:href "http://www.pm.mt.gov.br"}
      "PMMT"]]
    [:ul.nav
     [:li.active
      [:a {:href "dashboard.html"}
       [:i.pe-7s-graph]
       [:p "Dashboard"]]]
     [:li
      [:a {:href "user.html"}
       [:i.pe-7s-user]
       [:p "User Profile"]]]
     [:li
      [:a {:href "table.html"}
       [:i.pe-7s-note2]
       [:p "Table List"]]]
     [:li
      [:a {:href "typography.html"}
       [:i.pe-7s-news-paper]
       [:p "Typography"]]]
     [:li
      [:a {:href "icons.html"}
       [:i.pe-7s-science]
       [:p "Icons"]]]
     [:li
      [:a {:href "maps.html"}
       [:i.pe-7s-map-marker]
       [:p "Maps"]]]
     [:li
      [:a {:href "notifications.html"}
       [:i.pe-7s-bell]
       [:p "Notifications"]]]
     [:li.active-pro
      [:a {:href "upgrade.html"}
       [:i.pe-7s-rocket]
       [:p "Upgrade to PRO"]]]]]
   [:div.sidebar-background {:style {:background-image "url(/img/sidebar-5.jpg)"}}]])

; -----------------------------------------------------------------------
; NAVBAR
; -----------------------------------------------------------------------

(defn navbar []
  [:nav.navbar.navbar-default.navbar-fixed
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
     [:a.navbar-brand {:href "#"} "Dashboard"]]
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
        "\n\t\t\t\t\t\t\t\t\t\t5 Notifications\n\t\t\t\t\t\t\t\t\t\t"
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
         "\n\t\t\t\t\t\t\t\t\t\tDropdown\n\t\t\t\t\t\t\t\t\t\t"
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
      [:li.separator.hidden-lg.hidden-md]]]]])

; -----------------------------------------------------------------------
; CONTENT
; -----------------------------------------------------------------------

(defn email-statistics []
  [:div.card
   [:div.header
    [:h4.title "Email Statistics"]
    [:p.category "Last Campaign Performance"]]
   [:div.content
    [:div#chartPreferences.ct-chart.ct-perfect-fourth]
    [:div.footer
     [:div.legend
      [:i.fa.fa-circle.text-info]
      "Open"
      [:i.fa.fa-circle.text-danger]
      "Bounce"
      [:i.fa.fa-circle.text-warning]
      "Unsubscribe"]
     [:hr]
     [:div.stats
      [:i.fa.fa-clock-o]
      "Campaign sent 2 days ago"]]]])

(defn user-behavior []
  [:div.card
   [:div.header
    [:h4.title "Users Behavior"]
    [:p.category "24 Hours performance"]]
   [:div.content
    [:div#chartHours.ct-chart]
    [:div.footer
     [:div.legend
      [:i.fa.fa-circle.text-info]
      "Open"
      [:i.fa.fa-circle.text-danger]
      "Click"
      [:i.fa.fa-circle.text-warning]
      "Click Second Time"]
     [:hr]
     [:div.stats
      [:i.fa.fa-history]
      "Updated 3 minutes ago"]]]])

(defn sales []
  [:div.card
   [:div.header
    [:h4.title "2014 Sales"]
    [:p.category "All products including Taxes"]]
   [:div.content
    [:div#chartActivity.ct-chart]
    [:div.footer
     [:div.legend
      [:i.fa.fa-circle.text-info]
      "Tesla Model S"
      [:i.fa.fa-circle.text-danger]
      "BMW 5 Series"]
     [:hr]
     [:div.stats
      [:i.fa.fa-check]
      "Data information certified"]]]])

(defn tasks []
  [:div.card
   [:div.header
    [:h4.title "Tasks"]
    [:p.category "Backend development"]]
   [:div.content
    [:div.table-full-width
     [:table.table
      [:tbody
       [:tr
        [:td
         [:label.checkbox
          [:input
           {:data-toggle "checkbox",
            :value "",
            :type "checkbox"}]]]
        [:td
         "Sign contract for \"What are conference organizers afraid of?\""]
        [:td.td-actions.text-right
         [:button.btn.btn-info.btn-simple.btn-xs
          {:title "Edit Task", :rel "tooltip", :type "button"}
          [:i.fa.fa-edit]]
         [:button.btn.btn-danger.btn-simple.btn-xs
          {:title "Remove", :rel "tooltip", :type "button"}
          [:i.fa.fa-times]]]]
       [:tr
        [:td
         [:label.checkbox
          [:input
           {:checked "",
            :data-toggle "checkbox",
            :value "",
            :type "checkbox"}]]]
        [:td
         "Lines From Great Russian Literature? Or E-mails From My Boss?"]
        [:td.td-actions.text-right
         [:button.btn.btn-info.btn-simple.btn-xs
          {:title "Edit Task", :rel "tooltip", :type "button"}
          [:i.fa.fa-edit]]
         [:button.btn.btn-danger.btn-simple.btn-xs
          {:title "Remove", :rel "tooltip", :type "button"}
          [:i.fa.fa-times]]]]
       [:tr
        [:td
         [:label.checkbox
          [:input
           {:checked "",
            :data-toggle "checkbox",
            :value "",
            :type "checkbox"}]]]
        [:td
         "Flooded: One year later, assessing what was lost and what was found when a ravaging rain swept through metro Detroit\n"]
        [:td.td-actions.text-right
         [:button.btn.btn-info.btn-simple.btn-xs
          {:title "Edit Task", :rel "tooltip", :type "button"}
          [:i.fa.fa-edit]]
         [:button.btn.btn-danger.btn-simple.btn-xs
          {:title "Remove", :rel "tooltip", :type "button"}
          [:i.fa.fa-times]]]]
       [:tr
        [:td
         [:label.checkbox
          [:input
           {:data-toggle "checkbox",
            :value "",
            :type "checkbox"}]]]
        [:td
         "Create 4 Invisible User Experiences you Never Knew About"]
        [:td.td-actions.text-right
         [:button.btn.btn-info.btn-simple.btn-xs
          {:title "Edit Task", :rel "tooltip", :type "button"}
          [:i.fa.fa-edit]]
         [:button.btn.btn-danger.btn-simple.btn-xs
          {:title "Remove", :rel "tooltip", :type "button"}
          [:i.fa.fa-times]]]]
       [:tr
        [:td
         [:label.checkbox
          [:input
           {:data-toggle "checkbox",
            :value "",
            :type "checkbox"}]]]
        [:td "Read \"Following makes Medium better\""]
        [:td.td-actions.text-right
         [:button.btn.btn-info.btn-simple.btn-xs
          {:title "Edit Task", :rel "tooltip", :type "button"}
          [:i.fa.fa-edit]]
         [:button.btn.btn-danger.btn-simple.btn-xs
          {:title "Remove", :rel "tooltip", :type "button"}
          [:i.fa.fa-times]]]]
       [:tr
        [:td
         [:label.checkbox
          [:input
           {:data-toggle "checkbox",
            :value "",
            :type "checkbox"}]]]
        [:td "Unfollow 5 enemies from twitter"]
        [:td.td-actions.text-right
         [:button.btn.btn-info.btn-simple.btn-xs
          {:title "Edit Task", :rel "tooltip", :type "button"}
          [:i.fa.fa-edit]]
         [:button.btn.btn-danger.btn-simple.btn-xs
          {:title "Remove", :rel "tooltip", :type "button"}
          [:i.fa.fa-times]]]]]]]
    [:div.footer
     [:hr]
     [:div.stats
      [:i.fa.fa-history]
      "Updated 3 minutes ago"]]]])

(defn content []
  [:div.content
   [:div.container-fluid
    [:div.row
     [:div.col-md-4
      [email-statistics]]
     [:div.col-md-8
      [user-behavior]]]
    [:div.row
     [:div.col-md-6
      [sales]]
     [:div.col-md-6
      [tasks]]]]])

; -----------------------------------------------------------------------
; FOOTER
; -----------------------------------------------------------------------

(defn footer []
  [:footer.footer
   [:div.container-fluid
    [:nav.pull-left
     [:ul
      [:li
       [:a
        {:href "#"}
        "Home"]]
      [:li
       [:a
        {:href "#"}
        "Company"]]
      [:li
       [:a
        {:href "#"}
        "Portfolio"]]
      [:li
       [:a
        {:href "#"}
        "Blog"]]]]
    [:p.copyright.pull-right
     "© "
     (.getFullYear (js/Date.))
     " "
     [:a {:href "http://www.creative-tim.com"} "Creative Tim"]
     ", made with love for a better web"]]])

; -----------------------------------------------------------------------
; Main
; -----------------------------------------------------------------------

(defn dashboard-template []
  [:div.wrapper
   [sidebar]
   [:div.main-panel
    [navbar]
    [content]
    [footer]]])

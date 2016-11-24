(ns pmmt.components.admin.navbar
  (:require [clojure.string :as string]
            [reagent.core :as r :refer [atom]]
            [re-frame.core :as re-frame :refer
             [subscribe dispatch]]))


; navbar aux. components ------------------------------------------------

(defn brand-toggle []
  "Brand and toggle get grouped for better mobile display"
  [:div.navbar-header
   [:button.navbar-toggle
    {:data-target ".navbar-ex1-collapse",
     :data-toggle "collapse",
     :type "button"}
    [:span.sr-only "Toggle navigation"]
    [:span.icon-bar]
    [:span.icon-bar]
    [:span.icon-bar]]
   [:a.navbar-brand {:href "#/"} "Home"]])

; top-menu-items ---------------------------------------------------------

(defn user-messages []
  (let [expanded? (atom false)]
    (fn []
      [:li.dropdown
       {:class (when @expanded? "open")
        :on-click #(swap! expanded? not)}
       [:a.dropdown-toggle {:href "javascript:;"}
        [:i.fa.fa-envelope]]
       [:ul.dropdown-menu.message-dropdown
        [:li.message-preview
         [:a {:href "javascript:;"}]
         [:div.media
          [:span.pull-left
           [:img.media-object
            {:alt "", :src "http://placehold.it/50x50"}]]
          [:div.media-body
           [:h5.media-heading [:strong "John Smith"]]
           [:p.small.text-muted
            [:i.fa.fa-clock-o]
            " Yesterday at 4:32 PM"]
           [:p "Lorem ipsum dolor sit amet, consectetur..."]]]]
        [:li.message-footer [:a {:href "javascript:;"} "Read All New Messages"]]]])))

; TODO: notifications
(defn user-notifications []
  (let [expanded? (atom false)]
    (fn []
      [:li.dropdown
       {:class (when @expanded? "open")
        :on-click #(swap! expanded? not)}
       [:a.dropdown-toggle
        {:data-toggle "dropdown"
         :href "javascript:;"}
        [:i.fa.fa-bell]]
       [:ul.dropdown-menu.alert-dropdown
        [:li
         [:a {:href "javascript:;"}
          "Alert Name "
          [:span.label.label-default "Alert Badge"]]]
        [:li.divider]
        [:li [:a {:href "javascript:;"} "Ver todas"]]]])))

(defn user-actions [user-id]
  (let [expanded? (atom false)]
    (fn []
      [:li.dropdown
       {:class (when @expanded? "open")
        :on-click #(swap! expanded? not)}
       [:a.dropdown-toggle
        {:data-toggle "dropdown"
         :href "javascript:;"}
        [:i.fa.fa-user] " "
        user-id]
       [:ul.dropdown-menu
        ; TODO
        [:li [:a {:href "javascript:;"} [:i.fa.fa-fw.fa-user] " Profile"]]
        ; TODO
        [:li [:a {:href "javascript:;"} [:i.fa.fa-fw.fa-envelope] " Inbox"]]
        ; TODO
        [:li [:a {:href "javascript:;"} [:i.fa.fa-fw.fa-gear] " Settings"]]
        [:li.divider]
        [:li
         [:a {:on-click #(dispatch [:logout])
              :href "javascript:;"}
          [:i.fa.fa-fw.fa-power-off] " Sair"]]]])))

(defn top-menu-items []
  (let [user-id (subscribe [:identity])]
    (fn []
      [:ul.nav.navbar-right.top-nav
       [user-messages]
       [user-notifications]
       [user-actions @user-id]])))

; sidebar-menu -----------------------------------------------------------

(defn sidebar-item [{:keys [fav-icon title panel]}]
  (let [active-panel (subscribe [:admin/active-panel])]
    (fn []
      [:li
       {:class (when (= panel @active-panel) "active")
        :style {:cursor :pointer}
        :on-click #(dispatch [:admin/set-active-panel title panel])}
       [:a fav-icon " " title]])))

(defn sidebar-menu-items
  "Sidebar Menu Items - These collapse to the responsive navigation menu
  on small screens"
  []
  [:div.collapse.navbar-collapse.navbar-ex1-collapse
   [:ul.nav.navbar-nav.side-nav
    [sidebar-item
     {:fav-icon [:i.fa.fa-fw.fa-dashboard]
      :title "Dashboard"
      :panel :dashboard}]
    [sidebar-item
     {:fav-icon [:i.fa.fa-fw.fa-table]
      :title "Base de dados"
      :panel :database}]]])

; main ----------------------------------------------------------------

(defn navbar []
  [:nav.navbar.navbar-inverse.navbar-fixed-top
   {:role "navigation"}
   ; "<!-- Brand and toggle get grouped for better mobile display -->"
   [brand-toggle]
   ; "<!-- Top Menu Items -->"
   [top-menu-items]
   ; "<!-- Sidebar Menu Items - These collapse to the responsive navigation menu on small screens -->"
   [sidebar-menu-items]])

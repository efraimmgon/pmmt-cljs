(ns pmmt.pages.admin.navbar
  (:require
   [reagent.core :as r :refer [atom]]
   [re-frame.core :refer [dispatch subscribe]]))

; Brand -----------------------------------------------------------

;;; Brand and toggle get grouped for better mobile display
(defn brand-toggle []
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

; Top Menu Items ----------------------------------------------------

(defn user-messages []
  (r/with-let [active-menu (subscribe [:admin.navbar/active-menu])
               id :user-messages]
    [:li.dropdown
     {:class (when (= @active-menu id) "open")
      :on-click #(dispatch [:admin.navbar/toggle-active-menu id])}
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
      [:li.message-footer [:a {:href "javascript:;"} "Read All New Messages"]]]]))

(defn user-notifications []
  (r/with-let [active-menu (subscribe [:admin.navbar/active-menu])
               id :user-notifications]
    [:li.dropdown
     {:class (when (= @active-menu id) "open")
      :on-click #(dispatch [:admin.navbar/toggle-active-menu id])}
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
      [:li [:a {:href "javascript:;"} "Ver todas"]]]]))

(defn user-actions [user-id]
  (r/with-let [active-menu (subscribe [:admin.navbar/active-menu])
               id :user-actions]
    [:li.dropdown
     {:class (when (= @active-menu id) "open")
      :on-click #(dispatch [:admin.navbar/toggle-active-menu id])}
     [:a.dropdown-toggle
      {:data-toggle "dropdown"
       :href "javascript:;"}
      [:i.fa.fa-user] " "
      user-id]
     [:ul.dropdown-menu
      ; ; TODO
      ; [:li [:a {:href "javascript:;"} [:i.fa.fa-fw.fa-user] " Profile"]]
      ; ; TODO
      ; [:li [:a {:href "javascript:;"} [:i.fa.fa-fw.fa-envelope] " Inbox"]]
      ; ; TODO
      ; [:li [:a {:href "javascript:;"} [:i.fa.fa-fw.fa-gear] " Settings"]]
      ; [:li.divider]
      [:li
       [:a {:on-click #(dispatch [:logout])
            :href "javascript:;"}
        [:i.fa.fa-fw.fa-power-off] " Sair"]]]]))

(defn top-menu-items []
  (r/with-let [user-id (subscribe [:identity])]
    [:ul.nav.navbar-right.top-nav
     ;[user-messages]
     ;[user-notifications]
     [user-actions @user-id]]))

; Sidebar Menu ------------------------------------------------------

(defn sidebar-item [{:keys [fav-icon title sidebar-id]}]
  (r/with-let [active-sidebar (subscribe [:admin/active-panel])]
    [:li
     {:class (when (= sidebar-id @active-sidebar) "active")
      :style {:cursor :pointer}
      :on-click #(dispatch [:admin/set-active-panel title sidebar-id])}
     [:a fav-icon " " title]]))

(defn sidebar-menu-items
  "Sidebar Menu Items - These collapse to the responsive navigation menu
  on small screens"
  []
  [:div.collapse.navbar-collapse.navbar-ex1-collapse
   [:ul.nav.navbar-nav.side-nav
    [sidebar-item
     {:fav-icon [:i.fa.fa-fw.fa-dashboard]
      :title "Painel"
      :sidebar-id :dashboard}]
    [sidebar-item
     {:fav-icon [:i.fa.fa-fw.fa-table]
      :title "Base de dados"
      :sidebar-id :database}]
    [sidebar-item
     {:fav-icon [:i.fa.fa-fw.fa-table]
      :title "Usuários"
      :sidebar-id :users}]]])

; -----------------------------------------------------------------
; Main
; -----------------------------------------------------------------

(defn main []
  [:nav.navbar.navbar-inverse.navbar-fixed-top
   {:role "navigation"}
   [brand-toggle]
   ;; TODO: Implement top menu items
   [top-menu-items]
   [sidebar-menu-items]])

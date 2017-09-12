(ns pmmt.pages.base
  (:require
   [reagent.core :as r :refer [atom]]
   [re-frame.core :as re-frame :refer [subscribe dispatch]]))

; -----------------------------------------------------------------------
; NAVBAR
; -----------------------------------------------------------------------

(defn account-actions [user]
  [:ul.nav.navbar-nav.navbar-right
   [:li.dropdown
    [:a.dropdown-toggle {:data-toggle "dropdown", :href "javascript:void(0)"}
     [:p
      [:i.pe-7s-user] " "
      @user " "
      [:i.caret]]]
    [:ul.dropdown-menu
     [:li
      [:a {:href "#/admin"}
       "Admin"]]
     [:li.divider]
     [:li [:a {:href "#"} "Separated link"]]]]
   [:li
    [:a {:href "javascript:void(0)" :on-click #(dispatch [:logout])}
     [:p "Logout"]]]
   [:li.separator.hidden-lg.hidden-md]])

(defn navbar []
  (r/with-let [user (subscribe [:identity])]
    [:nav.navbar.navbar-transparent.navbar-absolute
     [:div.container
      [:div.navbar-header
       [:button.navbar-toggle
        {:data-target "#navigation-example-2",
         :data-toggle "collapse",
         :type "button"}
        [:span.sr-only "Toggle navigation"]
        [:span.icon-bar]
        [:span.icon-bar]
        [:span.icon-bar]]
       [:a.navbar-brand
        {:href "http://pm.mt.gov.br"}
        "PMMT"]]
      [:div.collapse.navbar-collapse
       (if @user
         [:ul.nav.navbar-nav.navbar-right
          [:li [account-actions user]]]
         [:ul.nav.navbar-nav.navbar-right
          [:li
           [:a {:href "#/register"}
            "Register"]]])]]]))

; -----------------------------------------------------------------------
; FOOTER & BACKGOUND-IMAGE/COLOR-PALETTE
; -----------------------------------------------------------------------

(defn footer []
  (r/with-let [user (subscribe [:identity])
               color-palette (subscribe [:settings/page-color-palette])
               background-image (subscribe [:settings/page-background-image])]
    [:div
     [:footer.footer.footer-transparent
      [:div.container
       [:nav.pull-left
        [:ul
         [:li
          [:a {:href "#/"} "Home"]]
         (when-not @user
           [:li
            [:a {:href "#/register"} "Register"]])]]
       [:p.copyright.pull-right
        "© "
        (.getFullYear (js/Date.)) " "
        [:a {:href "http://www.google.com"} "Laconic Projects"]]]]
     [:div.full-page-background
      {:style {:background-image (str "url(" @background-image ")")
               :display "block"}}]]))


; -----------------------------------------------------------------------
; BASE
; -----------------------------------------------------------------------

(defn base-template [page]
  (r/with-let [color-palette (subscribe [:settings/page-color-palette])
               background-image (subscribe [:settings/page-background-image])]
    [:div
     [navbar]
     [:div.wrapper.wrapper-full-page
      [page color-palette background-image]]]))

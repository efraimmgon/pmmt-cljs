(ns pmmt.pages.admin.dashboard
  (:require
   [reagent.core :as r]))

; -----------------------------------------------------------------------
; Dashboard Components
; -----------------------------------------------------------------------

(defn content-summary []
  [:div.row
   [:div.col-lg-3.col-md-6
    [:div.panel.panel-primary
     [:div.panel-heading
      [:div.row
       [:div.col-xs-3 [:i.fa.fa-comments.fa-5x]]
       [:div.col-xs-9.text-right
        [:div.huge "26"]
        [:div "New Comments!"]]]]
     [:a {:href "#"}]
     [:div.panel-footer
      [:span.pull-left "View Details"]
      [:span.pull-right [:i.fa.fa-arrow-circle-right]]
      [:div.clearfix]]]]
   [:div.col-lg-3.col-md-6
    [:div.panel.panel-green
     [:div.panel-heading
      [:div.row
       [:div.col-xs-3 [:i.fa.fa-tasks.fa-5x]]
       [:div.col-xs-9.text-right
        [:div.huge "12"]
        [:div "New Tasks!"]]]]
     [:a {:href "#"}]
     [:div.panel-footer
      [:span.pull-left "View Details"]
      [:span.pull-right [:i.fa.fa-arrow-circle-right]]
      [:div.clearfix]]]]
   [:div.col-lg-3.col-md-6
    [:div.panel.panel-yellow
     [:div.panel-heading
      [:div.row
       [:div.col-xs-3 [:i.fa.fa-shopping-cart.fa-5x]]
       [:div.col-xs-9.text-right
        [:div.huge "124"]
        [:div "New Orders!"]]]]
     [:a {:href "#"}]
     [:div.panel-footer
      [:span.pull-left "View Details"]
      [:span.pull-right [:i.fa.fa-arrow-circle-right]]
      [:div.clearfix]]]]
   [:div.col-lg-3.col-md-6
    [:div.panel.panel-red
     [:div.panel-heading
      [:div.row
       [:div.col-xs-3 [:i.fa.fa-support.fa-5x]]
       [:div.col-xs-9.text-right
        [:div.huge "13"]
        [:div "Support Tickets!"]]]]
     [:a {:href "#"}]
     [:div.panel-footer
      [:span.pull-left "View Details"]
      [:span.pull-right [:i.fa.fa-arrow-circle-right]]
      [:div.clearfix]]]]])

(defn tasks-panel []
  [:div.row
    [:div.col-lg-12
     [:div.panel.panel-default
      [:div.panel-heading
       [:h3.panel-title [:i.fa.fa-clock-o.fa-fw] " Tasks Panel"]]
      [:div.panel-body
       [:div.list-group
        [:a.list-group-item
         {:href "#"}
         [:span.badge "just now"]
         [:i.fa.fa-fw.fa-calendar]
         " Calendar updated"]
        [:a.list-group-item
         {:href "#"}
         [:span.badge "4 minutes ago"]
         [:i.fa.fa-fw.fa-comment]
         " Commented on a post"]
        [:a.list-group-item
         {:href "#"}
         [:span.badge "23 minutes ago"]
         [:i.fa.fa-fw.fa-truck]
         " Order 392 shipped"]
        [:a.list-group-item
         {:href "#"}
         [:span.badge "46 minutes ago"]
         [:i.fa.fa-fw.fa-money]
         " Invoice 653 has been paid"]
        [:a.list-group-item
         {:href "#"}
         [:span.badge "1 hour ago"]
         [:i.fa.fa-fw.fa-user]
         " A new user has been added"]
        [:a.list-group-item
         {:href "#"}
         [:span.badge "2 hours ago"]
         [:i.fa.fa-fw.fa-check]
         " Completed task: \"pick up dry cleaning\""]
        [:a.list-group-item
         {:href "#"}
         [:span.badge "yesterday"]
         [:i.fa.fa-fw.fa-globe]
         " Saved the world"]
        [:a.list-group-item
         {:href "#"}
         [:span.badge "two days ago"]
         [:i.fa.fa-fw.fa-check]
         " Completed task: \"fix error on sales page\""]]
       [:div.text-right
        [:a
         {:href "#"}
         "View All Activity "
         [:i.fa.fa-arrow-circle-right]]]]]]])

(defn transactions-panel []
  [:div.row
    [:div.col-lg-12
     [:div.panel.panel-default
      [:div.panel-heading
       [:h3.panel-title
        [:i.fa.fa-money.fa-fw]
        " Transactions Panel"]]
      [:div.panel-body
       [:div.table-responsive
        [:table.table.table-bordered.table-hover.table-striped
         [:thead
          [:tr
           [:th "Order #"]
           [:th "Order Date"]
           [:th "Order Time"]
           [:th "Amount (USD)"]]]
         [:tbody
          [:tr
           [:td "3326"]
           [:td "10/21/2013"]
           [:td "3:29 PM"]
           [:td "$321.33"]]
          [:tr
           [:td "3325"]
           [:td "10/21/2013"]
           [:td "3:20 PM"]
           [:td "$234.34"]]
          [:tr
           [:td "3324"]
           [:td "10/21/2013"]
           [:td "3:03 PM"]
           [:td "$724.17"]]
          [:tr
           [:td "3323"]
           [:td "10/21/2013"]
           [:td "3:00 PM"]
           [:td "$23.71"]]
          [:tr
           [:td "3322"]
           [:td "10/21/2013"]
           [:td "2:49 PM"]
           [:td "$8345.23"]]
          [:tr
           [:td "3321"]
           [:td "10/21/2013"]
           [:td "2:23 PM"]
           [:td "$245.12"]]
          [:tr
           [:td "3320"]
           [:td "10/21/2013"]
           [:td "2:15 PM"]
           [:td "$5663.54"]]
          [:tr
           [:td "3319"]
           [:td "10/21/2013"]
           [:td "2:13 PM"]
           [:td "$943.45"]]]]]
       [:div.text-right
        [:a
         {:href "#"}
         "View All Transactions "
         [:i.fa.fa-arrow-circle-right]]]]]]])

(defn dashboard []
  ; "<!-- Page Heading -->"
  [:div
   [:h3 "Nothing to see."]])
   ;[content-summary]])
   ; panels
   ;[tasks-panel]])

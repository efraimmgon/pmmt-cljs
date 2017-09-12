(ns pmmt.pages.admin.users
  (:require
   [reagent.core :as r :refer [atom]]
   [re-frame.core :refer [dispatch dispatch-sync subscribe]]
   [pmmt.components.common :as c]
   [pmmt.components.registration :as reg]))

(defn setup! []
  (when-not @(subscribe [:admin.users/setup-ready?])
    (dispatch-sync [:get-users])))

; -------------------------------------------------------------------------
; Main
; -------------------------------------------------------------------------

(defn users-template [users]
  [:div.row>div.col-md-12
   [:div.card
    [:div.header
     [:h4.title "Registered Users"]]
    [:div.content.table-responsive.table-full-width
     [:table.table.table-striped.table-bordered
      [c/thead (keys (first @users))]
      [c/tbody @users]]]]])


(defn content []
  (setup!)
  (r/with-let [users (subscribe [:admin/users])]
    [:div.content>div.container-fluid

     [:div.row>div.col-md-12
      [:div.card>div.content
       [:button.btn.btn-wd
        {:on-click #(dispatch [:modal reg/registration-form])}
        "Add user"]]

      [users-template users]]]))

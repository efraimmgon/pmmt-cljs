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

(defn users-table []
  (let [users (subscribe [:admin/users])]
    (fn []
      [:div.table-responsive
       [:table.table.table-striped.table-bordered
        [c/thead (keys (first @users))]
        [c/tbody @users]]])))

(defn users-template []
  (setup!)
  (fn []
    [:div
     [c/nav-button
      {:handler #(dispatch [:modal reg/registration-form])
       :title "Adicionar usuário"}]
     [:hr]
     [users-table]]))

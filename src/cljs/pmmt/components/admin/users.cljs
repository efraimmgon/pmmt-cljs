(ns pmmt.components.admin.users
  (:require [clojure.string :as string]
            [ajax.core :as ajax]
            [reagent.core :as r :refer [atom]]
            [re-frame.core :as re-frame :refer
             [subscribe dispatch dispatch-sync]]
            [pmmt.components.common :as c]
            [pmmt.components.registration :as reg]))

; local state ------------------------------------------------------------

(defonce local-state
  (atom {:setup-ready? false}))

; misc ------------------------------------------------------------------


; setup ------------------------------------------------------------------

(defn setup! []
  (let [setup-ready? (r/cursor local-state [:setup-ready?])]
    (when-not @setup-ready?
      (dispatch-sync [:get-users]))))


; components ------------------------------------------------------------

(defn display-users [users]
  [:div.table-responsive
   [:table.table.table-striped.table-bordered
    [c/thead (keys (first users))]
    [c/tbody users]]])

(defn users-interface []
  (setup!)
  (let [users (subscribe [:users])]
    (fn []
      [:div
       [c/nav-button
        {:handler #(dispatch [:modal reg/registration-form])
         :title "Adicionar usuário"}]
       [:hr]
       (if @users
          [display-users @users]
          [:h3 "Não há usuários"])])))

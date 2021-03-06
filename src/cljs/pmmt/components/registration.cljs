(ns pmmt.components.registration
  (:require [reagent.core :as r :refer [atom]]
            [re-frame.core :refer
             [reg-event-fx reg-event-db subscribe reg-sub dispatch dispatch-sync]]
            [ajax.core :as ajax]
            [pmmt.components.common :as c]
            [pmmt.validation :refer [registration-errors]]))

; local state ----------------------------------------------------------------

(defonce local-state
  (atom {:registration-form {:fields {}
                             :error nil}}))

; Events ----------------------------------------------------------------

(defn register-failure
  [{:keys [db]} [_ error response]]
  {:reset [error {:server-error (get-in response [:response :message])}]
   :db db})

(reg-event-fx
 :register-failure
 register-failure)

(reg-event-fx
 :register-success
 (fn register-success [{:keys [db]} [_ fields response]]
   {:reset [fields {}]
    :dispatch [:remove-modal]
    :db (assoc db :identity (:id @fields))}))

(reg-event-fx
 :register
 (fn register [cofx [_ fields error]]
   (if-let [err (registration-errors @fields)]
     {:reset [error err]}
     {:http-xhrio {:method :get
                   :uri "/register"
                   :params @fields
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:register-success fields]
                   :on-failure [:register-failure error]}})))

(reg-event-fx
 :delete-account-success
 (fn [{:keys [db]} _]
   {:dispatch-n (list [:page :home] [:remove-modal])
    :db (dissoc db :identity)}))

(reg-event-fx
 :delete-account
 (fn [{:keys [db]} _]
   {:http-xhrio {:method :delete
                 :uri "/account"
                 :on-success [:delete-account-success]
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})}}))

; Subscriptions ---------------------------------------------------------

(reg-sub
 :identity
 (fn [db _]
   (:identity db)))

; Core -------------------------------------------------------------------

(defn registration-form []
  (let [fields (r/cursor local-state [:registration-form :fields])
        error (r/cursor local-state [:registration-form :error])]
    (fn []
      [c/modal
       [:div "Registro de novo usuário"]
       [:div
        [:div.well.well-sm
         [:strong "* campo obrigatório"]]
        [c/text-input "usuário" :id "insira um nome de usuário" fields]
        (when-let [error (first (:id @error))]
          [:div.alert.alert-danger error])
        [c/password-input "senha" :pass "insira uma senha" fields]
        (when-let [error (first (:pass @error))]
          [:div.alert.alert-danger error])
        [c/password-input "senha" :pass-confirm "reinsira a senha" fields]
        (when-let [error (:server-error @error)]
          [:div.alert.alert-danger error])]
       [:div
        [:button.btn.btn-primary
         {:on-click #(dispatch [:register fields error])}
         "Registrar"]
        [:button.btn.btn-danger
         {:on-click #(dispatch [:remove-modal])}
         "Cancelar"]]])))

(defn registration-button []
  [:a.btn
   {:on-click #(dispatch [:modal registration-form])}
   "registrar"])

(defn delete-account-modal []
  (fn []
    [c/modal
     [:h2.alert.alert-danger "Deletar Conta!"]
     [:p "Você tem certeza que deseja excluir sua conta?"]
     [:div
      [:button.btn.btn-primary
       {:on-click #(dispatch [:delete-account])}
       "Deletar"]
      [:button.btn.btn-danger
       {:on-click #(dispatch [:remove-modal])}
       "Cancelar"]]]))

(ns pmmt.components.login
  (:require [reagent.core :refer [atom]]
            [re-frame.core :refer
             [reg-event-db reg-event-fx subscribe dispatch reg-sub reg-fx]]
            [reagent.session :as session]
            [goog.crypt.base64 :as b64]
            [clojure.string :as string]
            [ajax.core :as ajax]
            [pmmt.components.common :as c]))

; helpers ---------------------------------------------------------------

(def timeout-ms (* 1000 60 30))

(defn session-timer []
  (let [identity (subscribe [:identity])
        user-event (subscribe [:user-event])]
    (when @identity
      (if @user-event
        (do
         (session/remove! :user-event)
         (js/setTimeout #(session-timer) timeout-ms))
        (session/remove! :identity)))))

(defn encode-auth [user pass]
  (->> (str user ":" pass)
       (b64/encodeString)
       (str "Basic ")))

; Subscriptions ---------------------------------------------------------

(reg-sub
 :user-event
 (fn [db _]
   (:user-event db)))

; Events ----------------------------------------------------------------

(reg-event-fx
 :login-error
 (fn [{:keys [db]} [_ error response]]
   ; display the error on the form
   {:reset [error (get-in response [:response :message])]
    :db db}))

; TODO: side-effects
(reg-event-fx
 :login-success
 (fn [{:keys [db]} [_ user-id response]]
   (js/setTimeout session-timer timeout-ms)
   {:dispatch [:remove-modal]
    :db (assoc db :identity user-id)}))

(reg-event-fx
 :login
 (fn [{:keys [db]} [_ fields error]]
   (let [{:keys [id pass]} @fields]
     {:reset [error nil]
      :http-xhrio {:method :post
                   :uri "/login"
                   :headers {"Authorization" (encode-auth (string/trim id) pass)}
                   :on-success [:login-success id]
                   :on-failure [:login-error error]
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})}
      :db db})))

; Core ------------------------------------------------------------------

(defn login-form []
  (let [fields (atom {})
        error (atom nil)]
    (fn []
      [c/modal
       [:div "Login"]
       [:div
        [:div.well.well-sm
         [:strong "* campo obrigatório"]]
        [c/text-input "nome" :id "digite o nome de usuário" fields]
        [c/password-input "senha" :pass "digite a senha" fields]
        (when-let [error @error]
          [:div.alert.alert-danger error])]
       [:div
        [:button.btn.btn-primary
         {:on-click #(dispatch [:login fields error])}
         "Login"]
        [:button.btn.btn-danger
         {:on-click #(dispatch [:remove-modal])}
         "Cancelar"]]])))

(defn login-button []
  [:a.btn
   {:on-click #(dispatch [:modal login-form])}
   "entrar"])

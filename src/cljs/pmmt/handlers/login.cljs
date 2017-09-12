(ns pmmt.handlers.login
  (:require
   [ajax.core :as ajax]
   [clojure.string :as string]
   [goog.crypt.base64 :as b64]
   [reagent.session :as session]
   [re-frame.core :refer
    [reg-event-db reg-event-fx subscribe dispatch reg-sub]]))

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
   {:reset [error (get-in response [:response :message])]}))

(reg-event-fx
 :login-success
 (fn [{:keys [db]} [_ user-id response]]
   (js/setTimeout session-timer timeout-ms)
   {:dispatch [:page :admin]
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
                   :response-format (ajax/json-response-format {:keywords? true})}})))

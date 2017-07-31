(ns pmmt.components.admin.upload
  (:require [goog.events :as gev]
            [reagent.core :as reagent :refer [atom]]
            [re-frame.core :as re-frame :refer
             [reg-event-db reg-event-fx reg-sub subscribe dispatch dispatch-sync]]
            [day8.re-frame.http-fx]
            [reagent.session :as session]
            [pmmt.components.common :as c])
  (:import goog.net.IframeIo
           goog.net.EventType
           [goog.events EventType]))

(defn upload-file! [upload-form-id status]
  (reset! status nil)
  (let [io (IframeIo.)]
    (gev/listen
     io goog.net.EventType.SUCCESS
     #(reset! status [:div.alert.alert-success "arquivo carregado com successo"]))
    (gev/listen
     io goog.net.EventType.ERROR
     #(reset! status [:div.alert.alert-danger "falha ao carregar arquivo"]))
    (.setErrorChecker io #(= "error" (.getResponseText io)))
    (.sendFromForm io
                   (.getElementById js/document upload-form-id)
                   "/upload")))

(reg-event-fx
 :upload-file
 (fn [{:keys [db]} [_ form-id status]]
   (upload-file! form-id status)
   {:db db}))

(defn update-db-form []
  (let [status (atom nil)
        form-id "upload-form"]
    (fn []
      [c/modal
       [:div "Carregar arquivo"]
       [:div
        (when @status @status)
        [:form {:id form-id
                :enc-type "multipart/form-data"
                :method "POST"}
         [:fieldset.form-group
          [:label {:for "file"} "selecione um arquivo para carregar"]
          [:input.form-control {:id "file" :name "file" :type "file"}]]]
        [:button.btn.btn-danger.pull-right
         {:on-click #(dispatch [:remove-modal])}
         "Cancelar"]
        [:button.btn.btn-primary
         {:on-click #(dispatch [:upload-file form-id status])}
         "Carregar"]]])))

(defn update-db-button []
  [:button.btn.btn-primary
   {:on-click #(dispatch [:modal update-db-form])}
   "Carregar arquivo"])

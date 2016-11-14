(ns pmmt.components.utilitarios
  (:require [reagent.core :refer [atom]]
            [re-frame.core :refer
             [reg-event-db reg-event-fx reg-sub subscribe dispatch dispatch-sync]]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [pmmt.components.common :as c]
            [pmmt.validation :as v]))

(def app-schema {:time-delta {:date :date-init
                              :days :int
                              :end :date}})
(declare time-delta-result-modal)

; Events ---------------------------------------------------------------

(reg-event-fx
 :time-delta-response
 (fn [{:keys [db]} [_ fields response]]
   {:dispatch [:modal time-delta-result-modal]
    :db (-> db
           (assoc-in [:time-delta :date] (:date @fields))
           (assoc-in [:time-delta :days] (:days @fields))
           (assoc-in [:time-delta :end] response))}))



(reg-event-fx
 :calculate-delta
 (fn [{:keys [db]} [_ fields errors]]
   (print "Fields:" @fields)
   (if-let [err (v/validate-util-date-calc @fields)]
     (do (reset! errors err)
         {:db db})
     {:http-xhrio {:method :get
                   :uri "/calculate-delta"
                   :params @fields
                   :on-success [:time-delta-response fields]
                   :response-format (ajax/json-response-format {:keywords? true})}
      :db db})))


; Subscriptions ---------------------------------------------------------

(reg-sub
 :time-delta-result
 (fn [db _]
   (:time-delta db)))

; Components ------------------------------------------------------------

(defn delta-calculator-form []
  (let [fields (atom {})
        errors (atom {})]
    (fn []
      [c/modal
       [:div "Cálculo de dias de fastamento"]
       [:div
        [c/text-input "Data de início" :date "dd/mm/aaaa" fields]
        [c/display-error errors :date]
        [c/number-input "Quantidade de dias" :days "dias" fields]
        [c/display-error errors :days]]
       [:div
        [:button.btn.btn-primary
         ;include fields errors
         {:on-click #(dispatch [:calculate-delta fields errors])}
         "Calcular"]
        [:button.btn.btn-danger
         {:on-click #(dispatch [:remove-modal])}
         "Cancelar"]]])))

(defn time-delta-result-modal []
  (let [result (subscribe [:time-delta-result])]
    (fn []
      [c/modal
       [:div "Cálculo de dias de fastamento || Resultado"]
       [:div
        [:table.table.table-bordered>tbody
         [:tr
          [:th "Data inicial"]
          [:th "Data final"]
          [:th "Dias"]]
         [:tr
          [:td (:date @result)]
          [:td (:end @result)]
          [:td (:days @result)]]]]
       [:div
        [:button.btn.btn-primary
         {:on-click #(dispatch [:modal delta-calculator-form])}
         "Novo cálculo"]
        [:button.btn.btn-danger
         {:on-click #(dispatch [:remove-modal])}
         "Sair"]]])))

(defn delta-calculator-button []
  [:button.btn.btn-default
   {:on-click #(dispatch [:modal delta-calculator-form])}
   "Calcular dias de afastamento"])

; Main page -------------------------------------------------------------

(defn utilitarios-page []
  [:div.container
   [:div.page-header
    [:h1 "Utilitários " [:small "aplicativos de usos práticos"]]]

   [:div.panel.panel-primary
    [:div.panel-heading
     [:h3 "Aplicativos"]]]
   [delta-calculator-button]])

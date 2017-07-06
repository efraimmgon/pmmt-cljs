(ns pmmt.pages.utils
  (:require [reagent.core :refer [atom]]
            [re-frame.core :refer [reg-sub subscribe dispatch]]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [pmmt.components.common :as c]
            [pmmt.validation :as v]))

(def app-schema {:time-delta {:date :date-init
                              :days :int
                              :end :date}})

; Subscriptions ---------------------------------------------------------

(reg-sub
 :time-delta-result
 (fn [db _]
   (:time-delta db)))

; Components ------------------------------------------------------------

(defn date-delta-form []
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

(defn date-delta-result-modal []
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
         {:on-click #(dispatch [:modal date-delta-form])}
         "Novo cálculo"]
        [:button.btn.btn-danger
         {:on-click #(dispatch [:remove-modal])}
         "Sair"]]])))

; Main page -------------------------------------------------------------

(defn main-page []
  [:div.container
   [:div.page-header
    [:h1 "Utilitários " [:small "aplicativos de usos práticos"]]]
   [:div.panel.panel-primary
    [:div.panel-heading
     [:h3 "Aplicativos"]]]
   [:ul.nav.nav-pills.nav-stacked
    [:li
     [:a
      {:style {:cursor :pointer}
       :on-click #(dispatch [:modal date-delta-form])}
      [:h4 "Calcular data"]]]]])

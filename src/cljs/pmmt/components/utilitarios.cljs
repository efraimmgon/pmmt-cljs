(ns pmmt.components.utilitarios
  (:require [reagent.core :refer [atom]]
            [ajax.core :as ajax]
            [pmmt.components.common :as c]))

(defonce app-state (atom nil))

(defn calculate-delta! [fields]
  (ajax/GET "/calculate-delta"
            {:params @fields
             :handler #(do
                        (reset! app-state
                               (assoc @fields :end %))
                        (.log js/console (str @app-state)))

             :error-handler #(.log js/console (str "error: " %))}))

(defn delta-calculator-form []
  (let [fields (atom {})]
    (fn []
      [:div.form-horizontal.well
       [:fieldset
        [:legend "Cálculo de dias de afastamento"]
        [:div.row
         [:div.col-md-6
          [c/text-input "Data de início" :date "dd/mm/aaaa" fields]]
         [:div.col-md-6
          [c/text-input "Quantidade de dias" :days "dias" fields]]

         [:button.btn-btn-primary
          {:on-click #(calculate-delta! fields)}
          "Calcular"]]]])))

(defn timedelta-result []
  (when-let [result @app-state]
    [:div
     [:h4 "Resultado"]
     [:table.table.table-bordered
      [:tr
       [:th "Data inicial"]
       [:th "Data final"]
       [:th "Dias"]]
      [:tr
       [:td (:date @app-state)]
       [:td (:end @app-state)]
       [:td (:days @app-state)]]]]))

(defn utilitarios-page []
  [:div.container
   [:div.page-header
    [:h1 "Utilitários " [:small "aplicativos de usos práticos"]]]

   [:div.panel.panel-primary
    [:div.panel-heading
     [:h3 "Aplicativos"]]]

   [delta-calculator-form]
   [timedelta-result]])

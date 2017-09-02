(ns pmmt.components.admin.geocode
  (:require-macros
   [pmmt.macros :refer [log]])
  (:require
   [ajax.core :as ajax]
   [reagent.core :as r :refer [atom]]
   [re-frame.core :refer [dispatch subscribe]]
   [pmmt.components.common :as c]))

; ----------------------------------------------------------------------
; Local State
; ----------------------------------------------------------------------

(defonce local-state
  (atom {:setup-ready? false}))

; ----------------------------------------------------------------------
; Setup
; ----------------------------------------------------------------------

(defn setup! []
  (let [setup-ready? (r/cursor local-state [:setup-ready?])
        goog-api-key (subscribe [:goog-api-key])]
    (when-not @setup-ready?
      (c/add-script!
       {:src (str "https://maps.googleapis.com/maps/api/js?"
                  "key=" @goog-api-key)})
      (reset! setup-ready? true))))

; tipo de logradouro + nome do logradouro + número do lote +
; bairro + município + sigla do Estado
; "Avenida Governador Júlio Campos, 1111 - Setor Comercial, Sinop - MT, Brasil"

; https://maps.googleapis.com/maps/api/geocode/output?parameters
; params -> address, key

; ----------------------------------------------------------------------
; Templates
; ----------------------------------------------------------------------

(defn sync-info []
  [:div
   [:p.info
    "Atualizar o Banco de Dados com os coordenardas geográficas (latitude e
    longitude) com base no endereço de cada ocorrência."]])

(defn sync-button []
  [:button.btn.btn-primary
   {:on-click #(dispatch [:geocode/sync-lat-lng])}
   "Sincronizar"])

(defn zero-results-template [zero-results]
  (when (seq @zero-results)
    [:div
     [:h3.text-center "Sem resultados"]
     (into
      [:ul]
      (for [item @zero-results]
        [:li (:address item)]))]))

(defn persist-lat-lng-button [result]
  [:button.btn.btn-block.btn-danger
   {:on-click #(dispatch [:update-crime-reports @result])}
   "Atualizar o Banco de Dados"])

(defn results-template [result]
  (when (seq @result)
    [:div
     [:h3.text-center "Resultado"]
     [persist-lat-lng-button result]
     [:div.table-responsive
      [:table.table.table-striped.table-bordered
       [:thead>tr
        [:th "id"]
        [:th "Endereço pesquisado"]
        [:th "Endereço encontrado"]
        [:th "Latitude"]
        [:th "Longitude"]]
       (into
        [:tbody]
        (for [row (sort-by :id @result)]
          [:tr
           [:td (:id row)]
           [:td (:address row)]
           [:td (:found row)]
           [:td (:latitude row)]
           [:td (:longitude row)]]))]]]))

; ----------------------------------------------------------------------
; Main
; ----------------------------------------------------------------------

(defn sync-lat-lng-panel []
  (setup!)
  (let [result (subscribe [:geocode/result])
        zero-results (subscribe [:geocode/zero-results])
        geocode-ready? (subscribe [:geocode/ready?])]
    (fn []
      [:div.panel.panel-primary
       [:div.panel-heading
        [:h3 "Sincronizar Banco de Dados"]]
       [:div.panel-body
        [sync-info]
        [sync-button]
        (when @geocode-ready?
          [:div
           ;; crime report rows that couldn't be geocoded
           [zero-results-template zero-results]
           ;; crime report rows that were geocoded, right or wrong
           [results-template result]])]])))

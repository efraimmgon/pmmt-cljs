(ns pmmt.components.admin.geocode
  (:require-macros
   [pmmt.macros :refer [log]])
  (:require
   [ajax.core :as ajax]
   [laconic.utils.core :refer [with-deps]]
   [reagent.core :as r :refer [atom]]
   [re-frame.core :refer [dispatch subscribe]]
   [pmmt.components.common :as c]))

; ----------------------------------------------------------------------
; Local State
; ----------------------------------------------------------------------

(defonce local-state
  (atom {:setup-ready? false}))

; ----------------------------------------------------------------------
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
(defn panel-template [title & body]
  [:div.card
   [:div.header
    [:h4.title title]]
   (into [:div.content]
         body)])

(defn sync-lat-lng-panel* []
  (let [result (subscribe [:geocode/result])
        zero-results (subscribe [:geocode/zero-results])
        geocode-ready? (subscribe [:geocode/ready?])]
    (fn []
      [panel-template
       "Sincronizar Banco de Dados"
       [sync-info]
       [sync-button]
       (when @geocode-ready?
         [:div
          ;; crime report rows that couldn't be geocoded
          [zero-results-template zero-results]
          ;; crime report rows that were geocoded, right or wrong
          [results-template result]])])))

(defn sync-lat-lng-panel []
  (r/with-let [google-api-key (subscribe [:settings/google-api-key])]
    [with-deps
     {:deps [{:id "google-maps-js"
              :type "text/javascript"
              :src (str "https://maps.googleapis.com/maps/api/js?"
                        "key=" @google-api-key
                        "&libraries=geometry,visualization")}]
      :loaded [sync-lat-lng-panel*]
      :loading [:div.loading "Loading..."]}]))
(ns pmmt.pages.geoprocessing
  (:require
   [clojure.string :as string]
   [reagent.core :as r :refer [atom]]
   [re-frame.core :as rf :refer
    [subscribe dispatch dispatch-sync]]
   [pmmt.components.common :as c]
   [pmmt.components.map :as map]))

; ---------------------------------------------------------------------
; Aux. HTML
; ---------------------------------------------------------------------

(defn help-text [display?]
  [:div.tab-content
    [:div#howto.tab-pane.fade
     {:id "howto"
      :class (if @display? "tab-pane fade active in"
                           "tab-pane fade")}
     [:p "Selecione uma data inicial e uma data final para gerar com os dados
          de ocorrências do período selecionado."]
     [:p "As seleções avançadas - opcionais - podem ser usadas para propiciar
          um maior controle sobre as ocorrências selecionadas"]
     [:p "O tipo de marcador pode ser selecionado em " [:b "Configurações."]]
     [:p "As opções são:"]
     [:p [:b "Marcador básico"] ": é a opção padrão, e marca o ponto geográfico exato
          da respectiva ocorrência no mapa." [:br]
         [:b "Mapa de calor"] ": Uma camada mapa de calor é uma visualização que demonstra
          a intensidade dos dados em pontos geográficos. Quando a camada de mapa de
          calor é ativada, uma sobreposição colorida é exibida sobre o mapa.
          Por padrão, as áreas de maior intensidade são coloridas em vermelho e
          as de menor intensidade em verde."]]])

(defn howto-panel []
  (let [display? (atom false)]
    (fn []
      [:div
       [:ul.nav.nav-pills
        [:li>a {:aria-expanded "true"
                :data-toggle "tab"
                :href "#/analise-criminal/geo/#howto"
                :on-click #(swap! display? not)}
         "Como usar?"]]
       [help-text display?]])))

; ------------------------------------------------------------------------
; Options for geo-form-modal form
; ------------------------------------------------------------------------

(defn crimes->select-options [display options]
  (if (> (count options) 1)
    {:key display :display display :value (mapv :id options)}
    {:key display :display display :value options}))

(defn group-crimes-by [names crimes]
  (map (fn [name]
         [name (filter #(string/includes? (:type %) name) crimes)])
       names))

(defn grouped-options [crimes]
  (apply conj []
         {:key "todas" :value "todas" :display "TODAS"}
         (map (fn [[name options]]
                (crimes->select-options name options))
              (group-crimes-by ["HOMICIDIO" "TRAFICO" "DROGAS"]
                               crimes))))

(defn crime-form-opts [crimes]
  (concat
    (grouped-options crimes)
    (map (fn [m]
           {:key (:id m)
            :value (:id m)
            :display (:type m)})
         crimes)))

; options for settings-modal ---------------------------------------------

(def marker-options
  [{:display "Marcador básico"
    :value :basic-marker}
   {:display "Mapa de calor"
    :value :heatmap}])

; ---------------------------------------------------------------------
; Forms
; ---------------------------------------------------------------------

(defn settings-modal []
  (let [field (atom {})]
    (fn []
      [c/modal
       [:div "Configurações"]
       [:div
        [c/select-form "Tipo de marcador" :marker-type marker-options field]]
       [:div
        [:button.btn.btn-primary
         {:on-click #(dispatch [:update-marker-type (:marker-type @field)])}
         "Escolher"]
        [:button.btn.btn-danger
         {:on-click #(dispatch [:remove-modal])}
         "Cancelar"]]])))

(def placeholder-values
  {:crime_id "todas"
   :data_inicial "01/01/2017"
   :data_final "31/01/2017"})

(defn geo-form-modal []
  (let [;; placeholder-values while in dev
        fields (atom placeholder-values)
        errors (atom {})
        crime-form-opts (crime-form-opts @(subscribe [:crimes]))]
    (fn []
      [c/modal
       [:div "Opções de Seleção"]
       [:div
        [:div.well.well-sm
         [:strong "* campo obrigatório"]]
        [:fieldset
         [:legend "Básicas"]
         [c/select-form "Natureza" :crime_id crime-form-opts fields]
         [c/display-error errors :data_inicial]
         [c/text-input "Data inicial" :data_inicial "dd/mm/aaaa" fields]
         [c/display-error errors :data_final]
         [c/text-input "Data final" :data_final "dd/mm/aaaa" fields]]
        [:fieldset
         [:legend "Avançadas"]
         [c/text-input "Bairro" :neighborhood "ex: Centro" fields true]
         [c/text-input "Via" :route "ex: Avenida Central" fields true]
         [c/display-error errors :hora_inicial]
         [c/text-input "Hora inicial" :hora_inicial "hh:mm" fields true]
         [c/display-error errors :hora_final]
         [c/text-input "Hora final" :hora_final "hh:mm" fields true]]]
       [:div
        [:button.btn.btn-primary
         {:on-click #(dispatch [:query-geo-dados fields errors])}

         "Buscar"]
        [:button.btn.btn-danger
         {:on-click #(dispatch [:remove-modal])}
         "Cancelar"]]])))

; ---------------------------------------------------------------------
; Buttons
; ---------------------------------------------------------------------

(defn settings-button []
   [:span
     [:button.btn.btn-default
      {:on-click #(dispatch [:modal settings-modal])}
      "Configurações"] " "])

(defn selection-options-button []
   [:span
     [:button.btn.btn-default
       {:on-click #(dispatch [:modal geo-form-modal])}
       "Opções de seleção"] " "])

; ---------------------------------------------------------------------
; Main Page
; ---------------------------------------------------------------------

(def scripts
  {#(exists? js/google) "https://maps.googleapis.com/maps/api/js?key=AIzaSyA7ekvGGHxVTwTVcpi073GOERnktqvmYz8&libraries=geometry,visualization"})
   ;#(exists? js/StyledMarker) "/js/styledMarkers.js"
   ;#(exists? js/oms) "/js/oms.js"})

(defn main-page []
  (let [show-table? (subscribe [:show-table?])]
    (dispatch-sync [:query-crimes])
    (fn []
      [c/js-loader
       {:scripts scripts
        :loading [:div.loading "Loading..."]
        :loaded [:div.container
                 [:div.page-header
                  [:h1 "Georreferenciamento "
                   [:small "de registros criminais"]]]
                 ;; gmap
                 [map/map-outer]
                 [:br]
                 [howto-panel]
                 [:br]
                 [:div
                  [settings-button]
                  [selection-options-button]]
                 (when @show-table?
                   [map/table])]}])))

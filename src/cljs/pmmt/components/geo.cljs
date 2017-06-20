(ns pmmt.components.geo
  (:require [clojure.string :refer [includes?]]
            [reagent.core :as r :refer [atom]]
            [re-frame.core :as re-frame :refer
             [reg-event-db reg-event-fx reg-sub subscribe dispatch dispatch-sync]]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [cljs-dynamic-resources.core :as cdr]
            [pmmt.components.common :as c]
            [pmmt.components.map :as m]))

; aux. html ---------------------------------------------------------------

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

; options for geo-form-modal form ----------------------------------------

(defn naturezas->select-opts [options display]
  (if (> (count options) 1)
    (reduce (fn [acc m]
              (update acc :value conj (:id m)))
            {:display display, :value []} options)
    {:display display, :value (:id (first options))}))

(defn get-incident [s]
  (filter #(clojure.string/includes? (:nome %) (.normalize s "NFKD"))
          @(subscribe [:naturezas])))

(defn extra-opts []
  (let [names ["Homicídio" "Tráfico" "Drogas"]
        rows (doall (map get-incident names))]
    (->>
     (map (fn [[opts display]]
            (naturezas->select-opts opts display))
          (map vector rows names))
     (concat [{:value "todas" :display "Todas"}]))))

(defn format-opts [options value-key display-key]
  (into []
    (map (fn [m]
           {:value (get m value-key)
            :display (get m display-key)})
         options)))

; options for settings-modal ---------------------------------------------

(def marker-options
  [{:display "Marcador básico"
    :value :basic-marker}
   {:display "Mapa de calor"
    :value :heatmap}])

; Forms ------------------------------------------------------------------

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

(defn geo-form-modal []
  (let [fields (atom {})
        errors (atom {})
        cities (subscribe [:cities])
        naturezas (subscribe [:naturezas])
        ; group related incidents in `extra-opts`, so all their ids can be
        ; selected at once
        natureza-form-opts (concat (extra-opts) (format-opts @naturezas :id :nome))]
    (fn []
      [c/modal
       [:div "Opções de seleção"]
       [:div
        [:div.well.well-sm
         [:strong "* campo obrigatório"]]
        [:fieldset
         [:legend "Básicas"]
         [c/select-form "Natureza" :natureza_id natureza-form-opts fields]
         [c/display-error errors :data_inicial]
         [c/text-input "Data inicial" :data_inicial "dd/mm/aaaa" fields]
         [c/display-error errors :data_final]
         [c/text-input "Data final" :data_final "dd/mm/aaaa" fields]]
        [:fieldset
         [:legend "Avançadas"]
         [c/text-input "Bairro" :bairro "ex: Centro" fields true]
         [c/text-input "Via" :via "ex: Avenida Central" fields true]
         [c/display-error errors :hora_inicial]
         [c/text-input "Hora inicial" :hora_inicial "hh:mm" fields true]
         [c/display-error errors :hora_final]
         [c/text-input "Hora final" :hora_final "hh:mm" fields true]]]
       [:div
        [:button.btn.btn-primary
         {:on-click #(do (dispatch [:reset-map-state])
                         (dispatch [:query-geo-dados fields errors]))}
         "Buscar"]
        [:button.btn.btn-danger
         {:on-click #(dispatch [:remove-modal])}
         "Cancelar"]]])))

; Buttons -----------------------------------------------------------------

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

; main page ---------------------------------------------------------------
(def scripts [{:src "https://maps.googleapis.com/maps/api/js?key=AIzaSyA7ekvGGHxVTwTVcpi073GOERnktqvmYz8&libraries=geometry,visualization"}
              {:src "/js/styledMarkers.js"}
              {:src "/js/oms.js"}])

(defn geo-page []
  (let [show-table? (subscribe [:show-table?])
        scripts-loaded? (subscribe [:geo/scripts-loaded?])]
    (dispatch-sync [:query-naturezas])
    (when-not @scripts-loaded?
      (cdr/add-scripts! scripts #(dispatch [:geo/scripts-loaded])))
    (fn []
      [:div.container
       [:div.page-header
        [:h1 "Georreferenciamento "
         [:small "de registros criminais"]]]
       ;; gmap
       (when @scripts-loaded?
        [m/map-outer])
       [:br]
       [howto-panel]
       [:br]
       [:div
        [settings-button]
        [selection-options-button]]
       (when @show-table?
         [m/table])])))

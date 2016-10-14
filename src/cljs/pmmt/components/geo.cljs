(ns pmmt.components.geo
  (:require [reagent.core :as r :refer [atom]]
            [reagent.session :as session]
            [ajax.core :as ajax]
            [pmmt.components.common :as c]
            [pmmt.components.map :as m]))

(defn fetch-cidades! []
  (when-not (session/get :cities)
    (ajax/GET "/cidades"
              {:handler #(do (session/put! :cities %))
               :error-handler #(.log js/console (str "Error: " %))})))

(defn fetch-naturezas! []
  (when-not (session/get :naturezas))
    (ajax/GET "/naturezas"
              {:handler #(session/put! :naturezas %)
               :error-handler #(.log js/console (str "Error: " %))}))

(defn city-options []
  (into []
    (map (fn [{:keys [id nome]}]
           {:value id
            :display nome})
         (session/get :cities))))

(defn natureza-options []
  (into []
    (map (fn [{:keys [id nome]}]
           {:value id
            :display nome})
         (session/get :naturezas))))

(defonce marker-options
  [{:display "Marcador básico"
    :value "basicMarker"}
   {:display "Mapa de calor"
    :value "heatmap"}])

;; TODO: code to change marker style
(defn settings-modal []
  (let [field (atom {})]
    (fn []
      [c/modal
       [:div "Configurações"]
       [:div
        [c/select-form "Tipo de marcador" :marker_style marker-options field]]
       [:div
        [:button.btn.btn-primary
         {:on-click #(session/remove! :modal)}
         "Escolher"]
        [:button.btn.btn-danger
         {:on-click #(session/remove! :modal)}
         "Cancelar"]]])))

;; TODO rewrite map.js in ClojureScript
(defn geo-form-modal []
  ;; TODO: errors atom for validation
  (let [fields (atom {})]
    (fn []
      [c/modal
       [:div "Opções de seleção"]
       [:div
        [:fieldset
         [:legend "Básicas"]
         [c/select-form "Cidade" :cidade (city-options) fields]
         [c/select-form "Natureza" :natureza (natureza-options) fields]
         [c/text-input "Data inicial" :data_inicial "dd/mm/aaaa" fields]
         [c/text-input "Data final" :data_final "dd/mm/aaaa" fields]]
        [:fieldset
         [:legend "Avançadas"]
         [c/text-input "Bairro" :bairro "ex: Centro" fields true]
         [c/text-input "Via" :via "ex: Avenida Central" fields true]
         [c/text-input "Hora inicial" :hora_inicial "hh/mm" fields true]
         [c/text-input "Hora final" :hora_final "hh/mm" fields true]]]
       [:div
        [:button.btn.btn-primary
         {:on-click #(m/handle-request! fields)}
         "Buscar"]
        [:button.btn.btn-danger
         {:on-click #(session/remove! :modal)}
         "Cancelar"]]])))

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

(defn init-state! []
  (fetch-cidades!)
  (fetch-naturezas!)
  (m/get-sample-data!))



;;; map.js

;;; The actual page that reagent will render

(defn geo-page []
  (init-state!)
  [:div.container
   [:link {:rel "stylesheet"
           :type "text/css"
           :href "/css/jquery.datetimepicker.min.css"}]
   [:div.page-header
    [:h1 "Georreferenciamento "
     [:small "de registros criminais"]]]
   ;; gmap
   [m/map-container]
   [:br]
   [howto-panel]
   [:br]
   [:div
     [:button.btn
       {:href "#settings"
        :on-click (fn [e]
                    (.preventDefault e)
                    (session/put! :modal settings-modal))}
       "Configurações"]]
   [:br]
   [:div
     [:button.btn
      {:href "#selection-options"
       :on-click #(do (.preventDefault %)
                      (session/put! :modal geo-form-modal))}
      "Opções de seleção"]
    ;; div
    [:p (str (first (session/get :sample-ocorrencias)))]]])

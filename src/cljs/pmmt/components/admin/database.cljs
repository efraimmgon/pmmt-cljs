(ns pmmt.components.admin.database
  (:require [reagent.core :as r :refer [atom]]
            [re-frame.core :as re-frame :refer
             [reg-event-db reg-event-fx reg-sub subscribe dispatch dispatch-sync]]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [pmmt.components.common :as c]
            [pmmt.components.admin.upload :as u]))

(def tables
  ["cidade" "natureza" "ocorrencia" "tag" "document"])

; Components ------------------------------------------------------------

; search

(defn search-modal [table]
  (let [fields (atom {})
        errors (atom {})]
    (fn []
      [c/modal
       [:div "Buscar em " table]
       [:div
        [c/display-error errors :server-error]
        [c/text-input "Campo" :field "O campo em que o valor será procurado" fields]
        [c/text-input "Valor" :value "A informação que será buscada" fields]]
       [:div
        [:button.btn.btn-primary
         {:on-click #(dispatch [:admin/search-table table fields errors])}
         "Buscar"]
        [:button.btn.btn-danger
         {:on-click #(dispatch [:remove-modal])}
         "Cancelar"]]])))

; nav

(defn nav-button [handler title]
  [:span
   [:button.btn.btn-default
    {:on-click handler}
    title]
   " "])

; panel

(defn panel-body [visible? & components]
  (into
   [:div.panel-body {:class (if @visible? nil "hidden")}]
   components))

; Table

(defn thead [rows]
  (let [keyz (remove #(= :id %) (keys (first rows)))]
      [:thead
       (into [:tr]
        (cons
         [:th "id"]
         (for [k keyz]
           ^{:key k}
           [:th k])))]))

(defn tbody [rows]
  (into [:tbody]
    (for [row rows]
      ^{:key row}
      (into [:tr]
        (cons
         [:td (:id row)]
         (for [k (keys (dissoc row :id))]
           ^{:key k}
           [:td (k row)]))))))

(defn display-rows [table visible?]
  (let [page (atom 0)
        rows (subscribe [:admin/read-rows table])]
    (fn []
      (when-not (empty? @rows)
        [:div.table-responsive
         {:class (if @visible? nil "hidden")}
         [c/pager (count @rows) page]
         [:table.table.table-striped.table-bordered
          [thead (@rows @page)]
          [tbody (@rows @page)]]]))))

(defn display-tables []
      (into [:div]
       (for [t tables]
         ^{:key t}
         (let [visible? (atom false)]
           [:div.panel.panel-default
            [:div.panel-heading
             {:style {:cursor :pointer}
              :id t
              :on-click #(swap! visible? not)}
             (clojure.string/capitalize t) " "
             [:span.caret]]
            [panel-body visible?
             [nav-button #(dispatch [:admin/fetch-table-rows t]) "Todos os registros"]
             [nav-button #(dispatch [:modal (partial search-modal t)]) "Buscar"]]
            [display-rows t visible?]]))))

(defn nav-pill [title panel active-panel]
  [:li
   {:class (when (= panel @active-panel) "active")
    :on-click #(reset! active-panel panel)}
   [:a.btn title]])

(defn update-db-info-text []
  (let [naturezas (subscribe [:naturezas])]
    (fn []
      [:div
       [:p.info
         "Selecione um arquivo em format csv com os dados das
          ocorrências para inseri-los no Banco de Dados."]
       [:p.info
        "Selecione também a cidade de referência das ocorrências a serem inseridas."]
       [:p "Observações:"]
       [:ul
        [:li "Os campos devem estar organizados na seguinte ordem:"
         [:ol
          [:li "Naturezas"]
          [:li "Bairro"]
          [:li "Via (rua, avenida, etc)"]
          [:li "Número"]
          [:li "Hora"]
          [:li "Data"]]]
        [:li "Se a ocorrência não possuir data ou hora, os respectivos campos devem
             estar em branco."]
        [:li "A hora deve estar no formato hh:mm."]
        [:li "A data deve estar no formato dd/mm/aaaa."]
        [:li "As seguintes naturezas estão disponíveis para inserção:"
          [:ul
           (for [n @naturezas]
             ^{:key (:id n)}
             [:li (:nome n)])]]]])))

; Main page

(defn database-panel []
  [:div.panel.panel-primary
   [:div.panel-heading
    [:h3 "Base de Dados"]]
   [:div.panel-body
    [display-tables]]])

(defn update-db-panel []
      [:div.panel.panel-primary
       [:div.panel-heading
        [:h3 "Inserir ocorrências no BD"]]
       [:div.panel-body
        [update-db-info-text]
        [u/update-db-button]]])

(defn inner-navigation [active-panel]
  [:ul.nav.nav-tabs
   [nav-pill "Base de Dados" :database active-panel]
   [nav-pill "Inserir dados" :update-db active-panel]])

(def panels
  {:database database-panel
   :update-db update-db-panel})

(defn database-panel-interface []
  (dispatch-sync [:query-naturezas]) ; for update-db-panel
  (let [active-panel (atom :database)]
    (fn []
      [:div.container
       [:div.page-header
        [:h1
         "Administração"]]
       [inner-navigation active-panel]
       [(panels @active-panel)]])))

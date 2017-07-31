(ns pmmt.pages.admin.database
  (:require
   [clojure.string :as string]
   [reagent.core :as r :refer [atom]]
   [re-frame.core :as rf :refer [dispatch dispatch-sync subscribe]]
   [pmmt.components.common :as c]
   ;; TODO: refactor namespaces
   [pmmt.components.admin.geocode :refer [sync-lat-lng-panel]]
   [pmmt.components.admin.upload :as upload]))

(defn setup! []
  (when-not @(subscribe [:admin.database/setup-ready?])
    (dispatch-sync [:query-naturezas])
    (dispatch [:admin.database/setup-ready])))

; -------------------------------------------------------------------------
; Components
; -------------------------------------------------------------------------

(defn search-modal [table]
  (let [fields (atom {})
        errors (atom {})]
    (fn []
      [c/modal
       [:div "Buscar em `" table "`"]
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

(defn thead [ks]
  [:thead
   [:tr
    (cons
     [:th {:key :id} "ID"]
     (for [k (remove #(= :id %) ks)]
       ^{:key k}
       [:th k]))]])

(defn tbody [rows]
  [:tbody
    (for [row rows]
      ^{:key row}
      [:tr
       (cons
        [:td {:key :id} (:id row)]
        (for [k (keys (dissoc row :id))]
          ^{:key k}
          [:td (str (k row))]))])])

(defn table-rows [table]
  (r/with-let [page (atom 0)
               rows (subscribe [:admin/table-rows table])]
    (when-not (empty? @rows)
      (js/console.log @page)
      (js/console.log (count @rows))
      ;(js/console.log (str (@rows @page)))
      [:div.table-responsive
       [c/pager (count @rows) page]
       [:table.table.table-striped.table-bordered
        [thead (-> (@rows @page) first keys)]
        [tbody (@rows @page)]]])))


(defn tables-component []
  (r/with-let [tables (subscribe [:admin.database/tables])]
    (into [:div]
     (for [table @tables]
       ^{:key table}
       (let [hidden? (atom false)]
         [:div.panel.panel-default
          ;; panel heading
          [:div.panel-heading {:style {:cursor :pointer}
                               :on-click #(swap! hidden? not)}
           (string/capitalize table) " " [:span.caret]]
          ;; panel body
          [:div.panel-body {:class (when @hidden? "hidden")}
           [c/nav-button {:title "Todos os registros"
                          :handler #(dispatch [:admin/fetch-table-rows table])}]
           [c/nav-button {:title "Buscar"
                          :handler #(dispatch [:modal (fn [] (search-modal table))])}]
           ;; table rows
           [table-rows table]]])))))

(defn update-db-info-text []
  (let [naturezas (subscribe [:naturezas])]
    (fn []
      [:div
       [:p.info
         "Selecione um arquivo em formato `csv` com os dados das
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

; -------------------------------------------------------------------------
; Panels
; -------------------------------------------------------------------------

(defn panel-template [title & body]
  [:div.panel.panel-primary
   [:div.panel-heading
    [:h3 title]]
   (into [:div.panel-body]
         body)])

(defn database-panel []
  [panel-template
   "Tabelas"
   [tables-component]])

(defn update-db-panel []
  [panel-template
   "Inserir ocorrências no BD"
   [update-db-info-text]
   [upload/update-db-button]])

; -------------------------------------------------------------------------
; Navigation
; -------------------------------------------------------------------------
(defn nav-pill [title panel-id active-panel]
  [:li
   {:class (when (= panel-id @active-panel) "active")
    :on-click #(dispatch [:admin.database/set-active-panel panel-id])}
   [:a.btn title]])

(defn inner-navigation []
  (r/with-let [active-panel (rf/subscribe [:admin.database/active-panel])]
    [:ul.nav.nav-tabs
     [nav-pill "Tabelas" :database active-panel]
     [nav-pill "Inserir dados" :update-db active-panel]
     [nav-pill "Sincronizar Banco de Dados" :synchronize active-panel]]))

(def panels
  {:database database-panel
   :update-db update-db-panel
   :synchronize sync-lat-lng-panel})

; -------------------------------------------------------------------------
; Main Component
; -------------------------------------------------------------------------

(defn main []
  (setup!)
  (r/with-let [active-panel (subscribe [:admin.database/active-panel])]
    [:div
     [inner-navigation]
     [(panels @active-panel)]]))

(ns pmmt.pages.admin.database
  (:require
   [clojure.string :as string]
   [reagent.core :as r :refer [atom]]
   [re-frame.core :refer [dispatch dispatch-sync subscribe]]))

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
     [:th "ID"]
     (for [k (remove #(= :id %) ks)]
       ^{:key k}
       [:th k]))]])

(defn tbody [rows]
  [:tbody
    (for [row rows]
      ^{:key row}
      [:tr
       (cons
        [:td (:id row)]
        (for [k (keys (dissoc row :id))]
          ^{:key k}
          [:td (k row)]))])])

(defn table-rows [table]
  (r/with-let [page (atom 0)
               rows (subscribe [:admin/table-rows table])]
    (when (not-empty? @rows)
      [:div.table-responsive
       [c/pager (count @rows) page]
       [:table.table.table-striped.table-bordered
        [thead (-> (@rows @page) first keys)]
        [tbody (@rows @page)]]])))


(defn tables-component []
  (r/with-let [tables (subscribe [:admin.database/tables])]
    [:div
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
           [table-rows table]]]))]))

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
   [u/update-db-button]])

; -------------------------------------------------------------------------
; Navigation
; -------------------------------------------------------------------------
(defn nav-pill [title panel-id]
  (r/with-let [active-panel (subscribe [:admin.database/active-panel])]
    [:li
     {:class (when (= panel-id @active-panel) "active")
      :on-click #(reset! active-panel panel-id)}
     [:a.btn title]]))

(defn inner-navigation []
  [:ul.nav.nav-tabs
   [nav-pill "Tabelas" :database]
   [nav-pill "Inserir dados" :update-db]
   [nav-pill "Sincronizar Banco de Dados" :synchronize]])

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

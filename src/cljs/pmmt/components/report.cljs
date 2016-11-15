(ns pmmt.components.report
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [clojure.string :as string]
            [clojure.pprint :as p]
            [reagent.core :as r :refer [atom]]
            [re-frame.core :as re-frame :refer
             [reg-event-fx reg-event-db reg-sub subscribe dispatch dispatch-sync]]
            [ajax.core :as ajax]
            [pmmt.components.common :as c]
            [pmmt.validation :as v]))


; Events -----------------------------------------------------------------

(reg-event-fx
 :process-report-data
 (fn [{:keys [db]} [_ result]]
   {:dispatch [:remove-modal]
    :db (assoc db :report result)}))

(reg-event-fx
 :query-report
 (fn [{:keys [db]} [_ fields errors]]
   (if-let [err (v/validate-report-args @fields)]
     {:reset [errors err]
      :db db}
     {:http-xhrio {:method :get
                   :params @fields
                   :uri "analise-criminal/relatorio/dados"
                   :on-success [:process-report-data]
                   :response-format (ajax/json-response-format {:keywords? true})}
      :db (assoc db :report-params @fields)})))

(defn plotly-did-update-pie [comp]
  (let [;; get new data
        [_ id plot-data] (r/argv comp)
        container (.getElementById js/document (str "id_" (:name plot-data) "_graph"))
        data [{:type "pie"
               :labels (:labels plot-data)
               :values (:vals plot-data)}]]
    ;; clear previous plot from the div
    (.purge js/Plotly container)
    (.plot js/Plotly container (clj->js data))))

;; TODO: side-effects
(reg-event-fx
 :update-pie-plot
 (fn [{:keys [db]} [_ comp]]
   (plotly-did-update-pie comp)
   {:db db}))

(defn plotly-did-update-bar [comp]
  (let [;; get new data
        [_ id plot-data] (r/argv comp)
        container (.getElementById js/document (str "id_" id "_graph"))
        data (for [row plot-data]
               {:type "bar"
                :x (:x row)
                :y (:y row)
                :name (:name row)})
        layout {:xaxis {:title (string/capitalize id)}
                :yaxis {:title "Registros"}}]
    ;; clear previous plot from the div
    (.purge js/Plotly container)
    (.newPlot js/Plotly container (clj->js data) (clj->js layout))))

;; TODO: side-effects
(reg-event-fx
 :update-bar-plot
 (fn [{:keys [db]} [_ comp]]
   (plotly-did-update-bar comp)
   {:db db}))

; Subscriptions ----------------------------------------------------------

(reg-sub
 :plot-data
 (fn [db _]
   (get-in db [:report :plots])))

; helpers ----------------------------------------------------------------

(defn incident-ids [s]
  (vec
    (map :id
      (filter #(string/includes? (:nome %) (.normalize s "NFKD"))
              @(subscribe [:naturezas])))))

(defn format-opts [options value-key display-key]
  (into []
    (map (fn [m]
           {:value (get m value-key)
            :display (get m display-key)})
         options)))

; Components -------------------------------------------------------------

; General Components

(defn thead [headers]
  [:thead
    (into
     [:tr]
     (for [th headers]
       ^{:key th}
        [:th th]))])

; rows => coll of rows; keyz => keys that access each row's field
; I should probably implement `keyz` as an optional field, which
; defaults to a range of the row's count
(defn tbody [rows keyz]
  (into
    [:tbody]
    (for [row rows]
      ^{:key row}
      (into
       [:tr]
       (for [k keyz]
         ^{:key k}
         [:td (get row k)])))))

(defn panel-with-table [title {:keys [headers rows keyz]}]
  [:div.panel.panel-default
   [:div.panel-heading [:h3 title]]
   [:table.table.table-bordered.table-striped
    [thead headers]
    [tbody rows keyz]]])

(defn panel-with-body [title body]
  [:div.panel.panel-default
   [:div.panel-heading [:h3 title]]
   [:div.panel-body
    body]])

; Plotly components

(defn plotly-render [id]
  [panel-with-body
   (str "Comparação de " id)
   [:div {:id (str "id_" id "_graph")
          :style {:width "100%" :height "400px"}}]])

;; Note: we don't access the `row` parameter directly; we get it
;; later on by calling `r/argv` on the component
(defn plotly-component-bar [id row]
  (r/create-class {:display-name "plotly-component-bar"
                   :reagent-render #(plotly-render id)
                   :component-did-mount #(dispatch [:update-bar-plot %])
                   :component-did-update #(dispatch [:update-bar-plot %])}))

(defn plotly-component-pie [id row]
  (r/create-class {:display-name "plotly-component-bar"
                   :reagent-render #(plotly-render id)
                   :component-did-mount #(dispatch [:update-pie-plot %])
                   :component-did-update #(dispatch [:update-pie-plot %])}))

(defn plot-comparison []
  (let [plot-data (subscribe [:plot-data])
        ; bar
        bar-vals (reaction (get-in @plot-data [:bar :vals]))
        ids-bar (reaction (get-in @plot-data [:bar :ids]))
        ; pie
        pie-vals (reaction (get-in @plot-data [:pie :vals]))
        ids-pie (reaction (get-in @plot-data [:pie :ids]))]
    (fn []
      [:div
        (into
         [:div]
         (map (fn [id row-a row-b]
                ^{:key (str "bar-" id)}
                [plotly-component-bar id [row-a row-b]])
              @ids-bar (:a @bar-vals) (:b @bar-vals)))
       (into
        [:div]
        (map (fn [id row]
               ^{:key (str "pie-" id)}
               [plotly-component-pie id row])
             @ids-pie [(:a @pie-vals) (:b @pie-vals)]))])))

; Main components

;; todo: report on page - not modal
(def default-values {:data-inicial-a "01/01/2015"
                     :data-final-a "31/05/2015"
                     :data-inicial-b "01/06/2015"
                     :data-final-b "31/12/2015"})

(defn report-form []
  (let [fields (atom default-values)
        errors (atom {})
        cities (subscribe [:cities])
        city-form-opts (format-opts @cities :id :nome)]
    (fn []
      [c/modal
       [:div {:on-click #(print @fields)}
        "Relatório de análise criminal"]
       [:div
        [:div.well.well-sm
         [:strong "* campo obrigatório"]]
        [c/select-form "Cidade" :cidade-id city-form-opts fields true]
        [:fieldset
         [:legend "Período A"]
         [c/text-input "Data inicial" :data-inicial-a "dd/mm/aaaa" fields]
         [c/display-error errors :data-inicial-a]
         [c/text-input "Data final" :data-final-a "dd/mm/aaaa" fields]
         [c/display-error errors :data-final-a]]
        [:fieldset
         [:legend "Período B"]
         [c/text-input "Data inicial" :data-inicial-b "dd/mm/aaaa" fields]
         [c/display-error errors :data-inicial-b]
         [c/text-input "Data final" :data-final-b "dd/mm/aaaa" fields]
         [c/display-error errors :data-final-b]]
        [:fieldset
         [:legend "Filtros opcionais"]
         [:div.form-horizontal
          [:div.form-group
           [:label {:class "col-md-2 control-label"}
            "Naturezas"]
           [:div.col-md-10
            [c/checkbox-input "Roubo" :roubo true fields true]
            [c/checkbox-input "Furto" :furto true fields true]
            [c/checkbox-input "Tráfico" :trafico true fields true]
            [c/checkbox-input "Homicídio" :homicidio true fields true]]]
          [:div.form-group
           [:label {:class "col-md-2 control-label"}
            "Bairro"]
           [:div.col-md-10
            ; TODO: text-select input
            [c/text-input nil :bairro "ex: Centro" fields true]]]
          [:div.form-group
           [:label {:class "col-md-2 control-label"}
            "Outros"]
           [:div.col-md-10
            [c/checkbox-input "Dias da semana" :dias-da-semana true fields true]
            [c/checkbox-input "Horários" :horarios true fields true]]]]]]
       [:div
        [:button.btn.btn-primary
         {:on-click #(dispatch [:query-report fields errors])}
         "Gerar relatório"]
        [:button.btn.btn-danger
         {:on-click #(dispatch [:remove-modal])}
         "Cancelar"]]])))

(defn result-header [dates]
  (into
   [:div.container>div.row]
   (map (fn [period [start end]]
          ^{:key period}
          [:div.col-md-6.bg-primary
          ;  [:div.panel.panel-primary
          ;   [:panel-heading
             [:h2
               period [:br]
               (str start " a " end)]])
        (cycle ["Período A" "Período B"])
        dates)))

(reg-sub
 :optionals-result
 (fn [db _]
   (get-in db [:report :optionals])))

(defn optionals-result-panels []
  (let [optionals (subscribe [:optionals-result])]
    (fn []
      (into [:div]
        (for [option @optionals]
          ^{:key option}
          (for [[opt-key values] (vec option)]
            (into
             ^{:key opt-key}
              [:div.row
               [:h2 (str "Análise: " (string/capitalize (name opt-key)))]]
              (for [[_ distinct-values] (vec values)]
                ^{:key distinct-values}
                (into
                  [:div.col-md-6]
                  (for [[field rows] distinct-values]
                    ^{:key field}
                    [panel-with-table
                     (string/capitalize (name field))
                     {:headers (list (string/capitalize (name field)) "Registros")
                      :rows rows
                      :keyz (range 2)}]))))))))))

(defn result-panel []
  (let [params (subscribe [:get-db :report-params])
        result (subscribe [:get-db :report])]
    (fn []
      (when @result
        [:div
         [:p.bg-info
          "Parâmetros: " (str @params)]
         [result-header (list [(:data-inicial-a @params)
                               (:data-final-a @params)]
                              [(:data-inicial-b @params)
                               (:data-final-b @params)])]
         [panel-with-table
          "Total de ocorrências registradas"
          {:headers (list "Período A" "Período B" "Variação")
           :rows (list (get-in @result [:total]))
           :keyz (keys (get-in @result [:total]))}]
         [panel-with-table
          "Variação de registros por natureza"
          {:headers (list "Natureza" "Período A" "Período B" "Variação")
           :rows (get-in @result [:natureza-comparison])
           :keyz (keys (first (get-in @result [:natureza-comparison])))}]
         [plot-comparison]
         (when (:optionals @result)
           [optionals-result-panels])]))))

(defn report-button []
   [:span
     [:button.btn.btn-default
      {:on-click #(dispatch [:modal report-form])}
      "Gerar novo relatório"] " "])

(defn report-page []
  (dispatch-sync [:query-cities])
  (dispatch-sync [:query-naturezas])
  (fn []
    [:div.container
     [:div.page-header
      [:h1 "Relatório "
       [:small "de registros criminais"]]]
     [:div
      [report-button] [:hr]]
     [result-panel]]))

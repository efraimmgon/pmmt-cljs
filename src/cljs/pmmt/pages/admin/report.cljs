(ns pmmt.pages.admin.report
  (:require-macros
   [reagent.ratom :refer [reaction]])
  (:require
   [clojure.string :as string]
   [reagent.core :as r :refer [atom]]
   [reagent-forms.core :refer [bind-fields]]
   [re-frame.core :as re-frame :refer
    [subscribe dispatch dispatch-sync]]
   [pmmt.components.common :as c]))

; ------------------------------------------------------------------------
; Components
; ------------------------------------------------------------------------

; General Components -----------------------------------------------------

(defn thead [headers]
  [:thead
    (into
     [:tr]
     (for [th headers]
       ^{:key th}
        [:th.text-center th]))])

(defn preppend-text-rules [k row]
  (when (= k :fluctuation)
    (if (< (:a row) (:b row))
      "+ "
      "- ")))

(defn append-text-rules [k]
  (when (= k :fluctuation) "%"))

(defn class-rules [row]
  (when (:fluctuation row)
    (if (< (:a row) (:b row))
      "bg-danger"
      "bg-success")))
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
         (let [preppended-text (preppend-text-rules k row)
               appended-text (append-text-rules k)
               class (class-rules row)]
           [:td.text-center {:class class}
            (str preppended-text (get row k) appended-text)]))))))

(defn panel-with-table [title {:keys [headers rows keyz]}]
  [:div.panel.panel-default
   [:div.panel-heading [:h3.text-center title]]
   [:table.table.table-bordered.table-striped
    [thead headers]
    [tbody rows keyz]]])

(defn panel-with-body [title body]
  [:div.panel.panel-default
   [:div.panel-heading [:h3.text-center title]]
   [:div.panel-body
    body]])

; Plotly components -------------------------------------------------------

(defn plotly-render [id]
  [panel-with-body
   (str "Comparação de " id)
   [:div {:id (str "id_" id "_graph")
          :style {:width "100%" :height "400px"}}]])

;; Note: we don't access the `values` parameter directly; we get it
;; later on by calling `r/argv` on the component
(defn plotly-component-bar [id values]
  (r/create-class {:display-name "plotly-component-bar"
                   :reagent-render #(plotly-render id)
                   :component-did-mount #(dispatch [:update-bar-plot %])
                   :component-did-update #(dispatch [:update-bar-plot %])}))

(defn plotly-component-pie [id values]
  (r/create-class {:display-name "plotly-component-bar"
                   :reagent-render #(plotly-render id)
                   :component-did-mount #(dispatch [:update-pie-plot %])
                   :component-did-update #(dispatch [:update-pie-plot %])}))

(defn plot-comparison []
  (let [plot-data (subscribe [:plot-data])
        ;;; ids and values for plots
        ; bar
        bar-vals (reaction (get-in @plot-data [:bar :vals]))
        ids-bar (reaction (get-in @plot-data [:bar :ids]))
        ; pie
        pie-vals (reaction (get-in @plot-data [:pie :vals]))
        ids-pie (reaction (get-in @plot-data [:pie :ids]))]
    (fn []
      [:div
        (into [:div]
         (map (fn [id a-values b-values]
                ^{:key (str "bar-" id)}
                [plotly-component-bar id [a-values b-values]])
              @ids-bar
              (:a @bar-vals)
              (:b @bar-vals)))
       (into [:div]
        (map (fn [id values]
               ^{:key (str "pie-" id)}
               [plotly-component-pie id values])
             @ids-pie
             [(:a @pie-vals) (:b @pie-vals)]))])))

; Main components -------------------------------------------------------

;; todo: report on page - not modal
(def default-values {:data-inicial-a "01/01/2017"
                     :data-final-a "31/05/2017"
                     :data-inicial-b "01/06/2017"
                     :data-final-b "31/12/2017"})

(defn periodo-template [legend id-start id-end]
  [:fieldset
   [:legend legend]
   [:div
    ; TODO: [c/display-error errors id-start]
    [:div.form-group
     [:label "Data inicial"]
     [:div.input-group
      [:span.input-group-addon "*"]
      [:div {:field :datepicker, :id id-start, :date-format "dd/mm/yyyy",
             :auto-close? true, :lang :pt-PT}]]]
    ; TODO: [c/display-error errors id-end]
    [:div.form-group
     [:label "Data final"]
     [:div.input-group
      [:span.input-group-addon "*"]
      [:div {:field :datepicker, :id id-end, :date-format "dd/mm/yyyy", :auto-close? true, :lang :pt-PT}]]]]])


(def crimes-filter-template
  [:div.form-group
   [:label {:class "col-md-2 control-label"}
    "Naturezas"]
   [:div.col-md-10

     [:label.checkbox
      [:input {:field :checkbox, :id :roubo, :data-toggle "checkbox"}]
      "Roubo"]
    [:div
     [:label.checkbox
      [:input {:field :checkbox, :id :furto}]
      "Furto"]]
    [:div
     [:label.checkbox
      [:input {:field :checkbox, :id :trafico}]
      "Tráfico"]]
    [:div
     [:label.checkbox
      [:input {:field :checkbox, :id :homicidio}]
      "Homicídio"]]]])

(def neighborhood-filter-template
  [:div.form-group
   [:label {:class "col-md-2 control-label"}
    "Bairro"]
   [:div.col-md-10
    ; TODO: change to typehead
    [:div.form-group
     [:input.form-control
      {:field :text, :id :neighborhood, :placeholder "ex: Setor Comercial"}]]]])

(def misc-filter-template
  [:div.form-group
   [:label {:class "col-md-2 control-label"}
    "Outros"]
   [:div.col-md-10
    [:div.checkbox
     [:label
      [:input {:field :checkbox, :id :weekday}]
      "Dias da semana"]]
    [:div.checkbox
     [:label
      [:input {:field :checkbox, :id :times}]
      "Horários"]]]])

(def report-form-template
  [:div
   ;;; REQUIRED FIELDS
   (periodo-template "Período A" :data-inicial-a :data-final-a)
   (periodo-template "Período B" :data-inicial-b :data-final-b)
   ;;; OPTIONAL FIELDS
   [:fieldset
    [:legend "Filtros opcionais"]
    [:div.form-horizontal
     crimes-filter-template
     neighborhood-filter-template
     misc-filter-template]]])

(defn report-form []
  (let [doc (atom {:data-inicial-a "01/01/2017"
                   :data-final-a "31/05/2017"
                   :data-inicial-b "01/06/2017"
                   :data-final-b "31/12/2017"
                   :neighborhood "What?"})
        errors (atom {})]
    (fn []
      [c/modal
       [:div
        "Relatório de análise criminal"]
       [:div
        [:div.well.well-sm
         [:strong "* campo obrigatório"]]

        [bind-fields report-form-template doc]]

       [:div
        [:button.btn.btn-primary
         {:on-click #(dispatch [:query-report doc errors])}
         "Gerar relatório"]
        [:button.btn.btn-danger
         {:on-click #(dispatch [:remove-modal])}
         "Cancelar"]]])))

(defn result-header [params]
  (into
   [:div.row]
   (map (fn [period [start end]]
          ^{:key period}
          [:div.col-md-6
           [:div.panel.panel-primary
            [:panel-heading
             [:h2.text-center
               period [:br]
               (str start " a " end)]]]])
        ["Período A:", "Período B:"]
        [[(:data-inicial-a @params), (:data-final-a @params)]
         [(:data-inicial-b @params), (:data-final-b @params)]])))


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
               [:h2.text-center (str "Análise: " (string/capitalize (name opt-key)))]]
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

(defn params-sanity-check [params]
  [:p.alert.alert-info
   "Parâmetros: " (str @params)])

(defn result-panel []
  (let [;; sanity check
        params (subscribe [:get-db :report-params])
        ;; result from data crunching, after submission
        result (subscribe [:get-db :report])]
    (fn []
      (when @result
        [:div
         ;; sanity check
         ;[params-sanity-check params]
         [result-header params]
         [panel-with-table
          "Total de ocorrências registradas"
          {:headers ["Período A", "Período B", "Variação"]
           :rows (list (get-in @result [:total]))
           :keyz (keys (get-in @result [:total]))}]
         [panel-with-table
          "Variação de registros por natureza"
          {:headers (list "Natureza" "Período A" "Período B" "Variação")
           :rows (get-in @result [:crime-comparison])
           :keyz (keys (first (get-in @result [:crime-comparison])))}]
         [plot-comparison]
         (when (:optionals @result)
           [optionals-result-panels])]))))

(defn report-button []
   [:span
     [:button.btn.btn-block.btn-primary
      {:on-click #(dispatch [:modal report-form])}
      "Gerar novo relatório"] " "])

; ---------------------------------------------------------------------
; Main Page
; ---------------------------------------------------------------------

(defn content- []
  [:div.content
   [:div.container-fluid
    [:div.row
     [:div.col-md-12
      [:div.card
       [:div.header
        [:h4.title "Criminal Report"]]
       [:div.content
        [report-button]
        [:hr]
        [result-panel]]]]]]])

(defn content []
  (dispatch-sync [:query-crimes])
  (fn []
    [c/js-loader
     ;; TODO: replace plotly by chartist
     {:scripts {#(exists? js/Plotly) "https://cdn.plot.ly/plotly-latest.min.js"}
      :loading [:div.loading "Loading..."]
      :loaded [content-]}]))

(ns pmmt.pages.admin.report
  (:require-macros
   [reagent.ratom :refer [reaction]])
  (:require
   [clojure.string :as string]
   [reagent.core :as r :refer [atom]]
   [reagent-forms.core :refer [bind-fields]]
   [re-frame.core :as re-frame :refer
    [subscribe dispatch dispatch-sync]]
   [pmmt.components.common :as c :refer
    [card thead tbody chart]]
   [pmmt.utils :as utils :refer [date->readable]]))

; ------------------------------------------------------------------------
; Components
; ------------------------------------------------------------------------

; General Components -----------------------------------------------------

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
(defn tbody- [rows keyz]
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
    [tbody- rows keyz]]])

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
   (periodo-template "Período A" :range1.from :range1.to)
   (periodo-template "Período B" :range2.from :range2.to)
   ;;; OPTIONAL FIELDS
   [:fieldset
    [:legend "Filtros opcionais"]
    [:div.form-horizontal
     crimes-filter-template
     neighborhood-filter-template
     misc-filter-template]]])

;; todo: report on page - not modal
(def default-values
  {:range1 {:from {:year 2017, :month 1, :day 1}
            :to   {:year 2017, :month 1, :day 31}}
   :range2 {:from {:year 2017, :month 2, :day 1}
            :to   {:year 2017, :month 2, :day 28}}})

(defn report-form []
  (r/with-let [doc (atom default-values)
               errors (atom {})]
    [c/modal
     [:div
      "Relatório de análise criminal"]

     [:div
      [:div.well.well-sm
       [:strong "* campo obrigatório"]]
      [:p.text-info
       "Selecione um período de datas para gerar a respectiva análise, "
       "ou selecione dois períodos de datas para comparar a estatísticas "
       "das dos períodos, além das respectivas análises."]
      [bind-fields report-form-template doc]]

     [:div
      [:button.btn.btn-primary
       {:on-click #(dispatch [:query-report doc errors])}
       "Gerar relatório"]
      [:button.btn.btn-danger
       {:on-click #(dispatch [:remove-modal])}
       "Cancelar"]]]))

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
        ["Período 1:", "Período 2:"]
        [[(get-in @params [:range1 :from]), (get-in @params [:range1 :to])]
         [(get-in @params [:range2 :from]), (get-in @params [:range2 :to])]])))


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

(defn result-panel- []
  (let [;; sanity check
        params (subscribe [:get-db :report-params])
        ;; result from data crunching, after submission
        result (subscribe [:get-db :report])]
    (fn []
      (when @result
        [:div
         ;; sanity check
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

(defn pretty-display [title data]
  [:div
   [:h3 title]
   [:pre
    (with-out-str
     (cljs.pprint/pprint @data))]])

(defn table-statistics-template [{:keys [title subtitle headers rows]}]
  [:div.row
   [:div.col-md-12
    [card {:title title
           :subtitle subtitle
           :content [:table.table.table-bordered.table-striped
                     [thead headers]
                     [tbody rows]]}]]])

(defn one-value-template [params]
  [:div.row
   [:div.col-md-12
    [card params]]])

; For the sake of simplicity I'll start by displaying only the values,
; leaving the charts for latter
; The basic template is the card.
; All categories will have a title, which states what the data represents,
; followed by the data. If the data is an int, we only display it, if
; it's a vector, we will use a table for this displayal

(defn single-template []
  (r/with-let [statistics (subscribe [:report.single/statistics])
               by-crime-type (subscribe [:report.single/by-crime-type])
               by-neighborhood (subscribe [:report.single/by-neighborhood])
               by-weekday (subscribe [:report.single/by-weekday])
               by-route (subscribe [:report.single/by-route])
               by-period (subscribe [:report.single/by-period])
               by-hour (subscribe [:report.single/by-hour])
               by-date (subscribe [:report.single/by-date])]
    [:div
     ; count
     [one-value-template
      {:title "Total de registros", :content (:count @statistics)}]
     ; by crime type
     [card
      {:title "Registros"
       :subtitle "Por natureza"
       :content
       [:div.row
        [:div.col-md-6
         [chart
          {:display-name "chart-report-single-by-crime-type"
           :chart-type "pie"
           :data [(range 1 (inc (count @by-crime-type)))
                  (map second @by-crime-type)]}]]
        [:div.col-md-6
         [:table.table.table-bordered.table-striped
          [thead ["Natureza", "Registros"]]
          [tbody @by-crime-type]]]]}]
     ; by neighborhood
     [card
      {:title "Registros"
       :subtitle "Por bairro"
       :content
       [:div.row
        [:div.col-md-12
         [chart
          {:display-name "chart-report-single-by-neighborhood"
           :chart-type "bar"
           :data [(map first @by-neighborhood)
                  [(map second @by-neighborhood)]]}]]
        [:div.col-md-12
         [:table.table.table-bordered.table-striped
          [thead ["Bairro", "Registros"]]
          [tbody @by-neighborhood]]]]}]
     ; by weekday
     [card
      {:title "Registros"
       :subtitle "Por dia da semana"
       :content
       [:div.row
        [:div.col-md-6
         [chart
          {:display-name "chart-report-single-by-weekday"
           :chart-type "bar"
           :data [(map first @by-weekday)
                  [(map second @by-weekday)]]}]]
        [:div.col-md-6
         [:table.table.table-bordered.table-striped
          [thead ["Dia da semana", "Registros"]]
          [tbody @by-weekday]]]]}]
     ; by route
     [card
      {:title "Registros"
       :subtitle "Por via"
       :content
       [:div.row
        [:div.col-md-12
         [chart
          {:display-name "chart-report-single-by-route"
           :chart-type "bar"
           :data [(map first @by-route)
                  [(map second @by-route)]]}]]
        [:div.col-md-12
         [:table.table.table-bordered.table-striped
          [thead ["Por via", "Registros"]]
          [tbody @by-route]]]]}]
     ; by period
     [card
      {:title "Registros"
       :subtitle "Por período"
       :content
       [:div.row
        [:div.col-md-6
         [chart
          {:display-name "chart-report-single-by-period"
           :chart-type "line"
           :data [(map first @by-period)
                  [(map second @by-period)]]}]]
        [:div.col-md-6
         [:table.table.table-bordered.table-striped
          [thead ["Período", "Registros"]]
          [tbody @by-period]]]]}]
     ; by hour
     [card
      {:title "Registros"
       :subtitle "Por hora"
       :content
       [:div.row
        [:div.col-md-12
         [chart
          {:display-name "chart-report-single-by-hour"
           :chart-type "line"
           :data [(map first @by-hour)
                  [(map second @by-hour)]]}]]]}]
     ; by date
     [card
      {:title "Registros"
       :subtitle "Por data"
       :content
       [:div.row
        [:div.col-md-12
         [chart
          {:display-name "chart-report-single-by-date"
           :chart-type "line"
           :data [(range 1 (inc (count @by-date)))
                  [(map second @by-date)]]}]]]
       :footer
       [:div.legend
        [:p "* Datas substituídas pelo número ordinal"]]}]]))

(defn heading-template [search-response]
  (let [col-length (/ 12 (count (:ranges @search-response)))]
    (into
     [:div.row]
     (for [response (:ranges @search-response)]
       [:div {:class (str "col-md-" col-length)}
        [:h2.title.text-center
         (date->readable (:from response)) " - "
         (date->readable (:to response))]]))))


(defn statistics-template []
  (r/with-let [params (subscribe [:report/params])
               ;; result from data crunching, after submission
               statistics (subscribe [:report/statistics])]
    (when @statistics
      [:div
       [heading-template statistics]
       (if (= 1 (count @params))
         [single-template])])))


(defn report-button []
   [:span
     [:button.btn.btn-block.btn-primary
      {:on-click #(dispatch [:modal report-form])}
      "Gerar novo relatório"] " "])

; ---------------------------------------------------------------------
; Main Page
; ---------------------------------------------------------------------

(defn inner-content []
  [:div.content
   [:div.container-fluid
    [:div.row
     [:div.col-md-12
      [card {:title "Relatório de Análise Criminal"
             :content [:div
                       [report-button]
                       [:hr]
                       [statistics-template]]}]]]]])

(defn content []
  (dispatch-sync [:query-crimes])
  (fn []
    [c/js-loader
     ;; TODO: replace plotly by chartist
     {:scripts {#(exists? js/Plotly) "https://cdn.plot.ly/plotly-latest.min.js"}
      :loading [:div.loading "Loading..."]
      :loaded [inner-content]}]))

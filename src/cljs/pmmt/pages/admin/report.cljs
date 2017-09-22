(ns pmmt.pages.admin.report
  (:require
   [reagent.core :as r :refer [atom]]
   [reagent-forms.core :refer [bind-fields]]
   [re-frame.core :as re-frame :refer
    [subscribe dispatch dispatch-sync]]
   [pmmt.components.common :as c :refer
    [card thead tbody chart thead-indexed tbody-indexed]]
   [pmmt.utils :as utils :refer [date->readable]]))

; ------------------------------------------------------------------------
; Form
; ------------------------------------------------------------------------

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

; ------------------------------------------------------------------------
; Result
; ------------------------------------------------------------------------

(defn composite-range []
  "Template for composite-range queries responses."
  (r/with-let [statistics (subscribe [:report/statistics])
               count-stats (subscribe [:report.compare/count])
               by-crime-type (subscribe [:report.compare/by-crime-type])
               by-neighborhood (subscribe [:report.composite/by-neighborhood])
               by-weekday (subscribe [:report.compare/by-weekday])
               by-route (subscribe [:report.composite/by-route])
               by-period (subscribe [:report.compare/by-period])
               by-hour (subscribe [:report.composite/by-hour])
               by-date (subscribe [:report.composite/by-date])]
    [:div
     ; count (comparison)
     [card
      {:title "Total de registros"
       :content
       [:div.row
        [:div.col-md-12
         [:table.table.table-bordered.table-striped
          [thead ["Registros 1", "Registros 2", "Variação (%)"]]
          [tbody @count-stats]]]]}]
     ; by crime type (comparison)
     [card
      {:title "Por natureza"
       :subtitle "Registros"
       :content
       [:div
        [:div.row
         [:div.col-md-12
          [:table.table.table-bordered.table-striped
           [thead-indexed ["Natureza", "Registros 1", "Registros 2", "Variação (%)"]]
           [tbody-indexed @by-crime-type]]]]
        [:div.row
         [:div.col-md-6
          [chart
           {:display-name "chart-report-composite-by-crime-type-1"
            :chart-type "pie"
            :data [(range 1 (inc (count @by-crime-type)))
                   (map second @by-crime-type)]}]]
         [:div.col-md-6
          [chart
           {:display-name "chart-report-composite-by-crime-type-2"
            :chart-type "pie"
            :data [(range 1 (inc (count @by-crime-type)))
                   (map #(get % 2) @by-crime-type)]}]]]]}]
     ; by neighborhood (chart only)
     [card
      {:title "Por bairro"
       :subtitle "Registros"
       :content
       (into
         [:div.row]
         (for [rows @by-neighborhood]
           [:div.col-md-12
            [chart
             {:chart-type "bar"
              :data [(map first rows)
                     [(map second rows)]]}]]))}]
     ; by weekday (comparison)
     [card
      {:title "Por dia da semana"
       :subtitle "Registros"
       :content
       [:div
        [:div.row
         [:div.col-md-6
          [:table.table.table-bordered.table-striped
           [thead ["Dia da semana", "Registros 1" "Registros 2" "Variação (%)"]]
           [tbody @by-weekday]]]
         [:div.col-md-6
          [chart
           {:display-name "chart-report-composite-by-weekday"
            :chart-type "line"
            :data [(map first @by-weekday)
                   [(map second @by-weekday)
                    (map #(get % 2) @by-weekday)]]}]]]]}]
     ; by route
     [card
      {:title "Por via"
       :subtitle "Registros"
       :content
       (into
         [:div.row]
         (for [rows @by-route]
           [:div.col-md-12
            [chart
             {:chart-type "bar"
              :data [(map first rows)
                     [(map second rows)]]}]]))}]
     ; by period (comparison)
     [card
      {:title "Por período"
       :subtitle "Registros"
       :content
       [:div.row
        [:div.col-md-6
         [:table.table.table-bordered.table-striped
          [thead ["Período", "Registros 1" "Registros 2" "Variação (%)"]]
          [tbody @by-period]]]
        [:div.col-md-6
         [chart
          {:display-name "chart-report-composite-by-period"
           :chart-type "line"
           :data [(map first @by-period)
                  [(map second @by-period)
                   (map #(get % 2) @by-period)]]}]]]}]
     ; by hour
     [card
      {:title "Por hora"
       :subtitle "Registros"
       :content
       [:div.row
        [:div.col-md-12
         [chart
          {:display-name "chart-report-composite-by-hour"
           :chart-type "line"
           :data [(map first (first @by-hour))
                  (map #(map second %) @by-hour)]}]]]}]
     ; by date
     [card
      {:title "Por data"
       :subtitle "Registros"
       :content
       [:div.row
        [:div.col-md-12
         [chart
          {:display-name "chart-report-single-by-date"
           :chart-type "line"
           :data [(range 1 (inc (count (first @by-date))))
                  (map #(map second %) @by-date)]}]]]
       :footer
       [:div.legend
        [:p "* Datas substituídas pelo número ordinal"]]}]]))




(defn single-range []
  "Template for single-range queries responses."
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
     [card
      {:title "Total de registros", :content (:count @statistics)}]
     ; by crime type
     [card
      {:title "Por natureza"
       :subtitle "Registros"
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
      {:title "Por bairro"
       :subtitle "Registros"
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
      {:title "Por dia da semana"
       :subtitle "Registros"
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
      {:title "Por via"
       :subtitle "Registros"
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
      {:title "Por período"
       :subtitle "Registros"
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
      {:title "Por hora"
       :subtitle "Registros"
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
      {:title "Por data"
       :subtitle "Registros"
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
               statistics (subscribe [:report/statistics])]
    (when @statistics
      [:div
       [heading-template statistics]
       (if (= 1 (count @params))
         [single-range]
         [composite-range])])))


(defn report-button []
   [:span
     [:button.btn.btn-block.btn-primary
      {:on-click #(dispatch [:modal report-form])}
      "Gerar novo relatório"] " "])

; ---------------------------------------------------------------------
; Main Page
; ---------------------------------------------------------------------

(defn content []
  (dispatch-sync [:query-crimes])
  (fn []
    [:div.content

     [card {:title "Relatório de Análise Criminal"
            :content
            [:div
             [report-button]
             [:hr]
             [statistics-template]]}]]))

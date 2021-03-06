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
  {:range1 {:from {:year 2017, :month 7, :day 1}
            :to   {:year 2017, :month 7, :day 31}}
   :range2 {:from {:year 2017, :month 8, :day 1}
            :to   {:year 2017, :month 8, :day 31}}})

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
               by-crime-group (subscribe [:report.compare/by :crime-group])
               by-neighborhood (subscribe [:report.composite/by :neighborhood])
               by-weekday (subscribe [:report.compare/by-weekday])
               by-route (subscribe [:report.composite/by :route])
               by-period (subscribe [:report.compare/by-period])
               by-hour (subscribe [:report.composite/by :hour])
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
      {:title "Por grupo de natureza"
       :subtitle "Registros"
       :content
       [:div
        [:div.row
         [:div.col-md-12
          [:table.table.table-bordered.table-striped
           [thead-indexed ["Natureza", "Registros 1", "Registros 2", "Variação (%)"]]
           [tbody-indexed @by-crime-group]]]]
        [:div.row
         [:div.col-md-6
          [chart
           {:id "chart-report-composite-by-crime-group-1"
            :type :pie
            :labels (range 1 (inc (count @by-crime-group)))
            :datasets (map second @by-crime-group)}]]
         [:div.col-md-6
          [chart
           {:id "chart-report-composite-by-crime-group-2"
            :type :pie
            :labels (range 1 (inc (count @by-crime-group)))
            :datasets (map #(get % 2) @by-crime-group)}]]]]}]

     ; by neighborhood (chart only)
     [card
      {:title "Por bairro"
       :subtitle "Registros"
       :content
       (into
         [:div.row]
         (map-indexed
          (fn [i rows]
            [:div.col-md-12
             [chart
              {:id (str "chart-report-by-neighborhood" i)
               :type :horizontal-bar
               :labels (map first rows)
               :datasets (map second rows)}]])
          @by-neighborhood))}]
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
           {:id "chart-report-composite-by-weekday"
            :type :bar
            :labels (map first @by-weekday)
            :datasets (map #(get % 2) @by-weekday)}]]]]}]
     ; by route
     [card
      {:title "Por via"
       :subtitle "Registros"
       :content
       (into
         [:div.row]
         (map-indexed
          (fn [i rows]
            [:div.col-md-12
             [chart
              {:id (str "chart-report-by-route" i)
               :type :horizontal-bar
               :labels (map first rows)
               :datasets (map second rows)}]])
          @by-route))}]
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
          {:id "chart-report-composite-by-period"
           :type :line
           :labels (map first @by-period)
           :datasets [{:label "Report by period 1"
                       :data (map second @by-period)}
                      {:label "Report by period 2"
                       :data (map #(get % 2) @by-period)}]}]]]}]
     ; by hour
     [card
      {:title "Por hora"
       :subtitle "Registros"
       :content
       [:div.row
        [:div.col-md-12
         [chart
          {:id "chart-report-composite-by-hour"
           :type :line
           :labels (map first (first @by-hour))
           :datasets (map-indexed
                      (fn [i rows]
                        {:label (str "Reports by hour " (inc i))
                         :data (map second rows)})
                      @by-hour)}]]]}]
     ; by date
     [card
      {:title "Por data"
       :subtitle "Registros"
       :content
       (into
         [:div.row]
         (map-indexed
          (fn [i rows]
            [:div.col-md-12
             [chart
              {:id (str "chart-report-single-by-date" i)
               :type :line
               :labels (map first rows)
               :datasets [{:label (str "Reports by date " (inc i))
                           :data (map second rows)}]}]])
          @by-date))}]]))

(defn single-range []
  "Template for single-range queries responses."
  (r/with-let [statistics (subscribe [:report.single/statistics])
               by-crime-type (subscribe [:report.single/by :crime-type])
               by-neighborhood (subscribe [:report.single/by :neighborhood])
               by-weekday (subscribe [:report.single/by-weekday])
               by-route (subscribe [:report.single/by :route])
               by-period (subscribe [:report.single/by-period])
               by-hour (subscribe [:report.single/by :hour])
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
          {:id "chart-report-single-by-crime-type"
           :type :pie
           :labels (range 1 (inc (count @by-crime-type)))
           :datasets (map second @by-crime-type)}]]

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
          {:id "chart-report-single-by-neighborhood"
           :type :bar
           :labels (map first @by-neighborhood)
           :datasets (map second @by-neighborhood)}]]
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
          {:id "chart-report-single-by-weekday"
           :type :bar
           :labels (map first @by-weekday)
           :datasets (map second @by-weekday)}]]
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
          {:id "chart-report-single-by-route"
           :type :bar
           :labels (map first @by-route)
           :datasets (map second @by-route)}]]
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
          {:id "chart-report-single-by-period"
           :type :line
           :labels (map first @by-period)
           :datasets [{:label "Reports by period"
                       :data (map second @by-period)}]}]]
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
          {:id "chart-report-single-by-hour"
           :type :line
           :labels (map first @by-hour)
           :datasets [{:label "Reports by hour"
                       :data (map second @by-hour)}]}]]]}]
     ; by date
     [card
      {:title "Por data"
       :subtitle "Registros"
       :content
       [:div.row
        [:div.col-md-12
         [chart
          {:id "chart-report-single-by-date"
           :type :line
           :labels (map first @by-date)
           :datasets [{:label "Reports by date"
                       :data (map second @by-date)}]}]]]}]]))

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

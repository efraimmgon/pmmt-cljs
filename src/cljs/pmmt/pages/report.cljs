(ns pmmt.pages.report
  (:require-macros
   [reagent.ratom :refer [reaction]])
  (:require
   [clojure.string :as string]
   [reagent.core :as r :refer [atom]]
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
(def default-values {:data-inicial-a "01/01/2015"
                     :data-final-a "31/05/2015"
                     :data-inicial-b "01/06/2015"
                     :data-final-b "31/12/2015"})

(defn report-form []
  (let [fields (atom default-values)
        errors (atom {})
        cities (subscribe [:cities])]
    (fn []
      [c/modal
       [:div
        "Relatório de análise criminal"]
       [:div
        [:div.well.well-sm
         [:strong "* campo obrigatório"]]
        ;;; REQUIRED FIELDS
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
        ;;; OPTIONAL FIELDS
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
           :rows (get-in @result [:natureza-comparison])
           :keyz (keys (first (get-in @result [:natureza-comparison])))}]
         [plot-comparison]
         (when (:optionals @result)
           [optionals-result-panels])]))))

(defn report-button []
   [:span
     [:button.btn.btn-block.btn-primary
      {:on-click #(dispatch [:modal report-form])}
      "Gerar novo relatório"] " "])

(defonce setup-ready?
  (atom false))

(defn main-page []
  ;; available crimes
  (dispatch-sync [:query-naturezas])
  ;; load `Plotly` for graphs
  (when-not @setup-ready?
    (c/add-script! {:src "https://cdn.plot.ly/plotly-latest.min.js"})
    (reset! setup-ready? true))
  (fn []
    [:div.container
     [:div.page-header
      [:h1 "Relatório "
       [:small "de registros criminais"]]]
     [:div
      [report-button] [:hr]]
     [result-panel]]))

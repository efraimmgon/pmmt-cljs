(ns pmmt.components.map
  (:require
   [clojure.string :as string]
   [reagent.core :as r :refer [atom]]
   [re-frame.core :refer [subscribe dispatch dispatch-sync]]
   [pmmt.utils :as utils]))

; Sortable table ----------------------------------------------------------
; compare functions
(defn duration-comp [a b]
  (cond
    (= a "null") 1
    (= b "null") -1
    :else
    (let [tuple-a (string/split a ":")
          tuple-b (string/split b ":")]
      (- (-> (js/Number (tuple-a 0))
             (* 60)
             (+ (js/Number (tuple-a 1))))
         (-> (js/Number (tuple-b 0))
             (* 60)
             (+ (js/Number (tuple-b 1))))))))

(defn date-comp [a b]
  (compare (utils/str->date a)
           (utils/str->date b)))

(defn weekday-comp [a b]
  (let [comparable #(cond
                      (= "Segunda" %) 1
                      (= "Terça" %) 2
                      (= "Quarta" %) 3
                      (= "Quinta" %) 4
                      (= "Sexta" %) 5
                      (= "Sábado" %) 6
                      (= "Domingo" %) 7)]
    (compare (comparable a)
             (comparable b))))

; Auxiliary html ---------------------------------------------------------

(defn display-query-params []
  (let [query-params (subscribe [:get-db :geo-query-params])]
        ;; todo: user friendly cities and nats
        ; cities (subscribe [:cities])
        ; naturezas (subscribe [:naturezas])]
    (fn []
      [:div
       [:h3 "Parâmetros"]
       (into
        [:div]
        (map (fn [[k v]]
               [:span {:style {:padding-right "30px"}}
                (str (name k) ": " v)])
             (seq @query-params)))])))

(defn display-ocorrencias-count []
  (let [ocorrencias-count (subscribe [:get-db :ocorrencias-count])
        total (:total @ocorrencias-count)
        roubo (:roubo @ocorrencias-count)
        furto (:furto @ocorrencias-count)
        droga (:droga @ocorrencias-count)
        homicidio (:homicidio @ocorrencias-count)
        outros (:outros @ocorrencias-count)]
    (into [:div]
          (map (fn [len text]
                 (when (pos? len)
                   ^{:key (gensym)}
                   [:span {:style {:padding-right "30px"}}
                    text]))
               [total roubo furto droga homicidio outros]
               [(str "Total: " total)
                (str "Roubos: " roubo)
                (str "Furtos: " furto)
                (str "Entorpecentes: " droga)
                (str "Homicídios: " homicidio)
                (str "Outros: " outros)]))))

(defn table-body []
  (let [sorted-content (subscribe [:sorted-content])]
    (fn []
      (into [:tbody]
        (map (fn [ocorrencia class]
               ^{:key ocorrencia}
               [:tr {:class class}
                [:td (:natureza ocorrencia)]
                [:td (:bairro ocorrencia)]
                [:td (:via ocorrencia)]
                [:td (:data ocorrencia)]
                [:td (:weekday ocorrencia)]
                [:td (:hora ocorrencia)]])
             @sorted-content
             (cycle ["odd" "even"]))))))

(defn table []
  [:div
    [display-query-params]
    [:h3 "Ocorrências registradas"]
    [display-ocorrencias-count]
    [:table.sortable
     [:thead>tr
      [:th {:on-click #(dispatch [:update-sort-engine {:key :natureza}])}
       "Natureza"]
      [:th {:on-click #(dispatch [:update-sort-engine {:key :bairro}])}
       "Bairro"]
      [:th {:on-click #(dispatch [:update-sort-engine {:key :via}])}
       "Via"]
      [:th {:on-click #(dispatch [:update-sort-engine {:key :data, :fn date-comp}])}
       "Data"]
      [:th {:on-click #(dispatch [:update-sort-engine {:key :weekday, :fn weekday-comp}])}
       "Dia da semana"]
      [:th {:on-click #(dispatch [:update-sort-engine {:key :hora, :fn duration-comp}])}
       "Hora"]]
     [table-body]]])

; main component

(defn map-inner []
  (r/create-class {:display-name "map-container"
                   :reagent-render (fn []
                                     [:div#map.center-block
                                      {:style {:height "650px"}}])
                   :component-did-mount #(dispatch [:init-gmap %])}))

(defn map-outer []
  (dispatch-sync [:map/initial-state])
  #(do
    [map-inner]))

(ns pmmt.components.admin.geocode
  (:require [ajax.core :as ajax]
            [reagent.core :as r :refer [atom]]
            [re-frame.core :refer [dispatch reg-event-fx]]
            [day8.re-frame.http-fx]
            [cljs-dynamic-resources.core :as cdr]))

; local state ------------------------------------------------------------

(defonce local-state
  (atom {:setup-ready? false
         :goog-api-key "AIzaSyA7ekvGGHxVTwTVcpi073GOERnktqvmYz8"
         :sinop {:lat -11.8608456, :lng -55.50954509999997}
         :result []}))

; misc ------------------------------------------------------------

(defn setup! []
  (let [setup-ready? (r/cursor local-state [:setup-ready?])]
    (when-not @setup-ready?
      (cdr/add-scripts!
       [{:src (str "https://maps.googleapis.com/maps/api/js?"
                   "key=" (:goog-api-key @local-state))}])
      (reset! setup-ready? true))))

; tipo de logradouro + nome do logradouro + número do lote +
; bairro + município + sigla do Estado
; "Avenida Governador Júlio Campos, 1111 - Setor Comercial, Sinop - MT, Brasil"

; key=AIzaSyA7ekvGGHxVTwTVcpi073GOERnktqvmYz8
;https://maps.googleapis.com/maps/api/geocode/output?parameters
;params -> address, key

; events -----------------------------------------------------------------

(defn geocode [address]
  (let [geocoder (new js/google.maps.Geocoder)
        opts (clj->js {:address address})
        lat (fn [results] (-> results (get 0) .-geometry .-location (.lat)))
        lng (fn [results] (-> results (get 0) .-geometry .-location (.lng)))
        handler (fn [results status]
                  ; save the result in local-state
                  (swap! local-state update-in [:result] conj
                    (if (= status js/google.maps.GeocoderStatus.OK)
                      ; check if goog could geocoded the address partially only
                      (if (and (-> results (get 0) .-partial_match)
                               (= (lat results) (get-in @local-state [:sinop :lat]))
                               (= (lng results) (get-in @local-state [:sinop :lng])))
                        ; default value in db is null; we must change it
                        ; so that it won't be fetched again
                        {:lat 0.0, :lng 0.0}
                        {:lat (lat results)
                         :lng (lng results)})
                      {:error {:status status}})))]
    (.geocode geocoder opts handler)))

(reg-event-fx
 :process-geocode-data
 (fn [{:keys [db]} [_ geocode-data]]

   (dorun
    (map geocode
        ["Avenida Governador Júlio Campos, 1111 - Setor Comercial, Sinop - MT, Brasil"
         "Avenida das Itaúbas, 3777 - Setor Comercial, Sinop - MT, Brasil"
         "Rua Ouro Preto, 103 - Jardim Belo Horizonte, Sinop - MT, Brasil"]))
   {:db db}))

(reg-event-fx
 :admin/sync-lat-lng
 (fn [{:keys [db]} _]
   {:http-xhrio {:method :get
                 :uri "/db/ocorrencia/geocode"
                 :on-success [:process-geocode-data]
                 :response-format (ajax/json-response-format {:keywords? true})}}))

; components -------------------------------------------------------------

(defn sync-info []
  [:div
   [:p.info
    "Atualizar o Banco de Dados com os coordenardas geográficas (latitude e
    longitude) com base no endereço de cada ocorrência."]])

(defn sync-button []
  [:button.btn.btn-primary
   {:on-click #(dispatch [:admin/sync-lat-lng])}
   "Sincronizar"])

; main -------------------------------------------------------------------

(defn sync-lat-lng-panel []
  (setup!)
  (let [result (r/cursor local-state [:result])]
    (fn []
      [:div.panel.panel-primary
       [:div.panel-heading
        [:h3 "Sincronizar Banco de Dados"]]
       [:div.panel-body
        [sync-info]
        [sync-button]
        (when (seq @result)
          [:h3 "Resultado: " (str @result)])]])))

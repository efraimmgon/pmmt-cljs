(ns pmmt.components.admin.geocode
  (:require-macros
   [pmmt.macros :refer [log]])
  (:require
   [ajax.core :as ajax]
   [reagent.core :as r :refer [atom]]
   [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
   [pmmt.components.common :as c]))

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
      (c/add-script!
       {:src (str "https://maps.googleapis.com/maps/api/js?"
                  "key=" (:goog-api-key @local-state))})
      (reset! setup-ready? true))))

; tipo de logradouro + nome do logradouro + número do lote +
; bairro + município + sigla do Estado
; "Avenida Governador Júlio Campos, 1111 - Setor Comercial, Sinop - MT, Brasil"

; key=AIzaSyA7ekvGGHxVTwTVcpi073GOERnktqvmYz8
;https://maps.googleapis.com/maps/api/geocode/output?parameters
;params -> address, key

; events -----------------------------------------------------------------

(defn extract-lat-lng [GeocoderResult]
  (let [lat (fn [results] (-> results (get 0) .-geometry .-location (.lat)))
        lng (fn [results] (-> results (get 0) .-geometry .-location (.lng)))]
    ; save the result in local-state
    (swap! local-state update :result conj
          ; check if goog could geocoded the address partially only
      (if (and (-> GeocoderResult (get 0) .-partial_match)
               (= (lat GeocoderResult) (get-in @local-state [:sinop :lat]))
               (= (lng GeocoderResult) (get-in @local-state [:sinop :lng])))
        ; default value in db is null; we must change it
        ; so that it won't be fetched again
        {:lat 0.0, :lng 0.0}
        {:lat (lat GeocoderResult)
         :lng (lng GeocoderResult)}))))

(defn geocode
  "Use `js/google.maps.Geocoder` to geocode an address"
  [geocoder-request query-limit?]
  (let [geocoder (new js/google.maps.Geocoder)
        opts (clj->js geocoder-request)
        handler (fn [GeocoderResult status]
                  (condp = status
                         "OK" (extract-lat-lng GeocoderResult)
                         "OVER_QUERY_LIMIT" (reset! query-limit? true)
                         (log status)))]
    (.geocode geocoder opts handler)))

(defn run-geocode
  "Applies geocode to each address given, making sure gmap's API
   policies are followed."
  ([addresses]
   (run-geocode addresses {:queries (atom 0), :query-limit? (atom false)}))
  ([addresses {:keys [queries, start, query-limit?] :as control}]
   (cond
     @query-limit? (js/alert "OVER_QUERY_LIMIT")
     ;; NOTE: <implementatian simplicity> each 50 queries we wait
     ;;       for a minutebefore continuing execution so we don't
     ;;       exceed our 50 queries/s limit.
     (= @queries 50)
     (js/setTimeout
      #(run-geocode addresses (assoc control :queries (atom 0)))
      1000)

     (seq addresses)
     (do (geocode (first addresses) query-limit?)
         (run-geocode (rest addresses) (assoc control :queries (swap! (:queries control) inc))))

     (empty? addresses)
     (js/alert "Fini!"))))

; NOTE: sample test data
(def addresses
  [{:address "Avenida Governador Júlio Campos, 1111 - Setor Comercial, Sinop - MT, Brasil"}
   {:address "Avenida das Itaúbas, 3777 - Setor Comercial, Sinop - MT, Brasil"}
   {:address "Rua Ouro Preto, 103 - Jardim Belo Horizonte, Sinop - MT, Brasil"}])

(reg-event-db
 :admin/sync-lat-lng
 (fn [db _]
   (ajax/GET "/db/crime-reports/geocode"
             ;; NOTE: ------> change this line!!! <------
             {:handler #(run-geocode addresses)
              :error-handler #(log %)})
   db))

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
        (when-not (empty? @result)
          [:h3 "Resultado: " (str @result)])]])))
